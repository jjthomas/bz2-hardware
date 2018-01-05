package language

import chisel3._
import examples.{DualPortBRAM, ProcessingUnitIO}

import scala.collection.mutable.ArrayBuffer

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
    for ((r, i) <- vectorRegs.zipWithIndex) {
      if (r.init != null) {
        chiselVectorRegs.append(RegInit(VecInit(r.init.map(b => b.asUInt(r.width.W)))))
      } else {
        chiselVectorRegs.append(Reg(Vec(r.numEls, UInt(r.width.W))))
      }
      vectorRegWrites(i) = new ArrayBuffer[(StreamBool, StreamBits, StreamBits)]
    }
    val bramReads = new Array[ArrayBuffer[(StreamBool, StreamBits)]](brams.length) // cond, addr
    val bramWrites = new Array[ArrayBuffer[(StreamBool, StreamBits, StreamBits)]](brams.length) // cond, addr, data
    for ((b, i) <- brams.zipWithIndex) {
      chiselBrams.append(Module(new DualPortBRAM(b.width, util.log2Ceil(b.numEls))))
      bramReads(i) = new ArrayBuffer[(StreamBool, StreamBits)]
      bramWrites(i) = new ArrayBuffer[(StreamBool, StreamBits, StreamBits)]
    }
    val pipeDepth = Array(
      (assignments.map { case (_, a) => readDepth(a.rhs) } ++ Array(0)).max,
      (emits.map { case (_, e) => readDepth(e.data) } ++ Array(0)).max,
      (conds.map { case (_, c) => readDepth(c)} ++ Array(0)).max,
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
    for ((cond, a) <- assignments) {
      a.lhs match {
        case v: VectorRegSelect => vectorRegWrites(v.arg.stateId).append((cond, v.idx, a.rhs))
        case _ =>
      }
    }
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

}
