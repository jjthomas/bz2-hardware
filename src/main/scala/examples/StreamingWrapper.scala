package examples

import chisel3._
import chisel3.core.{Bundle, Module}

class PassThrough extends Module {
  val io = IO(new Bundle {
    val inputMemBlock = Input(UInt(512.W))
    val inputMemBlockValid = Input(Bool())
    val inputBits = Input(UInt(util.log2Up(512).W))
    val inputMemConsumed = Output(Bool())
    val outputMemBlock = Output(UInt(512.W))
    val outputMemBlockValid = Output(Bool())
    val outputBits = Output(UInt(util.log2Up(512).W))
    val outputFinished = Output(Bool())
    val outputMemFlushed = Input(Bool())
  })

  val inputMemBlock = Reg(UInt(512.W))
  val inputBitsRemaining = Reg(init = 0.asUInt(util.log2Up(513).W))
  val outputMemBlock = Reg(UInt(512.W))
  val outputBits = Reg(init = 0.asUInt(util.log2Up(513).W))


}

class StreamingCoreIO extends Bundle {
  val inputMemAddr = Output(UInt(64.W))
  val inputMemAddrValid = Output(Bool())
  val inputMemAddrReady = Input(Bool())
  val inputMemBlock = Input(UInt(512.W))
  val inputMemBlockValid = Input(Bool())
  val inputMemBlockReady = Output(Bool())
  val outputMemAddr = Output(UInt(64.W))
  val outputMemAddrValid = Output(Bool())
  val outputMemAddrReady = Input(Bool())
  val outputMemBlock = Output(UInt(512.W))
  val outputMemBlockValid = Output(Bool())
  val outputMemBlockReady = Input(Bool())
  val finished = Output(Bool())
  val init = Input(Bool())
}

class StreamingCore(inputAddrPtr: Long, outputAddrPtr: Long) extends Module {
  val io = IO(new StreamingCoreIO)

  val isInit = Reg(init = false.B)
  val initDone = Reg(init = false.B)
  val length = Reg(UInt(32.W))
  val inputMemAddr = Reg(UInt(64.W))
  val inputMemRequested = Reg(init = false.B)
  val outputMemAddr = Reg(UInt(64.W))
  val outputMemFlushed = Reg(init = false.B)
  when (io.init) {
    isInit := true.B
    initDone := false.B
  }
  when (isInit) {

  }

}

class StreamingWrapper(numInputChannels: Int, inputChannelStartAddrs: Array[Long], numOutputChannels: Int,
                       outputChannelStartAddrs: Array[Long], numCores: Int) extends Module {
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
  })

  val _cores = new Array[StreamingCore](numCores)
  val cores = Vec.do_fill(numCores) { new StreamingCoreIO }
  val curInputCore = new Array[UInt](numInputChannels)
  val curOutputCore = new Array[UInt](numOutputChannels)
  for (i <- 0 until numCores) {
    val inputChannel = i % numInputChannels
    val outputChannel = i % numOutputChannels
    val inputIdx = i / numInputChannels
    val outputIdx = i / numOutputChannels
    _cores(i) = Module(new StreamingCore(inputChannelStartAddrs(inputChannel) + inputIdx * 8,
      outputChannelStartAddrs(outputChannel) + outputIdx + 8))
    cores(i) <> _cores(i).io
  }
  def numCoresForChannel(numChannels: Int, channel: Int): Int = {
    (numCores - 1 - channel) / numChannels + 1
  }
  for (i <- 0 until numInputChannels) {
    curInputCore(i) = Reg(UInt(util.log2Up(numCoresForChannel(numInputChannels, i)).W))
  }
  for (i <- 0 until numOutputChannels) {
    curOutputCore(i) = Reg(UInt(util.log2Up(numCoresForChannel(numOutputChannels, i)).W))
  }

  val isInit = Reg(init = false.B)
  for (i <- 0 until numCores) {
    cores(i).init := io.init
  }
  when (io.init) {
    for (i <- 0 until numInputChannels) {
      curInputCore(i) := 0.asUInt(util.log2Up(numCoresForChannel(numInputChannels, i)).W)
    }
    for (i <- 0 until numOutputChannels) {
      curOutputCore(i) := 0.asUInt(util.log2Up(numCoresForChannel(numOutputChannels, i)).W)
    }
  }
  for (i <- 0 until numInputChannels) {
    io.inputMemAddrs(i) := cores(i).inputMemAddr

  }
}