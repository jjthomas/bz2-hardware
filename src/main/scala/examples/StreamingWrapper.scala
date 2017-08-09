package examples

import chisel3._
import chisel3.core.{Bundle, Module}


class StreamingCore extends Module {

}

class StreamingWrapper(numInputChannels: Int, numOutputChannels: Int, numPipes: Int) extends Module {

  val io = IO(new Bundle {
    val inputMemAddrs = Output(Vec(UInt(64.W), numInputChannels))
    val inputMemAddrValids = Output(Vec(Bool(), numInputChannels))
    val inputMemAddrReadys = Input(Vec(Bool(), numInputChannels))
    val inputMemBlocks = Input(Vec(UInt(512.W), numInputChannels))
    val inputMemBlockValids = Input(Vec(Bool(), numInputChannels))
    val inputMemBlockReadys = Output(Vec(Bool(), numInputChannels))
    val outputMemAddrs = Output(Vec(UInt(64.W), numOutputChannels))
    val outputMemAddrValids = Output(Vec(Bool(), numOutputChannels))
    val outputMemAddrReadys = Input(Vec(Bool(), numOutputChannels))
    val outputMemBlocks = Output(Vec(UInt(512.W), numOutputChannels))
    val outputMemBlockValids = Output(Vec(Bool(), numOutputChannels))
    val outputMemBlockReadys = Input(Vec(Bool(), numOutputChannels))
    val init = Input(Bool())
    // these signals held continuously after init is asserted
    val inputOffsetAddrs = Input(Vec(UInt(64.W), numInputChannels))
    val inputStartAddrs = Input(Vec(UInt(64.W), numInputChannels))
    val outputOffsetAddrs = Input(Vec(UInt(64.W), numOutputChannels))
    val outputStartAddrs = Input(Vec(UInt(64.W), numOutputChannels))
  })



  val isInitStage1 = Reg(init = false.B)
  val isInitStage2 = Reg(init = false.B)
  val inputOffsetAddrs = Reg(Vec(UInt(64.W), numInputChannels))
  val inputOffsetRequested = Reg(init = Vec.do_fill(numInputChannels) { false.B })
  val inputOffsetBlocks = Reg(Vec(UInt(512.W), numInputChannels))
  val inputOffsetBlocksValid = Reg(init = Vec.do_fill(numInputChannels) { false.B })
  val inputAddrs = Reg(Vec(UInt(64.W), numPipes))
  val inputBlocks = Reg(Vec(UInt(512.W), numPipes))
  val inputBlocksValid = Reg(init = Vec.do_fill(numPipes) { false.B })
  val outputOffsetAddrs = Reg(Vec(UInt(64.W), numOutputChannels))
  val outputOffsetRequested = Reg(init = Vec.do_fill(numOutputChannels) { false.B })
  val outputOffsetBlocks = Reg(init = Vec.do_fill(numOutputChannels) { 0.asUInt(512.W) })
  val outputOffsetBlocksValid = Reg(init = Vec.do_fill(numOutputChannels) { false.B })
  val outputAddrs = Reg(Vec(UInt(64.W), numPipes))
  val outputBlocks = Reg(Vec(UInt(512.W), numPipes))
  val outputBlocksValid = Reg(init = Vec.do_fill(numPipes) { false.B })
  val initDone = Reg(init = false.B)
  when (io.init) {
    isInitStage1 := true.B
    isInitStage2 := false.B
    initDone := false.B
  }
  when (isInitStage1) {
    for (i <- 0 until numInputChannels) {
      inputOffsetAddrs(i) := io.inputOffsetAddrs(i)
    }
    for (i <- 0 until numOutputChannels) {
      outputOffsetAddrs(i) := io.outputOffsetAddrs(i)
    }
    isInitStage1 := false.B
    isInitStage2 := true.B
  }
  when (isInitStage2) {
    for (i <- 0 until numInputChannels) {
      when (!inputOffsetBlocksValid(i) && inputOffsetRequested(i) && io.inputMemAddrReadys(i)) {
        io.inputMemAddrValids(i) := true.B
        io.inputMemAddrs(i) := inputOffsetAddrs(i)
        inputOffsetRequested(i) := true.B
      }
      when (inputOffsetRequested(i) && io.inputMemBlockValids(i)) {
        inputOffsetBlocks
      }
    }
  }
}