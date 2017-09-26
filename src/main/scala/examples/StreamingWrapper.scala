package examples

import chisel3._
import chisel3.core.{IntParam, Reg, Bundle, Module}

class DualPortBRAM(dataWidth: Int, addrWidth: Int)  extends Module /* extends BlackBox(Map("DATA" -> IntParam(dataWidth),
                                                                        "ADDR" -> IntParam(addrWidth))) */ {
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
  val mem = SyncReadMem(1 << addrWidth, UInt(dataWidth.W))

  io.a_dout := mem.read(io.a_addr)
  when (io.a_wr) {
    mem.write(io.a_addr, io.a_din)
  }

  io.b_dout := mem.read(io.b_addr)
  when (io.b_wr) {
    mem.write(io.b_addr, io.b_din)
  }
}

class PassThrough extends Module {
  val io = IO(new Bundle {
    val inputMemBlock = Input(UInt(16.W))
    val inputMemIdx = Input(UInt(5.W))
    val inputMemBlockValid = Input(Bool())
    val inputBits = Input(UInt(util.log2Ceil(513).W))
    val inputMemConsumed = Output(Bool())
    // continuously asserted at least one cycle after inputMemConsumed emitted for final block
    val inputFinished = Input(Bool())
    val outputMemBlock = Output(UInt(16.W))
    // must hold valid until we received flushed signal
    val outputMemBlockValid = Output(Bool())
    val outputMemBlockReady = Input(Bool())
    val outputBits = Output(UInt(util.log2Ceil(513).W))
    // hold continuously starting at some point (at least one cycle) after flush of final output
    val outputFinished = Output(Bool())
    val outputMemFlushed = Input(Bool())
  })

  val inputMemBlock = Reg(Vec(16, Bool()))
  val inputPieceBitsRemaining = RegInit(0.asUInt(util.log2Ceil(17).W))
  val inputBitsRemaining = RegInit(0.asUInt(util.log2Ceil(513).W))
  val inputBlockLoaded = RegInit(false.B)
  val outputMemBlock = Reg(Vec(16, Bool()))
  val outputBits = RegInit(0.asUInt(util.log2Ceil(513).W))
  val outputPieceBits = RegInit(0.asUInt(util.log2Ceil(17).W))
  val inputBram = Module(new DualPortBRAM(16, 5))
  // inputReadAddr and outputWriteAddr must wrap back to 0 after their last value (valid address range must
  // be a power of two)
  val inputReadAddr = RegInit(0.asUInt(5.W))
  val inputPieceRead = RegInit(false.B)
  val outputBram = Module(new DualPortBRAM(16, 5))
  val outputWriteAddr = RegInit(0.asUInt(5.W))
  val outputReadAddr = RegInit(0.asUInt(5.W))

  inputBram.io.a_wr := io.inputMemBlockValid
  inputBram.io.a_addr := io.inputMemIdx
  inputBram.io.a_din := io.inputMemBlock
  when (io.inputMemBlockValid && io.inputMemIdx === 31.U) {
    inputBlockLoaded := true.B
    inputBitsRemaining := io.inputBits
  }
  when (inputBlockLoaded && inputBitsRemaining === 0.U) {
    inputBlockLoaded := false.B
  }

  inputBram.io.b_wr := false.B
  inputBram.io.b_addr := inputReadAddr
  when (inputBlockLoaded && inputPieceBitsRemaining === 0.U && inputBitsRemaining =/= 0.U) {
    when (!inputPieceRead) {
      inputPieceRead := true.B
    } .otherwise {
      inputPieceRead := false.B
      inputPieceBitsRemaining := Mux(inputBitsRemaining < 16.U, inputBitsRemaining, 16.U)
      inputReadAddr := inputReadAddr + 1.U
      for (i <- 0 until 16) {
        inputMemBlock(i) := inputBram.io.b_dout(i)
      }
    }
  }
  val inputAdvance = Wire(Bool())
  inputAdvance := outputPieceBits =/= 16.U && !(outputBits === 512.U) && inputPieceBitsRemaining =/= 0.U
  when (inputAdvance) {
    inputPieceBitsRemaining := inputPieceBitsRemaining - 1.U
    inputBitsRemaining := inputBitsRemaining - 1.U
    for (i <- 0 until 15) {
      inputMemBlock(i) := inputMemBlock(i + 1)
    }
  }

  val nextBitValid = Wire(Bool())
  val nextBit = Wire(Bool())
  nextBitValid := inputAdvance
  nextBit := inputMemBlock(0)
  when (nextBitValid) {
    outputMemBlock(15) := nextBit
    for (i <- 0 until 15) {
      // important: this means that in a final, partial output block the valid bits will be stored in the upper bits
      // of the block
      outputMemBlock(i) := outputMemBlock(i + 1)
    }
    outputPieceBits := outputPieceBits + 1.U
    outputBits := outputBits + 1.U
  }

  // TODO this will cause a write during read for partial pieces, but it should be safe since the data is unchanged
  outputBram.io.a_wr := outputPieceBits === 16.U || (io.inputFinished && inputBitsRemaining === 0.U
    && outputPieceBits > 0.U)
  outputBram.io.a_addr := outputWriteAddr
  outputBram.io.a_din := outputMemBlock.asUInt
  when (outputPieceBits === 16.U) {
    outputPieceBits := 0.U
    outputWriteAddr := outputWriteAddr + 1.U
  }

  val outputReadingStarted = RegInit(false.B)
  outputBram.io.b_wr := false.B
  outputBram.io.b_addr := outputReadAddr
  when (!outputReadingStarted &&
    (outputBits === 512.U || (io.inputFinished && inputBitsRemaining === 0.U && outputBits > 0.U))) {
    outputReadingStarted := true.B
  }
  when (io.outputMemBlockReady && outputReadingStarted && outputReadAddr =/= 31.U) {
    outputReadAddr := outputReadAddr + 1.U
  }
  io.inputMemConsumed := inputBitsRemaining === 0.U && !io.inputFinished
  io.outputMemBlockValid := outputReadingStarted &&
    (outputBits === 512.U || (io.inputFinished && inputBitsRemaining === 0.U && outputBits > 0.U))
  io.outputMemBlock := outputBram.io.b_dout
  io.outputBits := outputBits
  io.outputFinished := io.inputFinished && inputBitsRemaining === 0.U && outputBits === 0.U
  when (io.outputMemFlushed) {
    outputBits := 0.U
    outputPieceBits := 0.U
    outputReadingStarted := false.B
    outputWriteAddr := 0.U
    outputReadAddr := 0.U
  }
}

class StreamingCoreIO extends Bundle {
  val inputMemAddr = Output(UInt(64.W))
  val inputMemAddrValid = Output(Bool())
  val inputMemAddrReady = Input(Bool())
  val inputMemBlock = Input(UInt(16.W))
  val inputMemIdx = Input(UInt(5.W))
  val inputMemBlockValid = Input(Bool())
  val inputMemBlockReady = Output(Bool())
  val outputMemAddr = Output(UInt(64.W))
  val outputMemAddrValid = Output(Bool())
  val outputMemAddrReady = Input(Bool())
  val outputMemBlock = Output(UInt(16.W))
  val outputMemIdx = Output(UInt(5.W))
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
  val coreInputFinished = RegInit(false.B)
  val outputBits = RegInit(0.asUInt(64.W))
  val outputBlockCounter = RegInit(0.asUInt(5.W))
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
  core.io.inputMemIdx := io.inputMemIdx
  val inputBlockReadable = Wire(Bool())
  inputBlockReadable := inputAddressAccepted && io.inputMemBlockValid
  core.io.inputMemBlockValid := inputBlockReadable && initDone
  core.io.inputBits := Mux(inputBitsRemaining > 512.U, 512.U, inputBitsRemaining(util.log2Ceil(513) - 1, 0))
  when (inputBlockReadable) {
    when (isInit) {
      for (i <- 0 until 4) {
        when (io.inputMemIdx === i.U) {
          val result =
            if (i == 0) {
              inputMemAddr(63, (i + 1) * 16)##io.inputMemBlock
            } else if (i == 3) {
              io.inputMemBlock##inputMemAddr(i * 16 - 1, 0)
            } else {
              inputMemAddr(63, (i + 1) * 16)##io.inputMemBlock##inputMemAddr(i * 16 - 1, 0)
            }
          inputMemAddr := result
        }
      }
      for (i <- 0 until 4) {
        when (io.inputMemIdx === (i + 4).U) {
          val result =
            if (i == 0) {
              inputBitsRemaining(63, (i + 1) * 16)##io.inputMemBlock
            } else if (i == 3) {
              io.inputMemBlock##inputBitsRemaining(i * 16 - 1, 0)
            } else {
              inputBitsRemaining(63, (i + 1) * 16)##io.inputMemBlock##inputBitsRemaining(i * 16 - 1, 0)
            }
          inputBitsRemaining := result
        }
      }
      for (i <- 0 until 4) {
        when (io.inputMemIdx === (i + 8).U) {
          val result =
            if (i == 0) {
              outputMemAddr(63, (i + 1) * 16)##io.inputMemBlock
            } else if (i == 3) {
              io.inputMemBlock##outputMemAddr(i * 16 - 1, 0)
            } else {
              outputMemAddr(63, (i + 1) * 16)##io.inputMemBlock##outputMemAddr(i * 16 - 1, 0)
            }
          outputMemAddr := result
          outputLenAddr := result
        }
      }
      when (io.inputMemIdx === 31.U) {
        isInit := false.B
        initDone := true.B
        outputMemAddr := outputMemAddr + 64.U
      }
    } .otherwise {
      when (io.inputMemIdx === 31.U) {
        inputBitsRemaining := Mux(inputBitsRemaining > 512.U, inputBitsRemaining - 512.U, 0.U)
        inputMemAddr := inputMemAddr + 64.U
      }
    }
    when (io.inputMemIdx === 31.U) {
      inputAddressAccepted := false.B
    }
  }
  io.inputFinished := inputBitsRemaining === 0.U
  when (core.io.inputMemConsumed && inputBitsRemaining === 0.U) {
    coreInputFinished := true.B
  }
  core.io.inputFinished := coreInputFinished

  val outputAddressAccepted = RegInit(false.B)
  val outputAddressAcceptedNext = RegInit(false.B)
  when (outputAddressAccepted) {
    outputAddressAcceptedNext := true.B
  }
  io.outputMemAddr := Mux(core.io.outputFinished, outputLenAddr, outputMemAddr)
  io.outputMemAddrValid := !outputAddressAccepted && (core.io.outputMemBlockValid ||
    (core.io.outputFinished && !outputLengthCommitted))
  when (io.outputMemAddrValid && io.outputMemAddrReady) {
    printf(p"output address 0x${Hexadecimal(io.outputMemAddr)} accepted for core $coreId\n")
    outputAddressAccepted := true.B
  }
  core.io.outputMemBlockReady := outputAddressAccepted
  when (outputAddressAcceptedNext) {
    when (outputBlockCounter === 31.U) {
      when (io.outputMemBlockReady) {
        outputBlockCounter := 0.U
      }
    } .otherwise {
      outputBlockCounter := outputBlockCounter + 1.U
    }
  }
  val outputBitsBlock = Wire(UInt(16.W))
  outputBitsBlock := Mux(outputBlockCounter === 0.U, outputBits(15, 0), Mux(outputBlockCounter === 1.U,
    outputBits(31, 16), Mux(outputBlockCounter === 2.U, outputBits(47, 32),
      Mux(outputBlockCounter === 3.U, outputBits(63, 48), 0.U))))
  io.outputMemBlock := Mux(core.io.outputFinished, outputBitsBlock, core.io.outputMemBlock)
  io.outputMemIdx := outputBlockCounter

  io.outputMemBlockValid := outputAddressAcceptedNext
  core.io.outputMemFlushed := outputAddressAcceptedNext && io.outputMemBlockReady
  when (outputAddressAcceptedNext && io.outputMemBlockReady && outputBlockCounter === 31.U) {
    outputAddressAccepted := false.B
    outputAddressAcceptedNext := false.B
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
      printf(p"inputBuffer: 0x${Hexadecimal(io.inputMemBlocks(i))} for core ${curInputCore(i)}, channel $i\n")
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
      cores(j).inputMemIdx := inputBufferIdx
      cores(j).inputMemBlockValid := Mux(curInputCore(i) === j.U, inputBufferValid, false.B)
    }

    when (cores(curInputCore(i)).inputFinished || inputBufferIdx === 31.U) {
      curInputCore(i) := Mux(curInputCore(i) === (inputChannelBounds(i + 1) - 1).U, inputChannelBounds(i).U,
        curInputCore(i) + 1.U)
    }
  }

  for (i <- 0 until numOutputChannels) {
    val outputBuffer = Reg(Vec(32, UInt(16.W)))
    val outputBufferValid = RegInit(false.B)
    when (cores(curOutputCore(i)).outputMemBlockValid && cores(curOutputCore(i)).outputMemIdx === 31.U) {
      outputBufferValid := true.B
    }
    // TODO this when wrapper may not be needed depending on the behavior of the inner core
    when (!outputBufferValid) {
      outputBuffer(cores(curOutputCore(i)).outputMemIdx) := cores(curOutputCore(i)).outputMemBlock
    }

    io.outputMemAddrs(i) := cores(curOutputCore(i)).outputMemAddr
    io.outputMemAddrValids(i) := cores(curOutputCore(i)).outputMemAddrValid
    io.outputMemBlocks(i) := outputBuffer.asUInt
    io.outputMemBlockValids(i) := outputBufferValid
    for (j <- outputChannelBounds(i) until outputChannelBounds(i + 1)) {
      cores(j).outputMemAddrReady := Mux(curOutputCore(i) === j.U, io.outputMemAddrReadys(i), false.B)
      cores(j).outputMemBlockReady := Mux(curOutputCore(i) === j.U,
        io.outputMemBlockReadys(i) && outputBufferValid, false.B)
    }

    when (io.outputMemBlockReadys(i) && io.outputMemBlockValids(i)) {
      printf(p"outputBuffer: 0x${Hexadecimal(io.outputMemBlocks(i))} for core ${curOutputCore(i)}, channel $i\n")
    }
    when (cores(curOutputCore(i)).outputFinished || (io.outputMemBlockReadys(i) && outputBufferValid)) {
      curOutputCore(i) := Mux(curOutputCore(i) === (outputChannelBounds(i + 1) - 1).U, outputChannelBounds(i).U,
        curOutputCore(i) + 1.U)
      outputBufferValid := false.B
    }
  }
  var cumFinished = cores(0).outputFinished
  for (i <- 1 until numCores) {
    cumFinished = cumFinished && cores(i).outputFinished
  }
  io.finished := cumFinished
}

object StreamingWrapperDriver extends App {
  chisel3.Driver.execute(args, () => new StreamingWrapper(2, Array(0L, 0L), 2, Array(0L, 0L), 50))
}