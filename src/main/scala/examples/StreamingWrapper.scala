package examples

import chisel3._
import chisel3.core.{IntParam, Reg, Bundle, Module}

class DualPortBRAM(dataWidth: Int, addrWidth: Int) extends BlackBox(Map("DATA" -> IntParam(dataWidth),
                                                                        "ADDR" -> IntParam(addrWidth))) {
  val io = IO(new Bundle {
    val a_addr = Input(UInt(addrWidth.W))
    val a_din = Input(UInt(dataWidth.W))
    val a_wr = Input(Bool())
    val a_dout = Output(UInt(dataWidth.W))
    val b_addr = Input(UInt(addrWidth.W))
    val b_din = Input(UInt(dataWidth.W))
    val b_wr = Input(Bool())
    val b_dout = Output(UInt(dataWidth.W))
  })

  // simulation model for BRAM
  // there's no guarantee about what happens on
  // collisions (sim access to same address with two memory ports)
  val mem = Mem(1 << addrWidth, UInt(dataWidth.W))

  val regAddrA = RegNext(io.a_addr)
  io.a_dout := mem.read(regAddrA)
  when (io.a_wr) {
    mem.write(io.a_addr, io.a_din)
  }

  val regAddrB = RegNext(io.b_addr)
  io.b_dout := mem.read(regAddrB)
  when (io.b_wr) {
    mem.write(io.b_addr, io.b_din)
  }
}

class PassThrough extends Module {
  val io = IO(new Bundle {
    val inputMemBlock = Input(UInt(512.W))
    val inputMemBlockValid = Input(Bool())
    val inputBits = Input(UInt(util.log2Ceil(513).W))
    val inputMemConsumed = Output(Bool())
    // continuously asserted at least one cycle after inputMemConsumed emitted for final block
    val inputFinished = Input(Bool())
    val outputMemBlock = Output(UInt(512.W))
    // must hold valid until we received flushed signal
    val outputMemBlockValid = Output(Bool())
    val outputBits = Output(UInt(util.log2Ceil(513).W))
    // hold continuously starting at some point (at least one cycle) after flush of final output
    val outputFinished = Output(Bool())
    val outputMemFlushed = Input(Bool())
  })

  val inputMemBlock = Reg(Vec(512, Bool()))
  val inputBitsRemaining = RegInit(0.asUInt(util.log2Ceil(513).W))
  val outputMemBlock = Reg(Vec(512, Bool()))
  val outputBits = RegInit(0.asUInt(util.log2Ceil(513).W))

  val inputAdvance = Wire(Bool())
  inputAdvance := outputBits =/= 512.U && inputBitsRemaining =/= 0.U
  when (io.inputMemBlockValid) {
    inputBitsRemaining := io.inputBits
    for (i <- 0 until 512) {
      inputMemBlock(i) := io.inputMemBlock(i)
    }
  } .elsewhen (inputAdvance) {
    inputBitsRemaining := inputBitsRemaining - 1.U
    for (i <- 0 until 511) {
      inputMemBlock(i) := inputMemBlock(i + 1)
    }
  }

  val nextBitValid = Wire(Bool())
  val nextBit = Wire(Bool())
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

  io.inputMemConsumed := inputBitsRemaining === 0.U && !io.inputFinished
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
  val inputMemBlock = Input(UInt(16.W))
  val inputMemBlockValid = Input(Bool())
  val inputMemBlockReady = Output(Bool())
  val outputMemAddr = Output(UInt(64.W))
  val outputMemAddrValid = Output(Bool())
  val outputMemAddrReady = Input(Bool())
  val outputMemBlock = Output(UInt(16.W))
  val outputMemBlockValid = Output(Bool())
  val outputMemBlockReady = Input(Bool())
  val inputFinished = Output(Bool())
  val outputFinished = Output(Bool())
}

// TODO current limitation: all addresses must be 512-bit aligned
class StreamingCore(metadataPtr: Long, coreId: Int) extends Module {
  val io = IO(new StreamingCoreIO)
  val core = Module(new PassThrough)

  val isInit = RegInit(true.B)
  val initDone = RegInit(false.B)
  val inputBitsRemaining = RegInit(1.asUInt(64.W)) // init nonzero so that inputFinished isn't immediately asserted
  val inputBlockCounter = RegInit(0.asUInt(5.W))
  val coreInputFinished = RegInit(false.B)
  val outputBits = RegInit(0.asUInt(64.W))
  val outputLengthCommitted = RegInit(false.B)
  val inputMemAddr = RegInit(metadataPtr.asUInt(64.W))
  val outputMemAddr = Reg(UInt(64.W))
  val outputLenAddr = Reg(UInt(64.W))
  val outputMemFlushed = RegInit(false.B)

  val inputAddressAccepted = RegInit(false.B)
  io.inputMemAddr := inputMemAddr
  // TODO with some changes to the StreamingWrapper the inputBitsRemaining === 0.U check may not be needed
  io.inputMemAddrValid := !inputAddressAccepted && (isInit || (initDone && core.io.inputMemConsumed &&
    !(inputBitsRemaining === 0.U)))
  when (io.inputMemAddrValid && io.inputMemAddrReady) {
    printf(p"input address 0x${Hexadecimal(io.inputMemAddr)} accepted for core $coreId\n")
    inputAddressAccepted := true.B
  }
  io.inputMemBlockReady := inputAddressAccepted
  core.io.inputMemBlock := io.inputMemBlock
  val inputBlockReadable = Wire(Bool())
  inputBlockReadable := inputAddressAccepted && io.inputMemBlockValid
  when (inputBlockReadable) {
    when (inputBlockCounter === 31.U) {
      inputBlockCounter := 0.U
    } .otherwise {
      inputBlockCounter := inputBlockCounter + 1.U
    }
  }
  core.io.inputMemBlockValid := inputBlockReadable && initDone
  core.io.inputBits := Mux(inputBitsRemaining > 512.U, 512.U, inputBitsRemaining(util.log2Ceil(513) - 1, 0))
  when (inputBlockReadable) {
    printf(p"input block 0x${Hexadecimal(io.inputMemBlock)} at address 0x${Hexadecimal(io.inputMemAddr)} accepted for core $coreId\n")
    when (isInit) {
      for (i <- 0 until 4) {
        when (inputBlockCounter === i.U) {
          inputMemAddr((i + 1) * 16 - 1, i * 16) := io.inputMemBlock
        }
      }
      for (i <- 0 until 4) {
        when (inputBlockCounter === (i + 4).U) {
          inputBitsRemaining((i + 1) * 16 - 1, i * 16) := io.inputMemBlock
        }
      }
      for (i <- 0 until 4) {
        when (inputBlockCounter === (i + 8).U) {
          outputMemAddr((i + 1) * 16 - 1, i * 16) := io.inputMemBlock
          outputLenAddr((i + 1) * 16 - 1, i * 16) := io.inputMemBlock
        }
      }
      when (inputBlockCounter === 31.U) {
        isInit := false.B
        initDone := true.B
        outputMemAddr := outputMemAddr + 64.U
      }
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
    curInputCore(i) = RegInit(inputChannelBounds(i).asUInt(util.log2Ceil(Math.max(numCores, 2)).W))
  }
  for (i <- 0 until numOutputChannels) {
    curOutputCore(i) = RegInit(outputChannelBounds(i).asUInt(util.log2Ceil(Math.max(numCores, 2)).W))
  }

  for (i <- 0 until numInputChannels) {
    val inputBuffer = Reg(Vec(32, UInt(16.W)))
    val inputBufferIdx = RegInit(0.asUInt(5.W))
    val inputBufferValid = RegInit(false.B)
    when (io.inputMemBlockReadys(i) && io.inputMemBlockValids(i)) {
      for (j <- 0 until 32) {
        inputBuffer(j) := io.inputMemBlocks(i)((j + 1) * 16 - 1, j * 16)
      }
      inputBufferValid := true.B
    }
    when (inputBufferValid) {
      when(inputBufferIdx === 31.U) {
        inputBufferValid := false.B
        inputBufferIdx := 0.U
      } .otherwise {
        inputBufferIdx := inputBufferIdx + 1.U
      }
    }

    io.inputMemAddrs(i) := cores(curInputCore(i)).inputMemAddr
    io.inputMemAddrValids(i) := cores(curInputCore(i)).inputMemAddrValid
    // TODO make sure it's fine for this to be asserted even after the block is read
    io.inputMemBlockReadys(i) := cores(curInputCore(i)).inputMemBlockReady
    for (j <- inputChannelBounds(i) until inputChannelBounds(i + 1)) {
      cores(j).inputMemAddrReady := Mux(curInputCore(i) === j.U, io.inputMemAddrReadys(i), false.B)
      cores(j).inputMemBlock := inputBuffer(inputBufferIdx)
      cores(j).inputMemBlockValid := Mux(curInputCore(i) === j.U, inputBufferValid, false.B)
    }

    when (cores(curInputCore(i)).inputFinished || inputBufferIdx === 31.U) {
      curInputCore(i) := Mux(curInputCore(i) === (inputChannelBounds(i + 1) - 1).U, inputChannelBounds(i).U,
        curInputCore(i) + 1.U)
    }
  }

  for (i <- 0 until numOutputChannels) {
    val outputBuffer = Reg(Vec(32, UInt(16.W)))
    val outputBufferIdx = RegInit(0.asUInt(5.W))
    when (cores(curOutputCore(i)).outputMemBlockValid) {
      when (outputBufferIdx === 31.U) {
        when (io.outputMemBlockReadys(i)) {
          outputBufferIdx := 0.U
        }
      } .otherwise {
        outputBufferIdx := outputBufferIdx + 1.U
      }
    }
    outputBuffer(outputBufferIdx) := cores(curOutputCore(i)).outputMemBlock

    io.outputMemAddrs(i) := cores(curOutputCore(i)).outputMemAddr
    io.outputMemAddrValids(i) := cores(curOutputCore(i)).outputMemAddrValid
    io.outputMemBlocks(i) := outputBuffer
    io.outputMemBlockValids(i) := outputBufferIdx === 31.U
    for (j <- outputChannelBounds(i) until outputChannelBounds(i + 1)) {
      cores(j).outputMemAddrReady := Mux(curOutputCore(i) === j.U, io.outputMemAddrReadys(i), false.B)
      // core doesn't need the ready signal here, the wrapper handles that
      cores(j).outputMemBlockReady := true.B
    }

    when (cores(curOutputCore(i)).outputFinished || (io.outputMemBlockReadys(i) && outputBufferIdx === 31.U)) {
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