package language

import java.io.{PrintWriter, File}

import chisel3._
import examples.{DualPortBRAM, ProcessingUnitIO}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class StreamException(message: String, cause: Throwable = null) extends Exception(message, cause)

object Builder {
  var curBuilder: Builder = null

  def nextBuilder(inputWidth: Int, outputWidth: Int, io: ProcessingUnitIO, coreId: Int): Unit = {
    curBuilder = new Builder(inputWidth, outputWidth, io, coreId)
  }
}

class Builder(val inputWidth: Int, val outputWidth: Int, io: ProcessingUnitIO, coreId: Int) {
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

  val pipeActive = RegInit(false.B)
  val inputRegistered = Reg(UInt(inputWidth.W))
  var nextRegs: Array[UInt] = null
  var nextVectorRegs: Array[(UInt, UInt)] = null
  var nextTokenDoesRamRead = false.B
  var curTokenDoesRamWrite = false.B

  object GenBitsCase extends Enumeration {
    type GenBitsCase = Value
    val CUR_TOK, NEXT_TOK = Value
  }
  import GenBitsCase._

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
        // start with the outermost item in context so that common context prefixes
        // that appear in multiple places always have the same Chisel expression
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

  def onlySelfRead(b: StreamBits, selfStateId: Int): Boolean = {
    val nonSelfRead = b match {
      case BRAMSelect(bram, idx) => bram.stateId != selfStateId
      case _ => false
    }
    if (nonSelfRead) {
      false
    } else {
      b.productIterator.map {
        case s: StreamBits => onlySelfRead(s, selfStateId)
        case _ => true
      }.foldLeft(true)((b1, b2) => b1 && b2)
    }
  }

  def addRAMReads(cond: StreamBool, b: StreamBits, reads: Array[ArrayBuffer[(StreamBool, StreamBits)]]): Unit = {
    b match {
      case s: BRAMSelect => {
        // ensures that correct address can be fed to this BRAM on the same cycle next input is let in (i.e.
        // a single-stage pipeline is sufficient)
        assert(onlySelfRead(cond, s.arg.stateId))
        reads(s.arg.stateId).append((cond, s.idx))
      }
      case _ =>
    }
    b.productIterator.foreach {
      case s: StreamBits => addRAMReads(cond, s, reads)
      case _ =>
    }
  }

  def genBits(b: StreamBits, useNextToken: GenBitsCase): UInt = {
    b match {
      case l: Literal => l.l.U
      case a: Add => genBits(a.first, useNextToken) + genBits(a.second, useNextToken)
      case s: Subtract => genBits(s.first, useNextToken) - genBits(s.second, useNextToken)
      case c: Concat => genBits(c.first, useNextToken)##genBits(c.second, useNextToken)
      case i: StreamInput => if (useNextToken == CUR_TOK) inputRegistered else io.inputWord
      case s: BitSelect => genBits(s.arg, useNextToken)(s.upper, s.lower)
      case r: StreamReg => {
        useNextToken match {
          case CUR_TOK => chiselRegs(r.stateId)
          case NEXT_TOK => Mux(pipeActive && (!io.outputValid || io.outputReady), nextRegs(r.stateId),
            chiselRegs(r.stateId))
        }
      }
      case b: BRAMSelect => chiselBrams(b.arg.stateId).io.a_dout
      case v: VectorRegSelect => {
        val addr = genBits(v.idx, useNextToken)
        useNextToken match {
          case CUR_TOK => chiselVectorRegs(v.arg.stateId)(addr)
          case NEXT_TOK => {
            if (nextVectorRegs(v.arg.stateId) == null) {
              chiselVectorRegs(v.arg.stateId)(addr)
            } else {
              Mux((pipeActive && (!io.outputValid || io.outputReady)) && addr === nextVectorRegs(v.arg.stateId)._1,
                nextVectorRegs(v.arg.stateId)._2, chiselVectorRegs(v.arg.stateId)(addr))
            }
          }
        }
      }
      case b: StreamBool => genBool(b, useNextToken) // treat the bool as regular bits
      case _ => throw new StreamException("unexpected type in genBits: " + b.getClass.toString)
    }
  }

  def genBool(b: StreamBool, useNextToken: GenBitsCase): Bool = {
    b match {
      case n: Negate => !genBool(n.arg, useNextToken)
      case a: And => genBool(a.arg1, useNextToken) && genBool(a.arg2, useNextToken)
      case o: Or => genBool(o.arg1, useNextToken) || genBool(o.arg2, useNextToken)
      case c: BoolCast => genBits(c.arg, useNextToken).toBool()
      case e: Equal => genBits(e.first, useNextToken) === genBits(e.second, useNextToken)
      case n: NotEqual => genBits(n.first, useNextToken) =/= genBits(n.second, useNextToken)
      case l: LessThan => genBits(l.first, useNextToken) < genBits(l.second, useNextToken)
      case l: LessThanEqual => genBits(l.first, useNextToken) <= genBits(l.second, useNextToken)
      case g: GreaterThan => genBits(g.first, useNextToken) > genBits(g.second, useNextToken)
      case g: GreaterThanEqual => genBits(g.first, useNextToken) >= genBits(g.second, useNextToken)
      case _ => throw new StreamException("unexpected type in genBool: " + b.getClass.toString)
    }
  }

  def cStringForValue(value: BigInt): String = {
    assert(value <= Long.MaxValue)
    value.toLong + (if (value > Integer.MAX_VALUE) "L" else "")
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

    // used to eliminate the outermost BRAMSelect in the LHS of an assignment so that it's not included
    // in the calculation of the read depth
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
      0
    ).max
    assert(pipeDepth <= 1)

    // Whenever there is an active token in the pipeline that had no output or whose output is accepted,
    // we commit its results and clear the pipeline. We may accept another input token in the same cycle
    // if the conditions in io.inputReady are met.
    when (!pipeActive || (!io.outputValid || io.outputReady)) {
      inputRegistered := io.inputWord
      pipeActive := io.inputValid && io.inputReady
    }
    io.outputFinished := io.inputFinished && !pipeActive

    // emits
    var curEmit = genBits(emits(0)._2.data, CUR_TOK)
    var emitValid = genBool(emits(0)._1, CUR_TOK)
    for ((cond, e) <- emits.drop(1)) {
      curEmit = Mux(genBool(cond, CUR_TOK), genBits(e.data, CUR_TOK), curEmit)
      emitValid = emitValid || genBool(cond, CUR_TOK)
    }
    emitValid = pipeActive && emitValid
    io.outputValid := emitValid
    io.outputWord := curEmit

    // register writes
    nextRegs = new Array[UInt](chiselRegs.length)
    for ((cond, a) <- assignments) {
      a.lhs match {
        case r: StreamReg => regWrites(r.stateId).append((cond, a.rhs))
        case _ =>
      }
    }
    for (((cr, writes), i) <- chiselRegs.zip(regWrites).zipWithIndex) {
      var data = cr
      for ((cond, d) <- writes) {
        data = Mux(genBool(cond, CUR_TOK), genBits(d, CUR_TOK), data)
      }
      nextRegs(i) = data
      cr := Mux(pipeActive && (!io.outputValid || io.outputReady), data, cr)
    }

    // vector register writes
    nextVectorRegs = new Array[(UInt, UInt)](chiselVectorRegs.length)
    for (((cv, writes), vecIdx) <- chiselVectorRegs.zip(vectorRegWrites).zipWithIndex) {
      if (writes.length > 0) { // don't write anything if no user-defined writes so that ROM will be synthesized
        var idx = genBits(writes(0)._2, CUR_TOK)
        for ((cond, i, _) <- writes.drop(1)) {
          idx = Mux(genBool(cond, CUR_TOK), genBits(i, CUR_TOK), idx)
        }
        var data = cv(idx)
        for ((cond, _, d) <- writes) {
          data = Mux(genBool(cond, CUR_TOK), genBits(d, CUR_TOK), data)
        }
        nextVectorRegs(vecIdx) = (idx, data)
        cv(idx) := Mux(pipeActive && (!io.outputValid || io.outputReady), data, cv(idx))
      }
    }

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
        // If the current pipeline token is being flushed, we send its results into the RAM read ports
        // so that RAM reads will be available for the next input (if there is one) on the next cycle. The
        // RAM read addresses are guaranteed to be based only on registers (and not other RAM reads) due to the
        // pipeDepth <= 1 condition, so we know that the next addresses will be immediately available as the
        // pipeline is being flushed. RAM reads are the only case (besides the nextTokenDoesRamRead flag below)
        // where we need to use signals based on the incoming input and incoming register values rather than the
        // registered input and current register values.
        var addr = genBits(reads(0)._2, NEXT_TOK)
        for ((cond, d) <- reads.drop(1)) {
          addr = Mux(genBool(cond, NEXT_TOK), genBits(d, NEXT_TOK), addr)
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
        var wr = genBool(writes(0)._1, CUR_TOK)
        var addr = genBits(writes(0)._2, CUR_TOK)
        var data = genBits(writes(0)._3, CUR_TOK)
        for ((cond, a, d) <- writes.drop(1)) {
          wr = wr || genBool(cond, CUR_TOK)
          addr = Mux(genBool(cond, CUR_TOK), genBits(a, CUR_TOK), addr)
          data = Mux(genBool(cond, CUR_TOK), genBits(d, CUR_TOK), data)
        }
        curTokenDoesRamWrite = curTokenDoesRamWrite || wr
        cb.io.b_wr := wr && (pipeActive && (!io.outputValid || io.outputReady))
        cb.io.b_addr := addr
        cb.io.b_din := data
      } else {
        cb.io.b_wr := false.B
      }
    }

    // if the returned condition evaluates to false, cond must also evaluate to false
    def maximalNonRamReadingCond(cond: StreamBool): StreamBool = {
      if (readDepth(cond) == 0) {
        cond
      } else {
        cond match {
          case And(arg1, arg2) => {
            val arg1Maximal = maximalNonRamReadingCond(arg1)
            val arg2Maximal = maximalNonRamReadingCond(arg2)
            if (arg1Maximal != null && arg2Maximal != null) {
              And(arg1Maximal, arg2Maximal)
            } else if (arg1Maximal != null) {
              arg1Maximal
            } else if (arg2Maximal != null) {
              arg2Maximal
            } else {
              null
            }
          }
          case _ => null
        }
      }
    }

    // TODO not having the muxes in genBits/genBool for the next token could save logic
    // here if RAM reads only occur under one condition, in which case the condition is not already
    // computed with the muxes to select the correct address for the pipelined read
    var hasUnconditionalRamRead = false
    for ((c, a) <- assignments) {
      if (readDepth(assignIdxOr0(a.lhs)) > 0 || readDepth(a.rhs) > 0) {
        val maxC = maximalNonRamReadingCond(c)
        if (maxC == null) {
          hasUnconditionalRamRead = true
        } else {
          nextTokenDoesRamRead = nextTokenDoesRamRead || genBool(maxC, NEXT_TOK)
        }
      }
    }
    for ((c, e) <- emits) {
      if (readDepth(e.data) > 0) {
        val maxC = maximalNonRamReadingCond(c)
        if (maxC == null) {
          hasUnconditionalRamRead = true
        } else {
          nextTokenDoesRamRead = nextTokenDoesRamRead || genBool(maxC, NEXT_TOK)
        }
      }
    }
    for ((c, cond) <- conds) {
      if (readDepth(cond) > 0) {
        val maxC = maximalNonRamReadingCond(c)
        if (maxC == null) {
          hasUnconditionalRamRead = true
        } else {
          nextTokenDoesRamRead = nextTokenDoesRamRead || genBool(maxC, NEXT_TOK)
        }
      }
    }
    if (hasUnconditionalRamRead) {
      nextTokenDoesRamRead = true.B
    }
    // conservative rule that allows next input only if no writes were performed to ANY BRAM on the
    // current token, or if no reads are performed to ANY BRAM by the next token otherwise
    // TODO may be profitable to remove curTokenDoesRamWrite and/or nextTokenDoesRamRead in pipelines
    // where they don't lead to higher throughput
    io.inputReady := !pipeActive ||
      ((!io.outputValid || io.outputReady) && (!curTokenDoesRamWrite || !nextTokenDoesRamRead))
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
    val simBramsWasRead = new Array[BigInt](brams.length)

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
        case b: BRAMSelect => {
          val addr = genSimBits(b.idx).toInt
          require(simBramsWasRead(b.arg.stateId) == null || simBramsWasRead(b.arg.stateId) == addr,
            s"BRAM ${b.arg.stateId} read with multiple different addresses")
          simBramsWasRead(b.arg.stateId) = addr
          simBramsRead(b.arg.stateId)(addr)
        }
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
        simBramsWasRead(i) = null
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
