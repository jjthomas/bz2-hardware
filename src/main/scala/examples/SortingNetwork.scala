package examples

import chisel3._

class SortingNetwork(logSize: Int, cmpSize: Int) extends Module { // transferSize: Int, wordSize: Int
  val size = 1 << logSize

  val io = IO(new Bundle {
    val blockValid = Input(Bool())
    val block = Input(UInt(64.W))
    val downstreamReady = Input(Bool())
    val thisReady = Output(Bool())
    val outValid = Output(Bool())
    val out = Output(Vec(size, UInt(64.W)))
  })

  val networkDepth = logSize * (logSize + 1) / 2 + 1 // extra one for the outputs
  val networkStages = new Array[Vec[UInt]](networkDepth)
  val stageValids = new Array[Bool](networkDepth)
  val numFilled = Reg(init = 0.asUInt((logSize + 1).W))

  for (i <- 0 until networkDepth) {
    networkStages(i) = Reg(Vec(size, UInt(64.W)))
    stageValids(i) = Reg(init = false.B)
  }

  for (i <- 0 until size) {
    io.out(i) := networkStages(networkDepth - 1)(i)
  }

  io.outValid := stageValids(networkDepth - 1)

  for (i <- 1 until networkDepth) {
    when (io.downstreamReady) {
      stageValids(i) := stageValids(i - 1)
      when (stageValids(0)) {
        stageValids(0) := false.B
        numFilled := 0.U
      }
    }
  }

  val isSpace = Wire(Bool())
  isSpace := numFilled < size.U
  when (io.blockValid && isSpace) {
    networkStages(0)(numFilled(logSize - 1, 0)) := io.block
    when (numFilled === (size - 1).U) {
      stageValids(0) := true.B
    }
    numFilled := numFilled + 1.U
  }
  io.thisReady := isSpace

  def cmp(first: Int, second: Int, depth: Int): Unit = {
    assert(first < second)
    when (io.downstreamReady) {
      when(networkStages(depth)(first)(cmpSize - 1, 0) < networkStages(depth)(second)(cmpSize - 1, 0)) {
        networkStages(depth + 1)(first) := networkStages(depth)(first)
        networkStages(depth + 1)(second) := networkStages(depth)(second)
      }.otherwise {
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

