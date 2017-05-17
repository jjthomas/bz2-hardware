package examples

import chisel3._
import chisel3.util.Cat

class SortingNetwork(logSize: Int, ioEls: Int, cmpSize: Int) extends Module { // wordSize: Int
  val size = 1 << logSize
  val ioElsPub = ioEls
  assert (size % ioEls == 0)

  val io = IO(new Bundle {
    val blockValid = Input(Bool())
    val block = Input(UInt((64 * ioEls).W))
    val downstreamReady = Input(Bool())
    val thisReady = Output(Bool())
    val outValid = Output(Bool())
    val out = Output(UInt((64 * ioEls).W))
  })

  val networkDepth = logSize * (logSize + 1) / 2 + 1 // extra one for the outputs
  val networkStages = new Array[Vec[UInt]](networkDepth)
  val stageValids = new Array[Bool](networkDepth)
  val numFilled = Reg(init = 0.asUInt(logSize.W))
  val numDrained = Reg(init = 0.asUInt(logSize.W))
  val advance = Wire(Bool())

  for (i <- 0 until networkDepth) {
    networkStages(i) = Reg(Vec(size, UInt(64.W)))
    stageValids(i) = Reg(init = false.B)
  }

  io.out := Cat((ioEls - 1 to 0 by -1).map(i => networkStages(networkDepth - 1)(numDrained + i.U)))
  io.outValid := stageValids(networkDepth - 1)

  advance := !stageValids(networkDepth - 1)
  for (i <- 1 until networkDepth) {
    when (advance) {
      stageValids(i) := stageValids(i - 1)
    }
  }

  when (advance && stageValids(0)) {
    stageValids(0) := false.B
  }

  when (io.blockValid && !stageValids(0)) {
    for (i <- 0 until ioEls) {
      networkStages(0)(numFilled + i.U) := io.block((i + 1) * 64 - 1, i * 64)
    }
    when (numFilled === (size - ioEls).U) {
      numFilled := 0.U
      stageValids(0) := true.B
    } .otherwise {
      numFilled := numFilled + ioEls.U
    }
  }
  io.thisReady := !stageValids(0)

  when (io.downstreamReady && stageValids(networkDepth - 1)) {
    when (numDrained === (size - ioEls).U) {
      numDrained := 0.U
      stageValids(networkDepth - 1) := false.B
    } .otherwise {
      numDrained := numDrained + ioEls.U
    }
  }

  def cmp(first: Int, second: Int, depth: Int): Unit = {
    assert(first < second)
    when (advance) {
      when(networkStages(depth)(first)(cmpSize - 1, 0) < networkStages(depth)(second)(cmpSize - 1, 0)) {
        networkStages(depth + 1)(first) := networkStages(depth)(first)
        networkStages(depth + 1)(second) := networkStages(depth)(second)
      } .otherwise {
        networkStages(depth + 1)(first) := networkStages(depth)(second)
        networkStages(depth + 1)(second) := networkStages(depth)(first)
      }
    }
  }

  def makeBitonicFromSorted(start: Int, end: Int, depth: Int): Unit = {
    assert((end - start) % 2 == 0)
    val halfRange = (end - start) / 2
    for (i <- 0 until halfRange) {
      cmp(start + i, end - 1 - i, depth)
    }
  }

  def cutBitonic(start: Int, end: Int, depth: Int): Unit = {
    assert((end - start) % 2 == 0)
    val halfRange = (end - start) / 2
    for (i <- start until start + halfRange) {
      cmp(i, i + halfRange, depth)
    }
  }

  var curDepth = 0
  var curSorted = 1
  while (curSorted < size) {
    for (i <- 0 until size by curSorted * 2) {
      makeBitonicFromSorted(i, i + curSorted * 2, curDepth)
    }
    curDepth += 1
    var curBitonic = curSorted
    while (curBitonic > 1) {
      for (i <- 0 until size by curBitonic) {
        cutBitonic(i, i + curBitonic, curDepth)
      }
      curDepth += 1
      curBitonic /= 2
    }
    curSorted *= 2
  }
  assert(curDepth == networkDepth - 1)

}

