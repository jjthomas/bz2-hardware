package examples

import chisel3._
import chisel3.core.{Reg, Bundle, Module}

class PassThrough extends Module {
  val io = IO(new Bundle {
    val inputMemBlock = Input(UInt(64.W))
    val inputMemBlockValid = Input(Bool())
    val inputBits = Input(UInt(util.log2Up(65).W))
    val inputMemConsumed = Output(Bool())
    // continuously asserted at least one cycle after inputMemConsumed emitted for final block
    val inputFinished = Input(Bool())
    val outputMemBlock = Output(UInt(64.W))
    // must hold valid until we received flushed signal
    val outputMemBlockValid = Output(Bool())
    val outputBits = Output(UInt(util.log2Up(65).W))
    // hold continuously starting at some point (at least one cycle) after flush of final output
    val outputFinished = Output(Bool())
    val outputMemFlushed = Input(Bool())
  })

  val inputMemBlock = Reg(Vec(64, Bool()))
  val inputBitsRemaining = RegInit(0.asUInt(util.log2Up(65).W))
  val outputMemBlock = Reg(Vec(64, Bool()))
  val outputBits = RegInit(0.asUInt(util.log2Up(65).W))

  val inputAdvance = Wire(Bool())
  inputAdvance := outputBits =/= 64.U && inputBitsRemaining =/= 0.U
  when (io.inputMemBlockValid) {
    inputBitsRemaining := io.inputBits
    for (i <- 0 until 64) {
      inputMemBlock(i) := io.inputMemBlock(i)
    }
  } .elsewhen (inputAdvance) {
    inputBitsRemaining := inputBitsRemaining - 1.U
    for (i <- 0 until 63) {
      inputMemBlock(i) := inputMemBlock(i + 1)
    }
  }

  val nextBitValid = Wire(Bool())
  val nextBit = Wire(Bool())
  nextBitValid := inputAdvance
  nextBit := inputMemBlock(0)
  when (nextBitValid) {
    outputMemBlock(63) := nextBit
    for (i <- 0 until 63) {
      // important: this means that in a final, partial output block the valid bits will be stored in the upper bits
      // of the block
      outputMemBlock(i) := outputMemBlock(i + 1)
    }
    outputBits := outputBits + 1.U
  }

  io.inputMemConsumed := inputBitsRemaining === 0.U
  io.outputMemBlockValid := outputBits === 64.U || (io.inputFinished && inputBitsRemaining === 0.U && outputBits > 0.U)
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
class StreamingCore(metadataPtr: Long, coreId: Int) extends Module {
  val io = IO(new StreamingCoreIO)
  val cores = new Array[PassThrough](8)
  for (i <- 0 until 8) {
    cores(i) = Module(new PassThrough)
  }

  val isInit = RegInit(true.B)
  val initDone = RegInit(false.B)
  // init nonzero so that inputFinished isn't immediately asserted
  val inputBitsRemainingPerCore = RegInit(VecInit((0 until 8).map(_ => 1.asUInt(64.W))))
  val inputFinishedPerCore = RegInit(VecInit((0 until 8).map(_ => false.B)))
  val outputBitsPerCore = RegInit(VecInit((0 until 8).map(_ => 0.asUInt(64.W))))
  val outputLengthCommitted = RegInit(false.B)
  val inputMemAddr = RegInit(metadataPtr.asUInt(64.W))
  val outputMemAddr = Reg(UInt(64.W))
  val outputLenAddr = Reg(UInt(64.W))
  val outputMemFlushed = RegInit(false.B)

  val inputAddressAccepted = RegInit(false.B)
  io.inputMemAddr := inputMemAddr
  var allCoresMemConsumed = cores(0).io.inputMemConsumed
  for (i <- 1 until 8) {
    allCoresMemConsumed = allCoresMemConsumed && cores(i).io.inputMemConsumed
  }
  // TODO we can probably eliminate someCoresBitsRemaining computation if we modify StreamingWrapper a bit
  var someCoresBitsRemaining = inputBitsRemainingPerCore(0) =/= 0.U
  for (i <- 1 until 8) {
    someCoresBitsRemaining = someCoresBitsRemaining || (inputBitsRemainingPerCore(i) =/= 0.U)
  }
  // the 8 cores advance through memory blocks in lockstep
  io.inputMemAddrValid := !inputAddressAccepted && (isInit || (initDone && allCoresMemConsumed &&
    someCoresBitsRemaining))
  when (io.inputMemAddrValid && io.inputMemAddrReady) {
    printf(p"input address 0x${Hexadecimal(io.inputMemAddr)} accepted for core $coreId\n")
    inputAddressAccepted := true.B
  }
  io.inputMemBlockReady := inputAddressAccepted
  for (i <- 0 until 8) {
    cores(i).io.inputMemBlock := io.inputMemBlock((i + 1) * 64 - 1, i * 64)
  }
  val inputBlockReadable = Wire(Bool())
  inputBlockReadable := inputAddressAccepted && io.inputMemBlockValid
  for (i <- 0 until 8) {
    cores(i).io.inputMemBlockValid := inputBlockReadable && initDone
  }
  for (i <- 0 until 8) {
    cores(i).io.inputBits := Mux(inputBitsRemainingPerCore(i) > 64.U, 64.U, inputBitsRemainingPerCore(i))
  }
  when (inputBlockReadable) {
    printf(p"input block 0x${Hexadecimal(io.inputMemBlock)} at address 0x${Hexadecimal(io.inputMemAddr)} accepted for core $coreId\n")
    when (isInit) {
      inputMemAddr := io.inputMemBlock(63, 0)
      outputMemAddr := io.inputMemBlock(127, 64) + 64.U
      outputLenAddr := io.inputMemBlock(127, 64)
      for (i <- 0 until 8) {
        inputBitsRemainingPerCore(i) := io.inputMemBlock(127 + 32 * (i + 1), 128 + 32 * i)
      }
      isInit := false.B
      initDone := true.B
    } .otherwise {
      for (i <- 0 until 8) {
        inputBitsRemainingPerCore(i) := Mux(inputBitsRemainingPerCore(i) > 64.U,
          inputBitsRemainingPerCore(i) - 64.U, 0.U)
      }
      inputMemAddr := inputMemAddr + 64.U
    }
    inputAddressAccepted := false.B
  }
  io.inputFinished := !someCoresBitsRemaining
  when (core.io.inputMemConsumed && inputBitsRemaining === 0.U) {
    coreInputFinished := true.B
  }
  core.io.inputFinished := coreInputFinished

  val outputAddressAccepted = RegInit(false.B)
  io.outputMemAddr := Mux(core.io.outputFinished, outputLenAddr, outputMemAddr)
  io.outputMemAddrValid := !outputAddressAccepted && (core.io.outputMemBlockValid ||
    (core.io.outputFinished && !outputLengthCommitted))
  when (io.outputMemAddrValid && io.outputMemAddrReady) {
    printf(p"output address 0x${Hexadecimal(io.outputMemAddr)} accepted for core $coreId\n")
    outputAddressAccepted := true.B
  }
  io.outputMemBlock := Mux(core.io.outputFinished, outputBits, core.io.outputMemBlock)
  io.outputMemBlockValid := outputAddressAccepted
  core.io.outputMemFlushed := outputAddressAccepted && io.outputMemBlockReady
  when (outputAddressAccepted && io.outputMemBlockReady) {
    printf(p"output block 0x${Hexadecimal(io.outputMemBlock)} at address 0x${Hexadecimal(io.outputMemAddr)} accepted for core $coreId\n")
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
      (i - inputChannelBounds(curInputChannel)) * 64, i))
  }
  // TODO may want to divide this into channel groups so that we don't have to do the random access
  // across all cores
  val cores = VecInit(_cores.map(_.io))
  for (i <- 0 until numInputChannels) {
    curInputCore(i) = RegInit(inputChannelBounds(i).asUInt(util.log2Up(Math.max(numCores, 2)).W))
  }
  for (i <- 0 until numOutputChannels) {
    curOutputCore(i) = RegInit(outputChannelBounds(i).asUInt(util.log2Up(Math.max(numCores, 2)).W))
  }

  for (i <- 0 until numInputChannels) {
    io.inputMemAddrs(i) := cores(curInputCore(i)).inputMemAddr
    io.inputMemAddrValids(i) := cores(curInputCore(i)).inputMemAddrValid
    io.inputMemBlockReadys(i) := cores(curInputCore(i)).inputMemBlockReady
    for (j <- inputChannelBounds(i) until inputChannelBounds(i + 1)) {
      cores(j).inputMemAddrReady := Mux(curInputCore(i) === j.U, io.inputMemAddrReadys(i), false.B)
      cores(j).inputMemBlock := io.inputMemBlocks(i)
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

object StreamingWrapperDriver extends App {
  chisel3.Driver.execute(args, () => new StreamingWrapper(2, Array(0L, 0L), 2, Array(0L, 0L), 100))
}