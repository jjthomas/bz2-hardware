package examples

import chisel3._
import chisel3.core.{Reg, Bundle, Module}

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
  val inputFinished = Output(Bool())
  val outputFinished = Output(Bool())
  val init = Input(Bool())
}

// TODO current limitation: all addresses must be 512-bit aligned
class StreamingCore(inputAddrPtr: Long, outputAddrPtr: Long) extends Module {
  val io = IO(new StreamingCoreIO)
  val core = Module(new PassThrough)

  val isInitInput1 = Reg(init = false.B)
  val isInitInput2 = Reg(init = false.B)
  val isInitOutput = Reg(init = false.B)
  val initDone = Reg(init = false.B)
  val finalInputAddr = Reg(UInt(64.W))
  val outputFinished = Reg(init = false.B)
  val inputMemAddr = Reg(init = (inputAddrPtr + 64).asUInt(64.W))
  val inputMemRequested = Reg(init = false.B)
  val outputMemAddr = Reg(UInt(64.W))
  val outputMemFlushed = Reg(init = false.B)
  when (io.init) {
    isInitInput1 := true.B
  }
  val inputAddressAccepted = Reg(init = false.B)
  io.outputFinished := outputFinished
  io.inputMemAddr := inputMemAddr
  io.inputMemAddrValid := !inputAddressAccepted && (isInitInput1 || isInitInput2 ||
    (initDone && inputMemAddr =/= finalInputAddr))
  when (io.inputMemAddrValid && io.inputMemAddrReady) {
    inputAddressAccepted := true.B
  }
  io.inputMemBlockReady := inputAddressAccepted
  core.io.inputMemBlock := io.inputMemBlock
  core.io.inputMemBlockValid := io.inputMemBlockReady && io.inputMemBlockValid
  when (core.io.inputMemBlockValid) {
    when (isInitInput1) {
      finalInputAddr := core.io.inputMemBlock
      inputMemAddr := inputAddrPtr.asUInt(64.W)
      isInitInput1 := false.B
      isInitInput2 := true.B
    } .elsewhen (isInitOutput) {
      
    } .otherwise {

    }
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
  // TODO may want to divide this into channel groups so that we don't have to do the random access
  // across all cores
  val cores = Vec.do_fill(numCores) { new StreamingCoreIO }
  val curInputCore = new Array[UInt](numInputChannels)
  val curOutputCore = new Array[UInt](numOutputChannels)

  def numCoresForChannel(numChannels: Int, channel: Int): Int = {
    (numCores - 1 - channel) / numChannels + 1
  }
  val inputChannelBounds = new Array[Int](numInputChannels + 1)
  val outputChannelBounds = new Array[Int](numOutputChannels + 1)
  inputChannelBounds(0) = 0
  outputChannelBounds(0) = 0
  for (i <- 0 until numInputChannels) {
    inputChannelBounds(i + 1) = inputChannelBounds(i) + numCoresForChannel(numInputChannels, i)
  }
  for (i <- 0 until numOutputChannels) {
    outputChannelBounds(i + 1) = outputChannelBounds(i) + numCoresForChannel(numOutputChannels, i)
  }
  var curInputChannel = 0
  var curOutputChannel = 0
  for (i <- 0 until numCores) {
    if (i >= inputChannelBounds(curInputChannel + 1)) {
      curInputChannel += 1
    }
    if (i >= outputChannelBounds(curOutputChannel + 1)) {
      curOutputChannel += 1
    }
    _cores(i) = Module(new StreamingCore(inputChannelStartAddrs(curInputChannel) +
      (i - inputChannelBounds(curInputChannel)) * 8,
      outputChannelStartAddrs(curOutputChannel) + (i - outputChannelBounds(curOutputChannel)) + 8))
    cores(i) <> _cores(i).io
  }
  for (i <- 0 until numInputChannels) {
    curInputCore(i) = Reg(init = inputChannelBounds(i).asUInt(util.log2Up(numCores).W))
  }
  for (i <- 0 until numOutputChannels) {
    curOutputCore(i) = Reg(init = outputChannelBounds(i).asUInt(util.log2Up(numCores).W))
  }

  val isInit = Reg(init = false.B)
  for (i <- 0 until numCores) {
    cores(i).init := io.init
  }
  for (i <- 0 until numInputChannels) {
    io.inputMemAddrs(i) := cores(curInputCore(i)).inputMemAddr
    io.inputMemAddrValids(i) := cores(curInputCore(i)).inputMemAddrValid
    cores(curInputCore(i)).inputMemAddrReady := io.inputMemAddrReadys(i)
    cores(curInputCore(i)).inputMemBlock := io.inputMemBlocks(i)
    cores(curInputCore(i)).inputMemBlockValid := io.inputMemBlockValids(i)
    io.inputMemBlockReadys(i) := cores(curInputCore(i)).inputMemBlockReady

    when (cores(curInputCore(i)).inputFinished || (cores(curInputCore(i)).inputMemBlockReady &&
      cores(curInputCore(i)).inputMemBlockValid)) {
      curInputCore(i) := Mux(curInputCore(i) === (inputChannelBounds(i + 1) - 1).U, inputChannelBounds(i).U,
        curInputCore(i) + 1.U)
    }
  }
  for (i <- 0 until numOutputChannels) {
    io.outputMemAddrs(i) := cores(curOutputCore(i)).outputMemAddr
    io.outputMemAddrValids(i) := cores(curOutputCore(i)).outputMemAddrValid
    cores(curOutputCore(i)).outputMemAddrReady := io.outputMemAddrReadys(i)
    io.outputMemBlocks(i) := cores(curOutputCore(i)).outputMemBlock
    io.outputMemBlockValids(i) := cores(curOutputCore(i)).outputMemBlockValid
    cores(curOutputCore(i)).outputMemBlockReady := io.outputMemBlockReadys(i)

    when (cores(curOutputCore(i)).outputFinished || (cores(curOutputCore(i)).outputMemBlockReady &&
      cores(curOutputCore(i)).outputMemBlockValid)) {
      curOutputCore(i) := Mux(curOutputCore(i) === (outputChannelBounds(i + 1) - 1).U, outputChannelBounds(i).U,
        curOutputCore(i) + 1.U)
    }
  }
}