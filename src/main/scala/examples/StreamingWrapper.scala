package examples

import chisel3._
import chisel3.core.{Reg, Bundle, Module}

class PassThrough extends Module {
  val io = IO(new Bundle {
    val inputMemBlock = Input(UInt(512.W))
    val inputMemBlockValid = Input(Bool())
    val inputBits = Input(UInt(util.log2Up(513).W))
    val inputMemConsumed = Output(Bool())
    // continuously asserted at least one cycle after inputMemConsumed emitted for final block
    val inputFinished = Input(Bool())
    val outputMemBlock = Output(UInt(512.W))
    // must hold valid until we received flushed signal
    val outputMemBlockValid = Output(Bool())
    val outputBits = Output(UInt(util.log2Up(513).W))
    // hold continuously starting at some point (at least one cycle) after flush of final output
    val outputFinished = Output(Bool())
    val outputMemFlushed = Input(Bool())
  })

  val inputMemBlock = Reg(Vec(512, Bool()))
  val inputBitsRemaining = Reg(init = 0.asUInt(util.log2Up(513).W))
  when (io.inputMemBlockValid) {
    inputBitsRemaining := io.inputBits
    for (i <- 0 until 512) {
      inputMemBlock(i) := io.inputMemBlock(i)
    }
  }
  io.inputMemConsumed := inputBitsRemaining === 0.U && !io.inputFinished

  val outputMemBlock = Reg(Vec(512, Bool()))
  val outputBits = Reg(init = 0.asUInt(util.log2Up(513).W))
  val nextBitValid = Wire(Bool())
  val nextBit = Wire(Bool())
  val inputAdvance = Wire(Bool())
  inputAdvance := outputBits =/= 512.U && inputBitsRemaining =/= 0.U
  when (inputAdvance) {
    inputBitsRemaining := inputBitsRemaining - 1.U
    for (i <- 0 until 511) {
      inputMemBlock(i) := inputMemBlock(i + 1)
    }
  }
  nextBitValid := inputAdvance
  nextBit := inputMemBlock(0)
  when (nextBitValid) {
    outputMemBlock(511) := nextBit
    for (i <- 0 until 511) {
      // important: this means that in a final, partial output block the valid bits will be stored in the upper bits
      // of the block
      outputMemBlock(i) := outputMemBlock(i + 1)
    }
    outputBits := outputBits + 1.U
  }
  io.outputMemBlockValid := outputBits === 512.U || (io.inputFinished && inputBitsRemaining === 0.U && outputBits > 0.U)
  io.outputMemBlock := outputMemBlock.asUInt
  io.outputBits := outputBits
  io.outputFinished := io.inputFinished && inputBitsRemaining === 0.U && outputBits === 0.U
  when (io.outputMemFlushed) {
    outputBits := 0.U
  }
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
}

// TODO current limitation: all addresses must be 512-bit aligned
class StreamingCore(metadataPtr: Long) extends Module {
  val io = IO(new StreamingCoreIO)
  val core = Module(new PassThrough)

  val isInit = Reg(init = true.B)
  val initDone = Reg(init = false.B)
  val inputBitsRemaining = Reg(init = 1.asUInt(64.W)) // init nonzero so that inputFinished isn't immediately asserted
  val coreInputFinished = Reg(init = false.B)
  val outputBits = Reg(init = 0.asUInt(64.W))
  val outputLengthCommitted = Reg(init = false.B)
  val inputMemAddr = Reg(init = metadataPtr.asUInt(64.W))
  val outputMemAddr = Reg(UInt(64.W))
  val outputLenAddr = Reg(UInt(64.W))
  val outputMemFlushed = Reg(init = false.B)

  val inputAddressAccepted = Reg(init = false.B)
  io.inputMemAddr := inputMemAddr
  io.inputMemAddrValid := !inputAddressAccepted && (isInit || (initDone && core.io.inputMemConsumed &&
    !(inputBitsRemaining === 0.U)))
  when (io.inputMemAddrValid && io.inputMemAddrReady) {
    inputAddressAccepted := true.B
  }
  io.inputMemBlockReady := inputAddressAccepted
  core.io.inputMemBlock := io.inputMemBlock
  val inputBlockReadable = Wire(Bool())
  inputBlockReadable := inputAddressAccepted && io.inputMemBlockValid
  core.io.inputMemBlockValid := inputBlockReadable && initDone
  core.io.inputBits := Mux(inputBitsRemaining > 512.U, 512.U, inputBitsRemaining(util.log2Up(513) - 1, 0))
  when (inputBlockReadable) {
    when (isInit) {
      inputMemAddr := io.inputMemBlock(63, 0)
      inputBitsRemaining := io.inputMemBlock(127, 64)
      outputMemAddr := io.inputMemBlock(191, 128) + 64.U
      outputLenAddr := io.inputMemBlock(191, 128)
      isInit := false.B
      initDone := true.B
    } .otherwise {
      inputBitsRemaining := Mux(inputBitsRemaining > 512.U, inputBitsRemaining - 512.U, 0.U)
      inputMemAddr := inputMemAddr + 64.U
    }
    inputAddressAccepted := false.B
  }
  io.inputFinished := inputBitsRemaining === 0.U
  when (core.io.inputMemConsumed && inputBitsRemaining === 0.U) {
    coreInputFinished := true.B
  }
  core.io.inputFinished := coreInputFinished

  val outputAddressAccepted = Reg(init = false.B)
  io.outputMemAddr := Mux(core.io.outputFinished, outputLenAddr, outputMemAddr)
  io.outputMemAddrValid := !outputAddressAccepted && (core.io.outputMemBlockValid ||
    (core.io.outputFinished && !outputLengthCommitted))
  when (io.outputMemAddrValid && io.outputMemAddrReady) {
    outputAddressAccepted := true.B
  }
  io.outputMemBlock := Mux(core.io.outputFinished, outputBits, core.io.outputMemBlock)
  io.outputMemBlockValid := outputAddressAccepted
  core.io.outputMemFlushed := outputAddressAccepted && io.outputMemBlockReady
  when (outputAddressAccepted && io.outputMemBlockReady) {
    outputAddressAccepted := false.B
    // TODO make sure outputFinished can't be set until a cycle after outputMemFlushed is asserted
    when (core.io.outputFinished) {
      outputLengthCommitted := true.B
    } .otherwise {
      outputBits := outputBits + core.io.outputBits
      outputMemAddr := outputMemAddr + 64.U
    }
  }
  io.outputFinished := outputLengthCommitted
}

class StreamingWrapper(val numInputChannels: Int, val inputChannelStartAddrs: Array[Long], val numOutputChannels: Int,
                       val outputChannelStartAddrs: Array[Long], numCores: Int) extends Module {
  val io = IO(new Bundle {
    val inputMemAddrs = Output(Vec(numInputChannels, UInt(64.W)))
    val inputMemAddrValids = Output(Vec(numInputChannels, Bool()))
    val inputMemAddrReadys = Input(Vec(numInputChannels, Bool()))
    val inputMemBlocks = Input(Vec(numInputChannels, UInt(512.W)))
    val inputMemBlockValids = Input(Vec(numInputChannels, Bool()))
    val inputMemBlockReadys = Output(Vec(numInputChannels, Bool()))
    val outputMemAddrs = Output(Vec(numOutputChannels, UInt(64.W)))
    val outputMemAddrValids = Output(Vec(numOutputChannels, Bool()))
    val outputMemAddrReadys = Input(Vec(numOutputChannels, Bool()))
    val outputMemBlocks = Output(Vec(numOutputChannels, UInt(512.W)))
    val outputMemBlockValids = Output(Vec(numOutputChannels, Bool()))
    val outputMemBlockReadys = Input(Vec(numOutputChannels, Bool()))
    val finished = Output(Bool())
  })

  val _cores = new Array[StreamingCore](numCores)
  val curInputCore = new Array[UInt](numInputChannels)
  val curOutputCore = new Array[UInt](numOutputChannels)

  def numCoresForInputChannel(channel: Int): Int = {
    (numCores - 1 - channel) / numInputChannels + 1
  }
  def numCoresForOutputChannel(channel: Int): Int = {
    (numCores - 1 - channel) / numOutputChannels + 1
  }
  // TODO we should pass in these bounds as arguments so there is a well-defined mapping from
  // input to output streams that doesn't depend on the details of the below code
  val inputChannelBounds = new Array[Int](numInputChannels + 1)
  val outputChannelBounds = new Array[Int](numOutputChannels + 1)
  inputChannelBounds(0) = 0
  outputChannelBounds(0) = 0
  for (i <- 0 until numInputChannels) {
    inputChannelBounds(i + 1) = inputChannelBounds(i) + numCoresForInputChannel(i)
  }
  for (i <- 0 until numOutputChannels) {
    outputChannelBounds(i + 1) = outputChannelBounds(i) + numCoresForOutputChannel(i)
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
      (i - inputChannelBounds(curInputChannel)) * 64))
  }
  // TODO may want to divide this into channel groups so that we don't have to do the random access
  // across all cores
  val cores = Vec(_cores.map(_.io))
  for (i <- 0 until numInputChannels) {
    curInputCore(i) = Reg(init = inputChannelBounds(i).asUInt(util.log2Up(Math.max(numCores, 2)).W))
  }
  for (i <- 0 until numOutputChannels) {
    curOutputCore(i) = Reg(init = outputChannelBounds(i).asUInt(util.log2Up(Math.max(numCores, 2)).W))
  }

  for (i <- 0 until numInputChannels) {
    io.inputMemAddrs(i) := cores(curInputCore(i)).inputMemAddr
    io.inputMemAddrValids(i) := cores(curInputCore(i)).inputMemAddrValid
    io.inputMemBlockReadys(i) := cores(curInputCore(i)).inputMemBlockReady
    for (j <- inputChannelBounds(i) until inputChannelBounds(i + 1)) {
      cores(j).inputMemAddrReady := Mux(curInputCore(i) === j.U, io.inputMemAddrReadys(i), false.B)
      cores(j).inputMemBlock := Mux(curInputCore(i) === j.U, io.inputMemBlocks(i), 0.U)
      cores(j).inputMemBlockValid := Mux(curInputCore(i) === j.U, io.inputMemBlockValids(i), false.B)
    }

    when (cores(curInputCore(i)).inputFinished || (cores(curInputCore(i)).inputMemBlockReady &&
      cores(curInputCore(i)).inputMemBlockValid)) {
      curInputCore(i) := Mux(curInputCore(i) === (inputChannelBounds(i + 1) - 1).U, inputChannelBounds(i).U,
        curInputCore(i) + 1.U)
    }
  }
  for (i <- 0 until numOutputChannels) {
    io.outputMemAddrs(i) := cores(curOutputCore(i)).outputMemAddr
    io.outputMemAddrValids(i) := cores(curOutputCore(i)).outputMemAddrValid
    io.outputMemBlocks(i) := cores(curOutputCore(i)).outputMemBlock
    io.outputMemBlockValids(i) := cores(curOutputCore(i)).outputMemBlockValid
    for (j <- outputChannelBounds(i) until outputChannelBounds(i + 1)) {
      cores(j).outputMemAddrReady := Mux(curOutputCore(i) === j.U, io.outputMemAddrReadys(i), false.B)
      cores(j).outputMemBlockReady := Mux(curOutputCore(i) === j.U, io.outputMemBlockReadys(i), false.B)
    }

    when (cores(curOutputCore(i)).outputFinished || (cores(curOutputCore(i)).outputMemBlockReady &&
      cores(curOutputCore(i)).outputMemBlockValid)) {
      curOutputCore(i) := Mux(curOutputCore(i) === (outputChannelBounds(i + 1) - 1).U, outputChannelBounds(i).U,
        curOutputCore(i) + 1.U)
    }
  }
  var cumFinished = cores(0).outputFinished
  for (i <- 1 until numCores) {
    cumFinished = cumFinished && cores(i).outputFinished
  }
  io.finished := cumFinished
}