package examples

import chisel3._

class SortingNetwork(logSize: Int, cmpSize: Int) extends Module { // transferSize: Int, wordSize: Int
  val size = 1 << logSize

  val io = IO(new Bundle {
    val blockValid = Input(Bool())
    val block = Input(UInt(64.W))
    val downstreamReady = Input(Bool())
    val reset = Input(Bool())
    val thisReady = Output(Bool())
    val outValid = Output(Bool())
    val out = Output(UInt(64.W))
  })

  val networkDepth = logSize * (logSize + 1) / 2 + 1 // extra one for the outputs
  val networkStages = new Array[Vec[UInt]](networkDepth)
  val stageValids = new Array[Bool](networkDepth)
  val numFilled = Reg(init = 0.asUInt((logSize + 1).W))
  val numDrained = Reg(init = 0.asUInt((logSize + 1).W))
  val advance = Wire(Bool())

  for (i <- 0 until networkDepth) {
    networkStages(i) = Reg(Vec(size, UInt(64.W)))
    stageValids(i) = Reg(init = false.B)
  }

  io.out := networkStages(networkDepth - 1)(numDrained)
  io.outValid := stageValids(networkDepth - 1)

  when (io.reset) {
    numFilled := 0.U
    numDrained := 0.U
  }
  for (i <- 0 until networkDepth) {
    when (io.reset) {
      stageValids(i) := false.B
    }
  }

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
    networkStages(0)(numFilled(logSize - 1, 0)) := io.block
    when (numFilled === (size - 1).U) {
      numFilled := 0.U
      stageValids(0) := true.B
    } .otherwise {
      numFilled := numFilled + 1.U
    }
  }
  io.thisReady := !stageValids(0)

  when (io.downstreamReady && stageValids(networkDepth - 1)) {
    when (numDrained === (size - 1).U) {
      numDrained := 0.U
      stageValids(networkDepth - 1) := false.B
    } .otherwise {
      numDrained := numDrained + 1.U
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

