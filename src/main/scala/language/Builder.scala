package language

import java.io.{PrintWriter, File}

import chisel3._
import examples.{DualPortBRAM, ProcessingUnitIO}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class StreamException(message: String, cause: Throwable = null) extends Exception(message, cause)

object Builder {
  var curBuilder: Builder = null

  def nextBuilder(inputWidth: Int, outputWidth: Int, io: ProcessingUnitIO): Unit = {
    curBuilder = new Builder(inputWidth, outputWidth, io)
  }
}

class Builder(val inputWidth: Int, val outputWidth: Int, io: ProcessingUnitIO) {
  require(inputWidth == outputWidth, "circuit inputWidth must equal outputWidth for now")
  val context = new ArrayBuffer[StreamWhenContext]

  // semantics for reg is single write per input token, arbitrary number of reads
  val regs = new ArrayBuffer[StreamReg]
  // semantics for vector reg is single write per input token (single address), arbitrary number of reads from any
  // addresses
  val vectorRegs = new ArrayBuffer[StreamVectorReg]
  // semantics for BRAM is single read, single write per input token
  val brams = new ArrayBuffer[StreamBRAM]
  val assignments = new ArrayBuffer[(StreamBool, Assign)]
  val emits = new ArrayBuffer[(StreamBool, Emit)]
  val conds = new ArrayBuffer[(StreamBool, StreamBool)]

  val chiselRegs = new ArrayBuffer[UInt]
  val chiselVectorRegs = new ArrayBuffer[Vec[UInt]]
  val chiselBrams = new ArrayBuffer[DualPortBRAM]

  def startContext(c: StreamWhenContext): Unit = {
    if (c.soloCond != null) {
      conds.append((getContextCondition(), c.soloCond))
    }
    context.append(c)
  }

  def endContext(): Unit = {
    context.remove(context.length - 1)
  }

  def registerAssignable(assignable: Any): Int = {
    require(context.isEmpty, "assignable cannot be declared inside a conditional")
    assignable match {
      case s: StreamReg => {
        regs.append(s)
        regs.length - 1
      }
      case b: StreamBRAM => {
        brams.append(b)
        brams.length - 1
      }
      case v: StreamVectorReg => {
        vectorRegs.append(v)
        vectorRegs.length - 1
      }
      case _ => throw new StreamException("registers, vector registers, and BRAMs are the only supported " +
        "assignables for now")
    }
  }

  def getContextCondition(): StreamBool = {
    if (context.isEmpty) {
      true.L.B
    } else {
      var cond = context(0).cond
      for (i <- 1 until context.length) {
        cond = cond && context(i).cond
      }
      cond
    }
  }

  def registerAssignment(assignment: Assign): Unit = {
    assignments.append((getContextCondition(), assignment))
  }

  def registerEmit(emit: Emit): Unit = {
    emits.append((getContextCondition(), emit))
  }

  def readDepth(b: StreamBits): Int = {
    val thisReads = b match {
      case BRAMSelect(_, _) => 1
      case _ => 0
    }
    thisReads + b.productIterator.map {
      case s: StreamBits => readDepth(s)
      case _ => 0
    }.max
  }

  def addRAMReads(cond: StreamBool, b: StreamBits, reads: Array[ArrayBuffer[(StreamBool, StreamBits)]]): Unit = {
    b match {
      case s: BRAMSelect => reads(s.arg.stateId).append((cond, s.idx))
      case _ =>
    }
    b.productIterator.foreach {
      case s: StreamBits => addRAMReads(cond, s, reads)
      case _ =>
    }
  }

  def genBits(b: StreamBits): UInt = {
    b match {
      case l: Literal => l.l.U
      case a: Add => genBits(a.first) + genBits(a.second)
      case s: Subtract => genBits(s.first) - genBits(s.second)
      case c: Concat => genBits(c.first)##genBits(c.second)
      case i: StreamInput => io.inputWord
      case s: BitSelect => genBits(s.arg)(s.upper, s.lower)
      case r: StreamReg => chiselRegs(r.stateId)
      case b: BRAMSelect => chiselBrams(b.arg.stateId).io.a_dout
      case v: VectorRegSelect => chiselVectorRegs(v.arg.stateId)(genBits(v.idx))
      case b: StreamBool => genBool(b) // treat the bool as regular bits
      case _ => throw new StreamException("unexpected type in genBits: " + b.getClass.toString)
    }
  }

  def genBool(b: StreamBool): Bool = {
    b match {
      case n: Negate => !genBool(n.arg)
      case a: And => genBool(a.arg1) && genBool(a.arg2)
      case o: Or => genBool(o.arg1) || genBool(o.arg2)
      case c: BoolCast => genBits(c.arg).toBool()
      case e: Equal => genBits(e.first) === genBits(e.second)
      case n: NotEqual => genBits(n.first) =/= genBits(n.second)
      case l: LessThan => genBits(l.first) < genBits(l.second)
      case l: LessThanEqual => genBits(l.first) <= genBits(l.second)
      case g: GreaterThan => genBits(g.first) > genBits(g.second)
      case g: GreaterThanEqual => genBits(g.first) >= genBits(g.second)
      case _ => throw new StreamException("unexpected type in genBool: " + b.getClass.toString)
    }
  }

  def compile(): Unit = {
    val regWrites = new Array[ArrayBuffer[(StreamBool, StreamBits)]](regs.length) // cond, data
    for ((r, i) <- regs.zipWithIndex) {
      if (r.init != null) {
        chiselRegs.append(RegInit(r.init.asUInt(r.width.W)))
      } else {
        chiselRegs.append(Reg(UInt(r.width.W)))
      }
      regWrites(i) = new ArrayBuffer[(StreamBool, StreamBits)]
    }
    val vectorRegWrites = new Array[ArrayBuffer[(StreamBool, StreamBits, StreamBits)]](vectorRegs.length) // cond,
    // addr, data
    for (i <- 0 until vectorRegWrites.length) {
      vectorRegWrites(i) = new ArrayBuffer[(StreamBool, StreamBits, StreamBits)]
    }
    for ((cond, a) <- assignments) {
      a.lhs match {
        case v: VectorRegSelect => vectorRegWrites(v.arg.stateId).append((cond, v.idx, a.rhs))
        case _ =>
      }
    }
    for ((r, writes) <- vectorRegs.zip(vectorRegWrites)) {
      if (r.init != null) {
        val initUInts = r.init.map(b => b.asUInt(r.width.W))
        if (writes.length == 0) {
          chiselVectorRegs.append(VecInit(initUInts))
        } else {
          chiselVectorRegs.append(RegInit(VecInit(initUInts)))
        }
      } else {
        chiselVectorRegs.append(Reg(Vec(r.numEls, UInt(r.width.W))))
      }
    }
    val bramReads = new Array[ArrayBuffer[(StreamBool, StreamBits)]](brams.length) // cond, addr
    val bramWrites = new Array[ArrayBuffer[(StreamBool, StreamBits, StreamBits)]](brams.length) // cond, addr, data
    for ((b, i) <- brams.zipWithIndex) {
      chiselBrams.append(Module(new DualPortBRAM(b.width, util.log2Ceil(b.numEls))))
      bramReads(i) = new ArrayBuffer[(StreamBool, StreamBits)]
      bramWrites(i) = new ArrayBuffer[(StreamBool, StreamBits, StreamBits)]
    }
    def assignIdxOr0(a: AssignableStreamData): StreamBits = {
      a match {
        case v: VectorRegSelect => v.idx
        case b: BRAMSelect => b.idx
        case _ => 0.L
      }
    }
    val pipeDepth = Array(
      (assignments.map { case (_, a) => Math.max(readDepth(assignIdxOr0(a.lhs)), readDepth(a.rhs)) } ++ Array(0)).max,
      (emits.map { case (_, e) => readDepth(e.data) } ++ Array(0)).max,
      (conds.map { case (_, c) => readDepth(c) } ++ Array(0)).max,
      1 // TODO reg-only design runs at 2 cycles per input currently
    ).max
    val pipe = RegInit(VecInit((0 until pipeDepth).map(_ => false.B)))
    when (!io.outputValid || io.outputReady) {
      for (i <- 1 until pipeDepth) {
        pipe(i) := pipe(i - 1)
      }
      // pipeline should be cleared and previous input acknowledged before accepting next one
      pipe(0) := io.inputValid && pipe.asUInt === 0.U
    }
    // Signalling ready for the input that was just processed (this ensures that we
    // don't have to save the input token in a register if we have a multi-cycle pipeline).
    // Only works if we can have only one token in the pipeline at a time.
    io.inputReady := pipe(pipeDepth - 1) && (!io.outputValid || io.outputReady)
    io.outputFinished := io.inputFinished && pipe.asUInt === 0.U

    // emits
    var curEmit = genBits(emits(0)._2.data)
    var emitValid = genBool(emits(0)._1)
    for ((cond, e) <- emits.drop(1)) {
      curEmit = Mux(genBool(cond), genBits(e.data), curEmit)
      emitValid = emitValid || genBool(cond)
    }
    emitValid = pipe(pipeDepth - 1) && emitValid
    io.outputValid := emitValid
    io.outputWord := curEmit

    // BRAM reads
    for ((cond, a) <- assignments) {
      addRAMReads(cond, a.rhs, bramReads)
      a.lhs match {
        case b: BRAMSelect => addRAMReads(cond, b.idx, bramReads)
        case v: VectorRegSelect => addRAMReads(cond, v.idx, bramReads)
        case v: VectorWireSelect => addRAMReads(cond, v.idx, bramReads)
        case _ =>
      }
    }
    for ((cond, e) <- emits) {
      addRAMReads(cond, e.data, bramReads)
    }
    for ((cond, c0) <- conds) {
      addRAMReads(cond, c0, bramReads)
    }
    for ((cb, reads) <- chiselBrams.zip(bramReads)) {
      cb.io.a_wr := false.B
      if (reads.length > 0) {
        var addr = genBits(reads(0)._2)
        for ((cond, d) <- reads.drop(1)) {
          addr = Mux(genBool(cond), genBits(d), addr)
        }
        cb.io.a_addr := addr
      }
    }

    // BRAM writes
    for ((cond, a) <- assignments) {
      a.lhs match {
        case b: BRAMSelect => bramWrites(b.arg.stateId).append((cond, b.idx, a.rhs))
        case _ =>
      }
    }
    for ((cb, writes) <- chiselBrams.zip(bramWrites)) {
      if (writes.length > 0) {
        var wr = genBool(writes(0)._1)
        var addr = genBits(writes(0)._2)
        var data = genBits(writes(0)._3)
        for ((cond, a, d) <- writes.drop(1)) {
          wr = wr || genBool(cond)
          addr = Mux(genBool(cond), genBits(a), addr)
          data = Mux(genBool(cond), genBits(d), data)
        }
        cb.io.b_wr := wr && pipe(pipeDepth - 1) && (!io.outputValid || io.outputReady)
        cb.io.b_addr := addr
        cb.io.b_din := data
      } else {
        cb.io.b_wr := false.B
      }
    }

    // register writes
    for ((cond, a) <- assignments) {
      a.lhs match {
        case r: StreamReg => regWrites(r.stateId).append((cond, a.rhs))
        case _ =>
      }
    }
    for ((cr, writes) <- chiselRegs.zip(regWrites)) {
      var data = cr
      for ((cond, d) <- writes) {
        data = Mux(genBool(cond), genBits(d), data)
      }
      cr := Mux(pipe(pipeDepth - 1) && (!io.outputValid || io.outputReady), data, cr)
    }

    // vector register writes
    for ((cv, writes) <- chiselVectorRegs.zip(vectorRegWrites)) {
      if (writes.length > 0) { // don't write anything if no user-defined writes so that ROM will be synthesized
        var idx = genBits(writes(0)._2)
        for ((cond, i, _) <- writes.drop(1)) {
          idx = Mux(genBool(cond), genBits(i), idx)
        }
        var data = cv(idx)
        for ((cond, _, d) <- writes) {
          data = Mux(genBool(cond), genBits(d), data)
        }
        cv(idx) := Mux(pipe(pipeDepth - 1) && (!io.outputValid || io.outputReady), data, cv(idx))
      }
    }
  }

  def simulate(numInputBits: Int, inputBits: BigInt): (Int, BigInt) = {
    assert(numInputBits % inputWidth == 0)

    val simRegsRead = new Array[BigInt](regs.length)
    val simVectorRegsRead = new Array[Array[BigInt]](vectorRegs.length)
    val simBramsRead = new Array[Array[BigInt]](brams.length)
    val simRegsWrite = new Array[BigInt](regs.length)
    val simVectorRegsWrite = new Array[Array[BigInt]](vectorRegs.length)
    val simBramsWrite = new Array[Array[BigInt]](brams.length)
    val simRegsWasWritten = (0 until regs.length).map(_ => false).toArray
    val simVectorRegsWasWritten = (0 until vectorRegs.length).map(_ => false).toArray
    val simBramsWasWritten = (0 until brams.length).map(_ => false).toArray

    for ((r, i) <- regs.zipWithIndex) {
      val nextEl = if (r.init != null) r.init else BigInt(Random.nextInt())
      simRegsRead(i) = nextEl
      simRegsWrite(i) = nextEl
    }
    for ((v, i) <- vectorRegs.zipWithIndex) {
      val nextEl = if (v.init != null) v.init.toArray else (0 until v.numEls).map(_ => BigInt(Random.nextInt())).toArray
      simVectorRegsRead(i) = nextEl
      simVectorRegsWrite(i) = nextEl
    }
    for ((b, i) <- brams.zipWithIndex) {
      val nextEl = (0 until b.numEls).map(_ => BigInt(Random.nextInt())).toArray
      simBramsRead(i) = nextEl
      simBramsWrite(i) = nextEl
    }

    var inputWord: BigInt = null

    def truncate(b: BigInt, bits: Int): BigInt = {
      b & ((BigInt(1) << bits) - 1)
    }

    def genSimBits(b: StreamBits): BigInt = {
      b match {
        case l: Literal => l.l
        case a: Add => genSimBits(a.first) + genSimBits(a.second)
        case s: Subtract => genSimBits(s.first) - genSimBits(s.second)
        case c: Concat => (genSimBits(c.first) << c.second.getWidth) | genSimBits(c.second)
        case i: StreamInput => inputWord
        case s: BitSelect => {
          val numBits = s.upper - s.lower + 1
          (genSimBits(s.arg) >> s.lower) & ((BigInt(1) << numBits) - 1)
        }
        case r: StreamReg => simRegsRead(r.stateId)
        case b: BRAMSelect => simBramsRead(b.arg.stateId)(genSimBits(b.idx).toInt)
        case v: VectorRegSelect => simVectorRegsRead(v.arg.stateId)(genSimBits(v.idx).toInt)
        case b: StreamBool => genSimBool(b) // treat the bool as regular bits
        case _ => throw new StreamException("unexpected type in genSimBits: " + b.getClass.toString)
      }
    }

    def genSimBool(b: StreamBool): Boolean = {
      b match {
        case n: Negate => !genSimBool(n.arg)
        case a: And => genSimBool(a.arg1) && genSimBool(a.arg2)
        case o: Or => genSimBool(o.arg1) || genSimBool(o.arg2)
        case c: BoolCast => genSimBits(c.arg) != 0
        case e: Equal => genSimBits(e.first) == genSimBits(e.second)
        case n: NotEqual => genSimBits(n.first) != genSimBits(n.second)
        case l: LessThan => genSimBits(l.first) < genSimBits(l.second)
        case l: LessThanEqual => genSimBits(l.first) <= genSimBits(l.second)
        case g: GreaterThan => genSimBits(g.first) > genSimBits(g.second)
        case g: GreaterThanEqual => genSimBits(g.first) >= genSimBits(g.second)
        case _ => throw new StreamException("unexpected type in genSimBool: " + b.getClass.toString)
      }
    }

    var numOutputBits = 0
    var output = BigInt(0)
    for (i <- 0 until numInputBits by inputWidth) {
      inputWord = (inputBits >> i) & ((BigInt(1) << inputWidth) - 1)
      for ((cond, a) <- assignments) {
        if (genSimBool(cond)) {
          a.lhs match {
            case r: StreamReg => {
              simRegsWrite(r.stateId) = truncate(genSimBits(a.rhs), regs(r.stateId).width)
              require(!simRegsWasWritten(r.stateId), s"reg ${r.stateId} written multiple times")
              simRegsWasWritten(r.stateId) = true
            }
            case v: VectorRegSelect => {
              simVectorRegsWrite(v.arg.stateId)(genSimBits(v.idx).toInt) =
                truncate(genSimBits(a.rhs), vectorRegs(v.arg.stateId).width)
              require(!simVectorRegsWasWritten(v.arg.stateId), s"vector reg ${v.arg.stateId} written multiple times")
              simVectorRegsWasWritten(v.arg.stateId) = true
            }
            case b: BRAMSelect => {
              simBramsWrite(b.arg.stateId)(genSimBits(b.idx).toInt) =
                truncate(genSimBits(a.rhs), brams(b.arg.stateId).width)
              require(!simBramsWasWritten(b.arg.stateId), s"BRAM ${b.arg.stateId} written multiple times")
              simBramsWasWritten(b.arg.stateId) = true
            }
            case _ =>
          }
        }
      }
      for ((cond, e) <- emits) {
        if (genSimBool(cond)) {
          output = (truncate(genSimBits(e.data), outputWidth) << numOutputBits) | output
          numOutputBits += outputWidth
        }
      }
      for (i <- 0 until simRegsWrite.length) {
        simRegsRead(i) = simRegsWrite(i)
        simRegsWasWritten(i) = false
      }
      for (i <- 0 until simVectorRegsWrite.length) {
        for (j <- 0 until simVectorRegsWrite(i).length) {
          simVectorRegsRead(i)(j) = simVectorRegsWrite(i)(j)
        }
        simVectorRegsWasWritten(i) = false
      }
      for (i <- 0 until simBramsWrite.length) {
        for (j <- 0 until simBramsWrite(i).length) {
          simBramsRead(i)(j) = simBramsWrite(i)(j)
        }
        simBramsWasWritten(i) = false
      }
    }
    (numOutputBits, output)
  }

  class CWriter(outputFile: File) {
    var indentLevel = 0
    val pw = new PrintWriter(outputFile)
    def writeLine(line: String): Unit = {
      if (line.endsWith("}") || line.endsWith("};")) {
        indentLevel = if (indentLevel == 0) 0 else indentLevel - 1
      }
      for (i <- 0 until indentLevel) {
        pw.write("  ")
      }
      pw.write(line + "\n")
      if (line.endsWith("{")) {
        indentLevel += 1
      }
    }

    def close(): Unit = {
      pw.close()
    }
  }

  def genCSim(outputFile: File): Unit = {
    val vectorElsPerLine = 10
    def getCWidthForBitWidth(bitWidth: Int): Int = {
      assert(bitWidth <= 64)
      1 << Math.max(util.log2Ceil(bitWidth), 3)
    }
    def getCRandForBitWidth(bitWidth: Int): BigInt = {
      val cWidth = getCWidthForBitWidth(bitWidth)
      cWidth match {
        case 8 => Random.nextInt() & ((1 << 8) - 1)
        case 16 => Random.nextInt() & ((1 << 16) - 1)
        case 32 => Random.nextInt()
        case 64 => Random.nextLong()
      }
    }
    def cStringForValue(value: BigInt): String = {
      assert(value <= Long.MaxValue)
      value.toLong + (if (value > Integer.MAX_VALUE) "L" else "")
    }
    def emitVectorInit(cw: CWriter, vectors: ArrayBuffer[Any]): Unit = {
      for ((v, i) <- vectors.zipWithIndex) {
        val (width, numEls, init, name) = v match {
          case vec: StreamVectorReg => (vec.width, vec.numEls, vec.init, "vec")
          case bram: StreamBRAM => (bram.width, bram.numEls, null, "bram")
          case _ => throw new StreamException("unexpected type in emitVectorInit: " + v.getClass.toString)
        }
        val elCWidth = getCWidthForBitWidth(width)
        for (rw <- Array("read", "write")) {
          cw.writeLine(s"uint${elCWidth}_t ${name}${i}_$rw[] = {")
          for (j <- 0 until numEls by vectorElsPerLine) {
            var line = ""
            for (k <- j until Math.min(j + vectorElsPerLine, numEls)) {
              val nextEl = if (init != null) cStringForValue(init(j + k))
                else cStringForValue(getCRandForBitWidth(width))
              line += s"${if (k == 0) "" else " "}$nextEl,"
            }
            cw.writeLine(line)
          }
          cw.writeLine("};")
        }
      }
    }
    def emitVectorWriteToRead(cw: CWriter, vectors: ArrayBuffer[Any]): Unit = {
      for ((v, i) <- vectors.zipWithIndex) {
        val (numEls, name) = v match {
          case vec: StreamVectorReg => (vec.numEls, "vec")
          case bram: StreamBRAM => (bram.numEls, "bram")
          case _ => throw new StreamException("unexpected type in emitVectorWriteToRead: " + v.getClass.toString)
        }
        cw.writeLine(s"for (uint32_t j = 0; j < $numEls; j++) {")
        cw.writeLine(s"${name}${i}_read[j] = ${name}${i}_write[j];")
        cw.writeLine("}")
      }
    }
    def genCBits(b: StreamBits): String = {
      assert(b.getWidth <= 64)
      b match {
        case l: Literal => cStringForValue(l.l)
        case a: Add => s"(${genCBits(a.first)} + ${genCBits(a.second)})"
        case s: Subtract => s"(${genCBits(s.first)} - ${genCBits(s.second)})"
        case c: Concat => s"(((uint64_t)${genCBits(c.first)} << ${c.second.getWidth}) | ${genCBits(c.second)})"
        case i: StreamInput => "input[i]"
        case s: BitSelect => s"(((uint64_t)${genCBits(s.arg)} >> ${s.lower}) & ((1L << ${s.upper - s.lower + 1}) - 1))"
        case r: StreamReg => s"reg${r.stateId}_read"
        case b: BRAMSelect => s"bram${b.arg.stateId}_read[min(${genCBits(b.idx)}, ${b.arg.numEls - 1})]"
        case v: VectorRegSelect => s"vec${v.arg.stateId}_read[min(${genCBits(v.idx)}, ${v.arg.numEls - 1})]"
        case b: StreamBool => genCBool(b) // treat the bool as regular bits
        case _ => throw new StreamException("unexpected type in genCBits: " + b.getClass.toString)
      }
    }

    def genCBool(b: StreamBool): String = {
      b match {
        case n: Negate => s"!${genCBool(n.arg)}"
        case a: And => s"(${genCBool(a.arg1)} && ${genCBool(a.arg2)})"
        case o: Or => s"(${genCBool(o.arg1)} || ${genCBool(o.arg2)})"
        case c: BoolCast => genCBits(c.arg)
        case e: Equal => s"(${genCBits(e.first)} == ${genCBits(e.second)})"
        case n: NotEqual => s"(${genCBits(n.first)} != ${genCBits(n.second)})"
        case l: LessThan => s"(${genCBits(l.first)} < ${genCBits(l.second)})"
        case l: LessThanEqual => s"(${genCBits(l.first)} <= ${genCBits(l.second)})"
        case g: GreaterThan => s"(${genCBits(g.first)} > ${genCBits(g.second)})"
        case g: GreaterThanEqual => s"(${genCBits(g.first)} >= ${genCBits(g.second)})"
        case _ => throw new StreamException("unexpected type in genCBool: " + b.getClass.toString)
      }
    }

    val cw = new CWriter(outputFile)
    val cInputWidth = getCWidthForBitWidth(inputWidth)
    val cOutputWidth = getCWidthForBitWidth(outputWidth)
    cw.writeLine(
      s"""#include <stdint.h>
         |#include <stdlib.h>
         |#include <stdio.h>
         |#include <sys/time.h>
         |
         |uint64_t min(uint64_t x, uint64_t y) { return x < y ? x : y; }\n""".stripMargin)
    cw.writeLine(s"uint32_t run(uint${cInputWidth}_t *input, uint32_t input_len, uint${cOutputWidth}_t *output) {")
    cw.writeLine("uint32_t output_count = 0;")
    for ((r, i) <- regs.zipWithIndex) {
      val init = if (r.init != null) r.init else getCRandForBitWidth(r.width)
      val cWidth = getCWidthForBitWidth(r.width)
      for (rw <- Array("read", "write")) {
        cw.writeLine(s"uint${cWidth}_t reg${i}_$rw = ${cStringForValue(init)};")
      }
    }
    emitVectorInit(cw, vectorRegs.asInstanceOf[ArrayBuffer[Any]])
    emitVectorInit(cw, brams.asInstanceOf[ArrayBuffer[Any]])
    cw.writeLine("for (uint32_t i = 0; i < input_len; i++) {")
    for ((cond, a) <- assignments) {
      cw.writeLine(s"if (${genCBool(cond)}) {")
      a.lhs match {
        case r: StreamReg => cw.writeLine(s"reg${r.stateId}_write = ${genCBits(a.rhs)};")
        case v: VectorRegSelect =>
          cw.writeLine(s"vec${v.arg.stateId}_write[min(${genCBits(v.idx)}, ${v.arg.numEls - 1})] = ${genCBits(a.rhs)};")
        case b: BRAMSelect =>
          cw.writeLine(
            s"bram${b.arg.stateId}_write[min(${genCBits(b.idx)}, ${b.arg.numEls - 1})] = ${genCBits(a.rhs)};")
        case _ =>
      }
      cw.writeLine("}")
    }
    for ((cond, e) <- emits) {
      cw.writeLine(s"if (${genCBool(cond)}) {")
      cw.writeLine(s"output[output_count++] = ${genCBits(e.data)};")
      cw.writeLine("}")
    }
    for (i <- 0 until regs.length) {
      cw.writeLine(s"reg${i}_read = reg${i}_write;")
    }
    emitVectorWriteToRead(cw, vectorRegs.asInstanceOf[ArrayBuffer[Any]])
    emitVectorWriteToRead(cw, brams.asInstanceOf[ArrayBuffer[Any]])
    cw.writeLine("}")
    cw.writeLine("return output_count;")
    cw.writeLine("}")
    cw.writeLine(
      s"""
         |int main() {
         |  uint32_t LEN = 1 << 25;
         |  uint8_t *input = (uint8_t *)malloc(LEN);
         |  uint8_t *output = (uint8_t *)malloc(LEN);
         |  for (uint32_t i = 0; i < LEN; i++) {
         |    input[i] = rand() % 256;
         |    output[i] = 0;
         |  }
         |  struct timeval start, end, diff;
         |  gettimeofday(&start, 0);
         |  uint32_t output_count = run((uint${cInputWidth}_t *)input, LEN / ${cInputWidth / 8},
         |    (uint${cOutputWidth}_t *)output);
         |  gettimeofday(&end, 0);
         |  timersub(&end, &start, &diff);
         |  double secs = diff.tv_sec + diff.tv_usec / 1000000.0;
         |  printf("%.2f MB/s, %d output tokens, random output byte: %d\\n", LEN / 1000000.0 / secs, output_count,
         |    output_count == 0 ? 0 : output[rand() % output_count]);
         |  return 0;
         |}\n""".stripMargin)
    cw.close()
  }

}
