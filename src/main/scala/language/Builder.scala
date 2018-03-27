package language

import java.io.{PrintWriter, File}

import chisel3._
import examples.{DualPortBRAM, ProcessingUnitIO}

import scala.collection.mutable
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

  // semantics for reg is single write per tick, arbitrary number of reads
  val regs = new ArrayBuffer[StreamReg]
  // semantics for vector reg is single write per tick (single address), arbitrary number of reads from any
  // addresses
  val vectorRegs = new ArrayBuffer[StreamVectorReg]
  // semantics for BRAM is single read, single write per tick
  val brams = new ArrayBuffer[StreamBRAM]
  val vars = new ArrayBuffer[(Int, StreamBits)]

  val fullAssignments = new ArrayBuffer[(Seq[StreamBool], Boolean, Assign)]
  lazy val assignments = fullAssignments.map(t => (collapseContext(t._1), t._2, t._3))
  val fullEmits = new ArrayBuffer[(Seq[StreamBool], Boolean, Emit)]
  lazy val emits = fullEmits.map(t => (collapseContext(t._1), t._2, t._3))
  val fullConds = new ArrayBuffer[(Seq[StreamBool], StreamBool)]
  lazy val conds = fullConds.map(t => (collapseContext(t._1), t._2))

  var inSwhile = false
  var inInputContext = false
  val fullSwhileConds = new ArrayBuffer[Seq[StreamBool]]
  lazy val swhileConds = fullSwhileConds.map(c => collapseContext(c))

  val chiselRegs = new ArrayBuffer[UInt]
  val chiselVectorRegs = new ArrayBuffer[Vec[UInt]]
  val chiselBrams = new ArrayBuffer[DualPortBRAM]
  val chiselVars = new ArrayBuffer[UInt]

  val inputReg = Reg(UInt(inputWidth.W))
  val inputRegValid = RegInit(false.B)
  // marks if the current token is the finished token, is set once and remains true thereafter
  val finishedReg = RegInit(false.B)
  // next reg and vector regs values that are about to be written for the current active tick
  var nextRegs: Array[UInt] = null
  var nextVectorRegs: Array[(UInt, UInt)] = null
  var nextVars: Array[UInt] = null
  val swhileDone = Wire(Bool())
  // all BRAM reads for active tick are ready
  val pipeFinalState = Wire(Bool())
  // a single tick is finishing, where a tick is defined as a single iteration of an swhile or the closing
  // iteration after all swhiles are complete
  val pipeFinishing = pipeFinalState && (!io.outputValid || io.outputReady)

  object GenBitsCase extends Enumeration {
    type GenBitsCase = Value
    val CUR_TICK, NEXT_TICK = Value
  }
  import GenBitsCase._

  def startSwhile(cond: StreamBool): Unit = {
    require(!inSwhile)
    inSwhile = true
    fullSwhileConds.append(getContextCondition() ++ Seq(cond))
  }

  def endSwhile(): Unit = {
    inSwhile = false
  }

  def startInputContext(): Unit = {
    inInputContext = true
  }

  def endInputContext(): Unit = {
    inInputContext = false
  }

  def startContext(c: StreamWhenContext): Unit = {
    if (c.soloCond != null) {
      fullConds.append((getContextCondition(), c.soloCond))
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
      case v: StreamVar => {
        vars.append((v.getWidth, v.init))
        vars.length - 1
      }
      case _ => throw new StreamException("registers, vector registers, BRAMs, and vars are the only supported " +
        "assignables for now")
    }
  }

  def collapseContext(ctx: Seq[StreamBool]): StreamBool = {
    var cond = ctx(0)
    for (i <- 1 until ctx.length) {
      // start with the outermost item in context so that common context prefixes
      // that appear in multiple places always have the same Chisel expression
      cond = cond && ctx(i)
    }
    cond
  }

  def getContextCondition(): Seq[StreamBool] = {
    if (context.isEmpty) {
      Seq(true.L.B)
    } else {
      context.map(c => c.cond)
    }
  }

  def registerAssignment(assignment: Assign): Unit = {
    require(inInputContext, "statements must be in an onInput or onFinished block")
    assignment.lhs match {
      // TODO var that is defined outside of swhile and set both inside and outside swhile is undefined, we should
      // probably check for this case
      case v: StreamVar =>
        if (vars(v.stateId)._2 == null) {
          vars(v.stateId) = (vars(v.stateId)._1, assignment.rhs)
        } else {
          vars(v.stateId) = (vars(v.stateId)._1,
            StreamMux(collapseContext(getContextCondition()), assignment.rhs, vars(v.stateId)._2))
        }
      case _ => fullAssignments.append((getContextCondition(), inSwhile, assignment))
    }
  }

  def registerEmit(emit: Emit): Unit = {
    require(inInputContext, "statements must be in an onInput or onFinished block")
    fullEmits.append((getContextCondition(), inSwhile, emit))
  }

  def addRAMReads(cond: StreamBool, hasPriority: Boolean, b: StreamBits,
                  reads: Array[ArrayBuffer[(StreamBool, Boolean, StreamBits)]]): Unit = {
    b match {
      case s: BRAMSelect => reads(s.arg.stateId).append((cond, hasPriority, s.idx))
      case _ =>
    }
    b match {
      case m: StreamMux => {
        addRAMReads(cond, hasPriority, m.cond, reads)
        addRAMReads(cond && m.cond, hasPriority, m.t, reads)
        addRAMReads(cond && !m.cond, hasPriority, m.f, reads)
      }
      case v: StreamVar => addRAMReads(cond, hasPriority, vars(v.stateId)._2, reads) // when computing RAM dependencies,
      // we always want to assume that vars are inlined
      case _ =>
        b.productIterator.foreach {
          case s: StreamBits => addRAMReads(cond, hasPriority, s, reads)
          case _ =>
        }
    }
  }

  def collectAllReads(b: StreamBits, s: mutable.Set[Int]): mutable.Set[Int] = {
    b match {
      case sel: BRAMSelect => s += sel.arg.stateId
      case _ =>
    }
    b match {
      case v: StreamVar => collectAllReads(vars(v.stateId)._2, s)
      case _ =>
        b.productIterator.foreach {
          case sb: StreamBits => collectAllReads(sb, s)
          case _ =>
        }
    }
    s
  }

  // add dependencies to reads in b
  // deps is set of dependencies that come from the condition on b
  // table format: for each BRAM, (dependencies, set of read idxs, and whether it has an indirect read)
  def addDependencies(b: StreamBits, deps: mutable.Set[Int],
                      table: Array[(mutable.Set[Int], mutable.Set[StreamBits], Boolean)]): mutable.Set[Int] = {
                      // returns set of BRAMs read in b
    val bramsRead = new mutable.HashSet[Int]
    b match {
      case v: StreamVar => bramsRead ++= addDependencies(vars(v.stateId)._2, deps, table)
      case _ =>
        b.productIterator.foreach {
          case sb: StreamBits => bramsRead ++= addDependencies(sb, deps, table)
          case _ =>
        }
    }
    b match {
      case sel: BRAMSelect => {
        table(sel.arg.stateId)._1 ++= deps
        table(sel.arg.stateId)._2 += sel.idx
        require(!bramsRead.contains(sel.arg.stateId), s"self-derived indirect read for BRAM ${sel.arg.stateId}")
        table(sel.arg.stateId)._1 ++= bramsRead
        if (bramsRead.size > 0) {
          table(sel.arg.stateId) = (table(sel.arg.stateId)._1, table(sel.arg.stateId)._2, true)
        }
        bramsRead += sel.arg.stateId
      }
      case _ =>
    }
    bramsRead
  }

  // using the table produced by addDependencies, compute the entry in the depths array for curRam, computing entries
  // for its dependencies if needed
  def determineReadDepthsHelper(table: Array[(mutable.Set[Int], mutable.Set[StreamBits], Boolean)], depths: Array[Int],
                                curRam: Int, curCallDepth: Int): Unit = {
    require(curCallDepth < depths.length, "cycle in BRAM read dependency graph")
    if (depths(curRam) != 0) {
      return
    }
    // case where curRam has no dependencies or has only one distinct read idx (and no indirect reads)
    if (table(curRam)._1.size == 0 || (!table(curRam)._3 && table(curRam)._2.size <= 1)) {
      depths(curRam) = 1
      return
    }
    depths(curRam) = table(curRam)._1.map(dep => {
      if (dep != curRam) {
        determineReadDepthsHelper(table, depths, dep, curCallDepth + 1)
        depths(dep) + 1
      } else {
        1
      }
    }).max
  }

  def determineReadDepths(table: Array[(mutable.Set[Int], mutable.Set[StreamBits], Boolean)]): Array[Int] = {
    val depths = new Array[Int](table.length)
    for (i <- 0 until depths.length) {
      determineReadDepthsHelper(table, depths, i, 0)
    }
    depths
  }

  def genBits(b: StreamBits, useNextToken: GenBitsCase): UInt = {
    b match {
      case l: Literal => l.l.asUInt(l.getWidth.W)
      case a: Add => genBits(a.first, useNextToken) +& genBits(a.second, useNextToken)
      case s: Subtract => genBits(s.first, useNextToken) - genBits(s.second, useNextToken)
      case c: Concat => genBits(c.first, useNextToken)##genBits(c.second, useNextToken)
      case i: StreamInput.type => if (useNextToken == CUR_TICK) inputReg else
        // unless there is no current input or we are about to flush the current input, use the current input
        Mux(!inputRegValid || (pipeFinishing && swhileDone), io.inputWord, inputReg)
      case s: BitSelect => genBits(s.arg, useNextToken)(s.upper, s.lower)
      case r: StreamReg => {
        useNextToken match {
          case CUR_TICK => chiselRegs(r.stateId)
          // only use nextReg when pipeFinishing so that output is not contaminated before it is flushed
          case NEXT_TICK => Mux(pipeFinishing, nextRegs(r.stateId), chiselRegs(r.stateId))
        }
      }
      case b: BRAMSelect => chiselBrams(b.arg.stateId).io.a_dout
      case v: VectorRegSelect => {
        val addr = genBits(v.idx, useNextToken)
        useNextToken match {
          case CUR_TICK => chiselVectorRegs(v.arg.stateId)(addr)
          case NEXT_TICK => {
            if (nextVectorRegs(v.arg.stateId) == null) {
              chiselVectorRegs(v.arg.stateId)(addr)
            } else {
              Mux(pipeFinishing && addr === nextVectorRegs(v.arg.stateId)._1, nextVectorRegs(v.arg.stateId)._2,
                chiselVectorRegs(v.arg.stateId)(addr))
            }
          }
        }
      }
      case m: StreamMux => Mux(genBool(m.cond, useNextToken), genBits(m.t, useNextToken), genBits(m.f, useNextToken))
      case b: StreamBool => genBool(b, useNextToken) // treat the bool as regular bits
      case v: StreamVar => if (useNextToken == CUR_TICK) chiselVars(v.stateId) else nextVars(v.stateId) // don't need
      // Mux(pipeFinishing, ...) around nextVars because any regs it depends on are already muxed
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
      case f: StreamFinished.type => if (useNextToken == CUR_TICK) finishedReg else
        // unless there is no current input or we are about to flush the current input, use the current value
        Mux(!inputRegValid || (pipeFinishing && swhileDone), io.inputFinished, finishedReg)
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
      case i: StreamInput.type => "input[min(i, input_len - 1)]"
      case s: BitSelect => s"(((uint64_t)${genCBits(s.arg)} >> ${s.lower}) & ((1L << ${s.upper - s.lower + 1}) - 1))"
      case r: StreamReg => s"reg${r.stateId}_read"
      case b: BRAMSelect => s"bram${b.arg.stateId}_read[min(${genCBits(b.idx)}, ${b.arg.numEls - 1})]"
      case v: VectorRegSelect => s"vec${v.arg.stateId}_read[min(${genCBits(v.idx)}, ${v.arg.numEls - 1})]"
      case m: StreamMux => s"(${genCBool(m.cond)} ? ${genCBits(m.t)} : ${genCBits(m.f)})"
      case b: StreamBool => genCBool(b) // treat the bool as regular bits
      case v: StreamVar => s"var_${v.stateId}"
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
      case f: StreamFinished.type => "(i == input_len)"
      case _ => throw new StreamException("unexpected type in genCBool: " + b.getClass.toString)
    }
  }

  def compile(): Unit = {
    val regWrites = new Array[ArrayBuffer[(StreamBool, Boolean, StreamBits)]](regs.length) // cond, is in swhile, data
    for ((r, i) <- regs.zipWithIndex) {
      if (r.init != null) {
        chiselRegs.append(RegInit(r.init.asUInt(r.width.W)))
      } else {
        chiselRegs.append(Reg(UInt(r.width.W)))
      }
      regWrites(i) = new ArrayBuffer[(StreamBool, Boolean, StreamBits)]
    }
    val vectorRegWrites = new Array[
      ArrayBuffer[(StreamBool, Boolean, StreamBits, StreamBits)]](vectorRegs.length) // cond, is in swhile, addr, data
    for (i <- 0 until vectorRegWrites.length) {
      vectorRegWrites(i) = new ArrayBuffer[(StreamBool, Boolean, StreamBits, StreamBits)]
    }
    for ((cond, isInSwhile, a) <- assignments) {
      a.lhs match {
        case v: VectorRegSelect => vectorRegWrites(v.arg.stateId).append((cond, isInSwhile, v.idx, a.rhs))
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
    val bramReads = new Array[ArrayBuffer[(StreamBool, Boolean, StreamBits)]](brams.length) // cond, has priority, addr
    val bramWrites = new Array[ArrayBuffer[(StreamBool, Boolean, StreamBits, StreamBits)]](brams.length) // cond,
    // is in swhile, addr, data
    for ((b, i) <- brams.zipWithIndex) {
      chiselBrams.append(Module(new DualPortBRAM(b.width, util.log2Ceil(b.numEls))))
      bramReads(i) = new ArrayBuffer[(StreamBool, Boolean, StreamBits)]
      bramWrites(i) = new ArrayBuffer[(StreamBool, Boolean, StreamBits, StreamBits)]
    }
    // separate loop for initialization because one chiselVar may depend on another
    for ((varWidth, _) <- vars) {
      chiselVars.append(Wire(UInt(varWidth.W)))
    }
    for (((_, varExpr), i) <- vars.zipWithIndex) {
      chiselVars(i) := genBits(varExpr, CUR_TICK)
    }

    def assignIdxOr0(a: AssignableStreamData): StreamBits = {
      a match {
        case v: VectorRegSelect => v.idx
        case b: BRAMSelect => b.idx
        case _ => 0.L
      }
    }

    // BRAM read preprocessing
    val depTable = new Array[(mutable.Set[Int], mutable.Set[StreamBits], Boolean)](brams.length)
    for (i <- 0 until depTable.length) {
      depTable(i) = (new mutable.HashSet[Int], new mutable.HashSet[StreamBits], false)
    }
    for ((cond, isInSwhile, a) <- assignments) {
      val condReads = collectAllReads(cond, new mutable.HashSet[Int])
      addRAMReads(cond, isInSwhile, a.rhs, bramReads)
      addDependencies(a.rhs, condReads, depTable)
      addRAMReads(cond, isInSwhile, assignIdxOr0(a.lhs), bramReads)
      addDependencies(assignIdxOr0(a.lhs), condReads, depTable)
    }
    for ((cond, isInSwhile, e) <- emits) {
      addRAMReads(cond, isInSwhile, e.data, bramReads)
      addDependencies(e.data, collectAllReads(cond, new mutable.HashSet[Int]), depTable)
    }
    for ((cond, c0) <- conds) {
      addRAMReads(cond, true, c0, bramReads)
      addDependencies(c0, collectAllReads(cond, new mutable.HashSet[Int]), depTable)
    }
    val readDepths = determineReadDepths(depTable)
    val maxReadDepth = if (readDepths.length == 0) 1 else readDepths.max

    var swhileDoneTmp = true.B
    for (cond <- swhileConds) {
      swhileDoneTmp = swhileDoneTmp && !genBool(cond, CUR_TICK)
    }
    swhileDone := swhileDoneTmp

    val nextTickMemsReady = Wire(Bool())
    val pipeState = RegInit(0.asUInt(util.log2Ceil(maxReadDepth + 1).W))
    val pipeActive = pipeState =/= 0.U
    io.inputReady := (!pipeActive && !inputRegValid) || (pipeFinishing && swhileDone && nextTickMemsReady)
    pipeFinalState := pipeState === maxReadDepth.U
    // TODO if this tick doesn't do any depth 1 RAM reads and we have a pipeline with depth > 1,
    // it may be possible to run the tick in fewer cycles by considering the depth 2 reads to be done
    // immediately, since there are no depth 1 reads they need to wait for
    when (pipeActive && !pipeFinalState) {
      pipeState := pipeState + 1.U
    }
    when (!pipeActive) {
      when (!inputRegValid) { // no token now, so try to accept new input
        inputReg := io.inputWord
        finishedReg := io.inputFinished
        inputRegValid := (io.inputFinished && !finishedReg) || io.inputValid
        pipeState := Mux((io.inputFinished && !finishedReg) || io.inputValid, 1.U, 0.U)
      } .otherwise { // we have a max one cycle delay for BRAM writes to commit at the end of the previous tick, so must
        // be safe to reactivate pipe
        pipeState := 1.U
      }
    } .elsewhen (pipeFinishing) { // active tick in the pipeline that had no output or whose output is accepted
      when (swhileDone) { // current token done, so try to accept new input
        inputReg := io.inputWord
        when (nextTickMemsReady) {
          finishedReg := io.inputFinished
        }
        inputRegValid := ((io.inputFinished && !finishedReg) || io.inputValid) && nextTickMemsReady // mark the input
        // valid even for the finished token to keep the pipe state machine in order
        pipeState := Mux(((io.inputFinished && !finishedReg) || io.inputValid) && nextTickMemsReady, 1.U, 0.U)
      } .otherwise { // may be possible to start another tick now
        pipeState := Mux(nextTickMemsReady, 1.U, 0.U)
      }
    }
    io.outputFinished := finishedReg && !pipeActive

    // emits
    var curEmit = genBits(emits(0)._3.data, CUR_TICK)
    var emitValid = genBool(emits(0)._1, CUR_TICK) && (if (emits(0)._2) true.B else swhileDone)
    for ((cond, isInSwhile, e) <- emits.drop(1)) {
      val valid = genBool(cond, CUR_TICK) && (if (isInSwhile) true.B else swhileDone)
      curEmit = Mux(valid, genBits(e.data, CUR_TICK), curEmit)
      emitValid = emitValid || valid
    }
    io.outputValid := pipeFinalState && emitValid
    io.outputWord := curEmit

    // register writes
    nextRegs = new Array[UInt](chiselRegs.length)
    for ((cond, isInSwhile, a) <- assignments) {
      a.lhs match {
        case r: StreamReg => regWrites(r.stateId).append((cond, isInSwhile, a.rhs))
        case _ =>
      }
    }
    for (((cr, writes), i) <- chiselRegs.zip(regWrites).zipWithIndex) {
      var data = cr
      for ((cond, isInSwhile, d) <- writes) {
        data = Mux(genBool(cond, CUR_TICK) && (if (isInSwhile) true.B else swhileDone), genBits(d, CUR_TICK), data)
      }
      nextRegs(i) = data
      cr := Mux(pipeFinishing, data, cr)
    }

    // vector register writes
    nextVectorRegs = new Array[(UInt, UInt)](chiselVectorRegs.length)
    for (((cv, writes), vecIdx) <- chiselVectorRegs.zip(vectorRegWrites).zipWithIndex) {
      if (writes.length > 0) { // don't write anything if no user-defined writes so that ROM will be synthesized
        var idx = genBits(writes(0)._3, CUR_TICK)
        for ((cond, isInSwhile, i, _) <- writes.drop(1)) {
          idx = Mux(genBool(cond, CUR_TICK) && (if (isInSwhile) true.B else swhileDone), genBits(i, CUR_TICK), idx)
        }
        var data = cv(idx)
        for ((cond, isInSwhile, _, d) <- writes) {
          data = Mux(genBool(cond, CUR_TICK) && (if (isInSwhile) true.B else swhileDone), genBits(d, CUR_TICK), data)
        }
        nextVectorRegs(vecIdx) = (idx, data)
        cv(idx) := Mux(pipeFinishing, data, cv(idx))
      }
    }

    // next vars
    nextVars = new Array[UInt](vars.length)
    for (((varWidth, _), i) <- vars.zipWithIndex) {
      nextVars(i) = Wire(UInt(varWidth.W))
    }
    for (((_, varExpr), i) <- vars.zipWithIndex) {
      nextVars(i) := genBits(varExpr, NEXT_TICK)
    }

    // BRAM reads
    for ((cb, reads) <- chiselBrams.zip(bramReads)) {
      cb.io.a_wr := false.B
      if (reads.length > 0) {
        // If the current tick is being flushed, we send its results into the RAM read ports
        // so that RAM reads will be available for the next input (if there is one) on the next cycle. The
        // RAM read addresses are guaranteed to be based only on registers (and not other RAM reads) due to the
        // pipeDepth <= 1 condition, so we know that the next addresses will be immediately available as the
        // pipeline is being flushed. RAM reads are the only case (besides the nextTickDoesRamReadAtDepth1 flags below)
        // where we need to use signals based on the incoming input and incoming register values rather than the
        // registered input and current register values.
        var addr: UInt = null
        // Priority is given to reads coming from within an swhile as well as all condition reads. This ensures that
        // assign/emit reads that occur on the post-while tick and have true conditions during the swhile do not
        // conflict with swhile reads. Our semantics are thus that all condition reads that themselves have true
        // conditions execute on each tick, and assign/emit reads occur in while/post-while order. This can still
        // lead to some nonintuitive conflict cases across conditions and assigns/emits, like the following:
        // if (a[x]) { # no post-while statements in this block
        //   while (...)
        // }
        // if (...) {
        //   z := a[y] + 1 # a[x] conflicts with a[y] even though they never need to occur on the same tick
        // }
        for ((cond, hasPriority, d) <- reads) {
          if (!hasPriority) {
            addr = if (addr == null) genBits(d, NEXT_TICK)
              else Mux(genBool(cond, NEXT_TICK), genBits(d, NEXT_TICK), addr)
          }
        }
        for ((cond, hasPriority, d) <- reads) {
          if (hasPriority) {
            addr = if (addr == null) genBits(d, NEXT_TICK)
              else Mux(genBool(cond, NEXT_TICK), genBits(d, NEXT_TICK), addr)
          }
        }
        cb.io.a_addr := addr
      }
    }

    // BRAM writes
    for ((cond, isInSwhile, a) <- assignments) {
      a.lhs match {
        case b: BRAMSelect => bramWrites(b.arg.stateId).append((cond, isInSwhile, b.idx, a.rhs))
        case _ =>
      }
    }
    for (((cb, writes), i) <- chiselBrams.zip(bramWrites).zipWithIndex) {
      if (writes.length > 0) {
        var wr = genBool(writes(0)._1, CUR_TICK) && (if (writes(0)._2) true.B else swhileDone)
        var addr = genBits(writes(0)._3, CUR_TICK)
        var data = genBits(writes(0)._4, CUR_TICK)
        for ((cond, isInSwhile, a, d) <- writes.drop(1)) {
          val valid = genBool(cond, CUR_TICK) && (if (isInSwhile) true.B else swhileDone)
          wr = wr || valid
          addr = Mux(valid, genBits(a, CUR_TICK), addr)
          data = Mux(valid, genBits(d, CUR_TICK), data)
        }
        cb.io.b_wr := wr && pipeFinishing
        cb.io.b_addr := addr
        cb.io.b_din := data
      } else {
        cb.io.b_wr := false.B
      }
    }

    // if the returned condition evaluates to false, cond must also evaluate to false
    def maximalNonRamReadingCond(cond: StreamBool): StreamBool = {
      if (collectAllReads(cond, new mutable.HashSet[Int]).size == 0) {
        cond
      } else {
        cond match {
          case And(arg1, arg2) => And(maximalNonRamReadingCond(arg1), maximalNonRamReadingCond(arg2))
          case _ => true.L.B
        }
      }
    }

    def getDepth1Reads(b: StreamBits): mutable.Set[Int] = {
      collectAllReads(b, new mutable.HashSet[Int]).filter(r => readDepths(r) == 1)
    }

    val nextTickDoesRamReadAtDepth1 = (0 until brams.length).map(_ => false.B).toArray
    // TODO not having the muxes in genBits/genBool for the NEXT_TICK could save logic
    // here if RAM reads only occur under one condition, in which case the condition is not already
    // computed with the muxes to select the correct address for the pipelined read
    // TODO these rules assume that assign/emit RAM reads in the post-while tick for the next token occur immediately
    // on the next tick
    for ((c, _, a) <- assignments) {
      val maxC = maximalNonRamReadingCond(c)
      for (r <- getDepth1Reads(assignIdxOr0(a.lhs)).union(getDepth1Reads(a.rhs))) {
        nextTickDoesRamReadAtDepth1(r) = nextTickDoesRamReadAtDepth1(r) || genBool(maxC, NEXT_TICK)
      }
    }
    for ((c, _, e) <- emits) {
      val maxC = maximalNonRamReadingCond(c)
      for (r <- getDepth1Reads(e.data)) {
        nextTickDoesRamReadAtDepth1(r) = nextTickDoesRamReadAtDepth1(r) || genBool(maxC, NEXT_TICK)
      }
    }
    for ((c, cond) <- conds) {
      val maxC = maximalNonRamReadingCond(c)
      for (r <- getDepth1Reads(cond)) {
        nextTickDoesRamReadAtDepth1(r) = nextTickDoesRamReadAtDepth1(r) || genBool(maxC, NEXT_TICK)
      }
    }
    // rule that checks whether there is a write-read conflict on any depth 1 RAM
    // TODO may be profitable to remove nextTickDoesRamReadAtDepth1 entries in pipelines where they don't lead to
    // higher throughput
    var nextTickMemsReadTmp = true.B
    for ((cb, nextRead) <- chiselBrams.zip(nextTickDoesRamReadAtDepth1)) {
      nextTickMemsReadTmp = nextTickMemsReadTmp && (!cb.io.b_wr || !nextRead)
    }
    nextTickMemsReady := nextTickMemsReadTmp
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
    val simVarsCache = new Array[BigInt](vars.length)
    var emitOccurred = false

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
    var inputFinished: Boolean = false

    def truncate(b: BigInt, bits: Int): BigInt = {
      b & ((BigInt(1) << bits) - 1)
    }

    def genSimBits(b: StreamBits): BigInt = {
      b match {
        case l: Literal => l.l
        case a: Add => genSimBits(a.first) + genSimBits(a.second)
        case s: Subtract => {
          val first = genSimBits(s.first)
          val second = genSimBits(s.second)
          require(first >= second)
          first - second
        }
        case c: Concat => (genSimBits(c.first) << c.second.getWidth) | genSimBits(c.second)
        case i: StreamInput.type => inputWord
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
        case m: StreamMux => if (genSimBool(m.cond)) genSimBits(m.t) else genSimBits(m.f)
        case b: StreamBool => genSimBool(b) // treat the bool as regular bits
        case v: StreamVar => {
          if (simVarsCache(v.stateId) == null) {
            simVarsCache(v.stateId) = genSimBits(vars(v.stateId)._2) & ((BigInt(1) << vars(v.stateId)._1) - 1)
          }
          simVarsCache(v.stateId)
        }
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
        case f: StreamFinished.type => inputFinished
        case _ => throw new StreamException("unexpected type in genSimBool: " + b.getClass.toString)
      }
    }

    // only evaluate as much as we need so that we don't trigger multiple reads from a single BRAM if it doesn't
    // actually need to happen
    def evalFullCond(cond: Seq[StreamBool]): Boolean = {
      for (c <- cond) {
        if (!genSimBool(c)) {
          return false
        }
      }
      return true
    }

    var numOutputBits = 0
    var output = BigInt(0)
    for (i <- 0 until numInputBits + 1 by inputWidth) {
      inputWord = (inputBits >> i) & ((BigInt(1) << inputWidth) - 1)
      inputFinished = i == numInputBits
      var simSwhileDone = false
      do {
        simSwhileDone = fullSwhileConds.map(c => !evalFullCond(c)).foldLeft(true)((b1, b2) => b1 && b2)
        for ((cond, isInSwhile, a) <- fullAssignments) {
          if (evalFullCond(cond) && (simSwhileDone || isInSwhile)) { // semantics require us to always evaluate
            // the condition, even if we already know we won't be executing the assignment because it's post-while
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
        for ((cond, isInSwhile, e) <- fullEmits) {
          if (evalFullCond(cond) && (simSwhileDone || isInSwhile)) {
            require(!emitOccurred)
            emitOccurred = true
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
        for (i <- 0 until simVarsCache.length) {
          simVarsCache(i) = null
        }
        emitOccurred = false
      } while (!simSwhileDone)
    }
    (numOutputBits, output)
  }

  class CWriter(outputFile: File) {
    var indentLevel = 0
    val pw = new PrintWriter(outputFile)
    def writeLine(line: String): Unit = {
      if (line.startsWith("}") || line.startsWith("};")) {
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

    val varGenerated = new Array[Boolean](vars.length)
    def generateContainedVars(cw: CWriter, b: StreamBits): Unit = {
      b match {
        case v: StreamVar => generateVar(cw, v.stateId)
        case _ =>
          b.productIterator.foreach {
            case s: StreamBits => generateContainedVars(cw, s)
            case _ =>
          }
      }
    }
    def generateVar(cw: CWriter, varId: Int): Unit = {
      if (!varGenerated(varId)) {
        generateContainedVars(cw, vars(varId)._2)
        cw.writeLine(s"uint${getCWidthForBitWidth(vars(varId)._1)}_t var_${varId} = ${genCBits(vars(varId)._2)};")
        varGenerated(varId) = true
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
    cw.writeLine("for (uint32_t i = 0; i <= input_len; i++) {")
    cw.writeLine("uint8_t swhile_done = 0;")
    cw.writeLine("do {")
    for (i <- 0 until vars.length) {
      generateVar(cw, i)
    }
    cw.writeLine(
      s"swhile_done = ${genCBool(swhileConds.foldLeft(true.L.B.asInstanceOf[StreamBool])((b1, b2) => b1 && !b2))};")
    for ((cond, isInSwhile, a) <- assignments) {
      cw.writeLine(s"if (${if (isInSwhile) "" else "swhile_done && "}${genCBool(cond)}) {")
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
    for ((cond, isInSwhile, e) <- emits) {
      cw.writeLine(s"if (${if (isInSwhile) "" else "swhile_done && "}${genCBool(cond)}) {")
      cw.writeLine(s"output[output_count++] = ${genCBits(e.data)};")
      cw.writeLine("}")
    }
    for (i <- 0 until regs.length) {
      cw.writeLine(s"reg${i}_read = reg${i}_write;")
    }
    emitVectorWriteToRead(cw, vectorRegs.asInstanceOf[ArrayBuffer[Any]])
    emitVectorWriteToRead(cw, brams.asInstanceOf[ArrayBuffer[Any]])
    cw.writeLine("} while (!swhile_done);")
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
