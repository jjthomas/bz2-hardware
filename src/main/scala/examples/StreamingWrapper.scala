package examples

import chisel3._
import chisel3.core.{Reg, Bundle, Module}

class InnerCore(bramWidth: Int, bramNumAddrs: Int, wordBits: Int, puFactory: (Int) => ProcessingUnit,
                coreId: Int) extends Module {
  val bramAddrBits = util.log2Ceil(bramNumAddrs)
  val bramLineSize = bramWidth * bramNumAddrs
  val io = IO(new Bundle {
    val inputMemBlock = Input(UInt(bramWidth.W))
    val inputMemIdx = Input(UInt(bramAddrBits.W))
    val inputMemBlockValid = Input(Bool())
    val inputBits = Input(UInt(util.log2Ceil(bramLineSize + 1).W))
    val inputMemConsumed = Output(Bool())
    // continuously asserted at least one cycle after inputMemConsumed emitted for final block
    val inputFinished = Input(Bool())
    val outputMemBlock = Output(UInt(bramWidth.W))
    // must hold valid until we received flushed signal
    val outputMemBlockValid = Output(Bool())
    val outputMemBlockReady = Input(Bool())
    val outputBits = Output(UInt(util.log2Ceil(bramLineSize + 1).W))
    // hold continuously starting at some point (at least one cycle) after flush of final output
    val outputFinished = Output(Bool())
    val outputMemFlushed = Input(Bool())
  })
  assert(wordBits <= bramWidth)
  assert(bramWidth % wordBits == 0)
  val inner = Module(puFactory(coreId))

  // TODO this does not need to be coupled with the bramWidth (same with outputMemBlock)
  val inputMemBlock = Reg(Vec(bramWidth, Bool()))
  val inputPieceBitsRemaining = RegInit(0.asUInt(util.log2Ceil(bramWidth + 1).W))
  val inputBitsRemaining = RegInit(0.asUInt(util.log2Ceil(bramLineSize + 1).W))
  val inputBlockLoaded = RegInit(false.B)
  val outputMemBlock = Reg(Vec(bramWidth, Bool()))
  val outputBits = RegInit(0.asUInt(util.log2Ceil(bramLineSize + 1).W))
  val outputPieceBits = RegInit(0.asUInt(util.log2Ceil(bramWidth + 1).W))
  val inputBram = Module(new DualPortBRAM(bramWidth, bramAddrBits))
  // inputReadAddr and outputWriteAddr must wrap back to 0 after their last value (valid address range must
  // be a power of two)
  val inputReadAddr = RegInit(0.asUInt(bramAddrBits.W))
  val outputBram = Module(new DualPortBRAM(bramWidth, bramAddrBits))
  val outputWriteAddr = RegInit(0.asUInt(bramAddrBits.W))
  val outputReadAddr = RegInit(0.asUInt(bramAddrBits.W))

  inputBram.io.a_wr := io.inputMemBlockValid
  inputBram.io.a_addr := io.inputMemIdx
  inputBram.io.a_din := io.inputMemBlock
  when (io.inputMemBlockValid && io.inputMemIdx === 1.U) {
    inputBlockLoaded := true.B
    inputPieceBitsRemaining := wordBits.U
    inputBitsRemaining := io.inputBits
  }
  when (inputBlockLoaded && inputBitsRemaining === 0.U) {
    inputBlockLoaded := false.B
    inputReadAddr := 0.U
  }

  inputBram.io.b_wr := false.B
  inputBram.io.b_addr := inputReadAddr
  val pieceCompleteCondition =
    if (bramWidth == wordBits) {
      inputPieceBitsRemaining === 0.U
    } else {
      inputReadAddr === 0.U ||
        (inner.io.inputReady && ((inputPieceBitsRemaining - wordBits.U) === 0.U)) // inner.io.inputValid is implied
    }
  when (inputBlockLoaded && pieceCompleteCondition && !(inputBitsRemaining === 0.U)) {
    val newInputBitsRemaining = Mux(inputReadAddr === 0.U, inputBitsRemaining,
        Mux(inputBitsRemaining < bramWidth.U, 0.U, inputBitsRemaining - bramWidth.U))
    inputBitsRemaining := newInputBitsRemaining
    inputPieceBitsRemaining := Mux(newInputBitsRemaining < bramWidth.U, newInputBitsRemaining, bramWidth.U)
    inputReadAddr := Mux(inputReadAddr === (bramNumAddrs - 1).U, inputReadAddr, inputReadAddr + 1.U)
    for (i <- 0 until bramWidth) {
      inputMemBlock(i) := inputBram.io.b_dout(i)
    }
  }
  val pieceProgressCondition =
    if (bramWidth == wordBits) {
      !(inputPieceBitsRemaining === 0.U)
    } else {
      inner.io.inputValid && !((inputPieceBitsRemaining - wordBits.U) === 0.U)
    }
  when (inner.io.inputReady && pieceProgressCondition) {
    inputPieceBitsRemaining := inputPieceBitsRemaining - wordBits.U
    for (i <- 0 until (bramWidth - wordBits)) {
      inputMemBlock(i) := inputMemBlock(i + wordBits)
    }
  }

  val nextWord =
    if (wordBits == 0) {
      inputMemBlock(0)
    } else {
      inputMemBlock.asUInt()(wordBits - 1, 0)
    }
  val validCondition =
    if (bramWidth == wordBits) {
      !(inputPieceBitsRemaining === 0.U)
    } else {
      !(inputBitsRemaining === 0.U) && !(inputReadAddr === 0.U)
    }
  inner.io.inputValid := validCondition
  inner.io.inputFinished := io.inputFinished && inputBitsRemaining === 0.U
  inner.io.inputWord := nextWord
  inner.io.outputReady := !(outputBits === bramLineSize.U)
  // outputValid must be asserted on the same cycle as inputValid if that input triggered the output
  when ((inner.io.outputValid && inner.io.outputReady) || (inner.io.outputFinished && outputPieceBits > 0.U &&
    outputPieceBits < bramWidth.U)) {
    for (i <- 0 until wordBits) {
      outputMemBlock(bramWidth - 1 - i) := inner.io.outputWord(wordBits - 1 - i)
    }
    for (i <- 0 until (bramWidth - wordBits)) {
      outputMemBlock(i) := outputMemBlock(i + wordBits)
    }
    outputPieceBits := Mux(outputPieceBits === bramWidth.U, wordBits.U, outputPieceBits + wordBits.U)
    outputWriteAddr := Mux(outputPieceBits === bramWidth.U, outputWriteAddr + 1.U, outputWriteAddr)
    when (inner.io.outputValid) {
      outputBits := outputBits + wordBits.U
    }
  }

  outputBram.io.a_wr := outputPieceBits === bramWidth.U
  outputBram.io.a_addr := outputWriteAddr
  outputBram.io.a_din := outputMemBlock.asUInt

  val outputReadingStartedPrev = RegInit(false.B) // needed to make sure we don't start reading BRAM address 0
  // on the same cycle it is written in the case of a single-word final output block
  val outputReadingStarted = RegInit(false.B)
  outputBram.io.b_wr := false.B
  outputBram.io.b_addr := outputReadAddr
  when (!outputReadingStartedPrev &&
    (outputBits === bramLineSize.U || (inner.io.outputFinished && outputBits > 0.U &&
      outputPieceBits === bramWidth.U))) {
    outputReadingStartedPrev := true.B
  }
  when (outputReadingStartedPrev && !outputReadingStarted) {
    outputReadingStarted := true.B
  }
  when (io.outputMemBlockReady && outputReadingStarted && outputReadAddr =/= (bramNumAddrs - 1).U) {
    outputReadAddr := outputReadAddr + 1.U
    when (outputReadAddr === 0.U) {
      outputBits := 0.U // allow new output bits to be written into the output BRAM right away
    }
  }
  io.inputMemConsumed := inputBitsRemaining === 0.U && !io.inputFinished
  io.outputMemBlockValid := outputReadingStarted
  io.outputMemBlock := outputBram.io.b_dout
  io.outputBits := outputBits
  io.outputFinished := inner.io.outputFinished && outputBits === 0.U && outputReadAddr === 0.U // last term needed
  // so that this signal is only asserted after outputMemFlushed signaled for last block
  when (io.outputMemFlushed) {
    outputReadingStartedPrev := false.B
    outputReadingStarted := false.B
    outputReadAddr := 0.U
  }
}

class StreamingCoreIO(bramWidth: Int, bramNumAddrs: Int) extends Bundle {
  val bramAddrBits = util.log2Ceil(bramNumAddrs)

  val inputMemAddr = Output(UInt(32.W))
  val inputMemAddrConsumed = Input(Bool())
  val inputMemBlock = Input(UInt(bramWidth.W))
  val inputMemIdx = Input(UInt(bramAddrBits.W))
  val inputMemBlockValid = Input(Bool())
  val inputMemBlockReady = Output(Bool())
  val outputMemAddr = Output(UInt(32.W))
  val outputMemAddrValid = Output(Bool())
  val outputMemAddrReady = Input(Bool())
  val outputMemBlock = Output(UInt(bramWidth.W))
  val outputMemIdx = Output(UInt(bramAddrBits.W))
  val outputMemBlockValid = Output(Bool())
  val outputMemBlockReady = Input(Bool())
  val inputAddrsIgnore = Output(Bool())
  val inputBlocksFinished = Output(Bool())
  val outputFinished = Output(Bool())

  override def cloneType(): this.type = new StreamingCoreIO(bramWidth, bramNumAddrs).asInstanceOf[this.type]
}

// TODO current limitation: all addresses must be 512-bit aligned
class StreamingCore(metadataPtr: Long, bramWidth: Int, bramNumAddrs: Int, wordSize: Int,
                    puFactory: (Int) => ProcessingUnit, coreId: Int)
  extends Module {
  val bramAddrBits = util.log2Ceil(bramNumAddrs)
  val bramLineSize = bramWidth * bramNumAddrs
  val bytesInLine = bramLineSize / 8
  val io = IO(new StreamingCoreIO(bramWidth, bramNumAddrs))
  val core = Module(new InnerCore(bramWidth, bramNumAddrs, wordSize, puFactory, coreId))

  val isInit = RegInit(true.B)
  val initDone = RegInit(false.B)
  val inputBitsRemaining = RegInit(1.asUInt(32.W)) // init nonzero so that inputFinished isn't immediately asserted
  val coreInputFinished = RegInit(false.B)
  val outputBits = RegInit(0.asUInt(32.W))
  val outputBlockCounter = RegInit(0.asUInt(bramAddrBits.W))
  val outputLengthCommitted = RegInit(false.B)
  val inputMemAddr = RegInit(metadataPtr.asUInt(32.W))
  val inputMemBound = Reg(UInt(32.W))
  val outputMemAddr = Reg(UInt(32.W))
  val outputLenAddr = Reg(UInt(32.W))
  val outputMemFlushed = RegInit(false.B)

  io.inputMemAddr := inputMemAddr
  when (io.inputMemAddrConsumed && !io.inputAddrsIgnore) {
    printf(p"input address 0x${Hexadecimal(io.inputMemAddr)} accepted for core $coreId\n")
  }
  val initAddressAcked = RegInit(false.B)
  when (io.inputMemAddrConsumed) {
    initAddressAcked := true.B
  }
  when (io.inputMemAddrConsumed && initDone) {
    inputMemAddr := inputMemAddr + bytesInLine.U
  }

  io.inputAddrsIgnore := (isInit && initAddressAcked) || (initDone && inputMemAddr === inputMemBound)
  // TODO with some changes to the StreamingWrapper the inputBitsRemaining === 0.U check may not be needed
  io.inputMemBlockReady := isInit || (initDone && core.io.inputMemConsumed && !(inputBitsRemaining === 0.U))
  core.io.inputMemBlock := io.inputMemBlock
  core.io.inputMemIdx := io.inputMemIdx
  core.io.inputMemBlockValid := io.inputMemBlockValid && initDone
  core.io.inputBits := Mux(inputBitsRemaining > bramLineSize.U, bramLineSize.U, inputBitsRemaining)
  when (io.inputMemBlockValid) {
    when (isInit) {
      for (i <- 0 until bramNumAddrs) {
        val startBit = i * bramWidth
        val endBit = (i + 1) * bramWidth - 1
        if (startBit <= 0 && endBit >= 31) {
          when (io.inputMemIdx === i.U) {
            inputMemAddr := io.inputMemBlock(31, 0)
          }
        } else if (startBit >= 0 && endBit <= 31) {
          when (io.inputMemIdx === i.U) {
            val startBitOffset = startBit
            val endBitOffset = endBit
            if (startBitOffset == 0) {
              inputMemAddr := inputMemAddr(31, endBitOffset + 1)##io.inputMemBlock
            } else if (endBitOffset == 31) {
              inputMemAddr := io.inputMemBlock##inputMemAddr(startBitOffset - 1, 0)
            } else {
              inputMemAddr := inputMemAddr(31, endBitOffset + 1)##io.inputMemBlock##inputMemAddr(startBitOffset - 1, 0)
            }
          }
        }
        if (startBit <= 64 && endBit >= 95) {
          when (io.inputMemIdx === i.U) {
            val result = io.inputMemBlock(95 - startBit, 64 - startBit)
            inputBitsRemaining := result
            if (startBit <= 0) {
              inputMemBound := io.inputMemBlock(31, 0) +
                result(31, util.log2Ceil(bramLineSize))##0.asUInt(util.log2Ceil(bytesInLine).W) +
                Mux(result(util.log2Ceil(bramLineSize) - 1, 0) =/= 0.U, bytesInLine.U, 0.U)
            } else {
              inputMemBound := inputMemAddr +
                result(31, util.log2Ceil(bramLineSize))##0.asUInt(util.log2Ceil(bytesInLine).W) +
                Mux(result(util.log2Ceil(bramLineSize) - 1, 0) =/= 0.U, bytesInLine.U, 0.U)
            }
          }
        } else if (startBit >= 64 && endBit <= 95) {
          when (io.inputMemIdx === i.U) {
            val startBitOffset = startBit - 64
            val endBitOffset = endBit - 64
            val result = if (startBitOffset == 0) {
              inputBitsRemaining(31, endBitOffset + 1)##io.inputMemBlock
            } else if (endBitOffset == 31) {
              io.inputMemBlock##inputBitsRemaining(startBitOffset - 1, 0)
            } else {
              inputBitsRemaining(31, endBitOffset + 1)##io.inputMemBlock##inputBitsRemaining(startBitOffset - 1, 0)
            }
            inputBitsRemaining := result
            inputMemBound := inputMemAddr +
              result(31, util.log2Ceil(bramLineSize))##0.asUInt(util.log2Ceil(bytesInLine).W) +
              Mux(result(util.log2Ceil(bramLineSize) - 1, 0) =/= 0.U, bytesInLine.U, 0.U)
          }
        }
        if (startBit <= 128 && endBit >= 159) {
          when (io.inputMemIdx === i.U) {
            outputMemAddr := io.inputMemBlock(159 - startBit, 128 - startBit)
            outputLenAddr := io.inputMemBlock(159 - startBit, 128 - startBit)
          }
        } else if (startBit >= 128 && endBit <= 159) {
          when (io.inputMemIdx === i.U) {
            val startBitOffset = startBit - 128
            val endBitOffset = endBit - 128
            val result = if (startBitOffset == 0) {
              outputMemAddr(31, endBitOffset + 1)##io.inputMemBlock
            } else if (endBitOffset == 31) {
              io.inputMemBlock##outputMemAddr(startBitOffset - 1, 0)
            } else {
              outputMemAddr(31, endBitOffset + 1)##io.inputMemBlock##outputMemAddr(startBitOffset - 1, 0)
            }
            outputMemAddr := result
            outputLenAddr := result
          }
        }
      }
      when (io.inputMemIdx === (bramNumAddrs - 1).U) {
        isInit := false.B
        initDone := true.B
        outputMemAddr := outputMemAddr + bytesInLine.U
      }
    } .otherwise {
      when (io.inputMemIdx === (bramNumAddrs - 1).U) {
        inputBitsRemaining := Mux(inputBitsRemaining > bramLineSize.U, inputBitsRemaining - bramLineSize.U, 0.U)
      }
    }
  }
  io.inputBlocksFinished := initDone && inputBitsRemaining === 0.U
  core.io.inputFinished := initDone && inputBitsRemaining === 0.U

  val outputAddressAccepted = RegInit(false.B)
  val outputAddressAcceptedNext = RegInit(false.B) // because it takes a cycle after core.io.outputMemBlockReady
  // signalled for output to actually arrive
  when (outputAddressAccepted) {
    outputAddressAcceptedNext := true.B
  }
  io.outputMemAddr := Mux(core.io.outputFinished, outputLenAddr, outputMemAddr)
  io.outputMemAddrValid := !outputAddressAccepted && (core.io.outputMemBlockValid ||
    (core.io.outputFinished && !outputLengthCommitted))
  when (io.outputMemAddrValid && io.outputMemAddrReady) {
    printf(p"output address 0x${Hexadecimal(io.outputMemAddr)} accepted for core $coreId\n")
    outputAddressAccepted := true.B
    when (!core.io.outputFinished) {
      outputBits := outputBits + core.io.outputBits
    }
  }
  core.io.outputMemBlockReady := outputAddressAccepted
  when (outputAddressAcceptedNext) {
    when (outputBlockCounter === (bramNumAddrs - 1).U) {
      when (io.outputMemBlockReady) {
        outputBlockCounter := 0.U
      }
    } .otherwise {
      outputBlockCounter := outputBlockCounter + 1.U
    }
  }
  val outputBitsBlock = Wire(UInt(bramWidth.W))
  var tmp = Mux(outputBlockCounter === 0.U, outputBits(Math.min(31, bramWidth), 0), 0.U)
  for (i <- 1 until bramNumAddrs) {
    if (bramWidth * i <= 31) {
      tmp = Mux(outputBlockCounter === i.U, outputBits((i + 1) * bramWidth - 1, i * bramWidth), tmp)
    }
  }
  outputBitsBlock := tmp
  io.outputMemBlock := Mux(core.io.outputFinished, outputBitsBlock, core.io.outputMemBlock)
  io.outputMemIdx := outputBlockCounter

  io.outputMemBlockValid := outputAddressAcceptedNext
  core.io.outputMemFlushed := outputAddressAcceptedNext && io.outputMemBlockReady
  when (outputAddressAcceptedNext && io.outputMemBlockReady) {
    outputAddressAccepted := false.B
    outputAddressAcceptedNext := false.B
    // TODO make sure outputFinished can't be set until a cycle after outputMemFlushed is asserted
    when (core.io.outputFinished) {
      outputLengthCommitted := true.B
    } .otherwise {
      outputMemAddr := outputMemAddr + bytesInLine.U
    }
  }
  io.outputFinished := outputLengthCommitted
}

class StreamingWrapper(val numInputChannels: Int, val inputChannelStartAddrs: Array[Long], val numOutputChannels: Int,
                       val outputChannelStartAddrs: Array[Long], val numCores: Int, inputGroupSize: Int,
                       outputGroupSize: Int, bramWidth: Int, bramNumAddrs: Int, wordSize: Int,
                       val puFactory: (Int) => ProcessingUnit) extends Module {
  val io = IO(new Bundle {
    val inputMemAddrs = Output(Vec(numInputChannels, UInt(64.W)))
    val inputMemAddrValids = Output(Vec(numInputChannels, Bool()))
    val inputMemAddrLens = Output(Vec(numInputChannels, UInt(8.W)))
    val inputMemAddrReadys = Input(Vec(numInputChannels, Bool()))
    val inputMemBlocks = Input(Vec(numInputChannels, UInt(512.W)))
    val inputMemBlockValids = Input(Vec(numInputChannels, Bool()))
    val inputMemBlockReadys = Output(Vec(numInputChannels, Bool()))
    val outputMemAddrs = Output(Vec(numOutputChannels, UInt(64.W)))
    val outputMemAddrValids = Output(Vec(numOutputChannels, Bool()))
    val outputMemAddrLens = Output(Vec(numOutputChannels, UInt(8.W)))
    val outputMemAddrIds = Output(Vec(numOutputChannels, UInt(16.W)))
    val outputMemAddrReadys = Input(Vec(numOutputChannels, Bool()))
    val outputMemBlocks = Output(Vec(numOutputChannels, UInt(512.W)))
    val outputMemBlockValids = Output(Vec(numOutputChannels, Bool()))
    val outputMemBlockLasts = Output(Vec(numOutputChannels, Bool()))
    val outputMemBlockReadys = Input(Vec(numOutputChannels, Bool()))
    val finished = Output(Bool())
  })
  assert(numCores % numInputChannels == 0)
  assert((numCores / numInputChannels) % inputGroupSize == 0)
  assert(numCores >= 2 * inputGroupSize)
  assert(numCores % numOutputChannels == 0)
  assert((numCores / numOutputChannels) % outputGroupSize == 0)
  assert(numCores >= 2 * outputGroupSize)
  assert(util.isPow2(bramWidth))
  assert(util.isPow2(bramNumAddrs))
  assert(bramWidth * bramNumAddrs >= 512)
  val bramAddrBits = util.log2Ceil(bramNumAddrs)
  val bramLineSize = bramWidth * bramNumAddrs
  val bytesInLine = bramLineSize / 8
  val bramNumNativeLines = bramLineSize / 512
  val bramAddrsPerNativeLine = 512 / bramWidth

  val _cores = new Array[StreamingCore](numCores)
  val curInputAddrCore = new Array[UInt](numInputChannels)
  val curInputBlockCore = new Array[UInt](numInputChannels)
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
      (i - inputChannelBounds(curInputChannel)) * bytesInLine, bramWidth, bramNumAddrs, wordSize, puFactory, i))
  }

  val cores = VecInit(_cores.map(_.io))

  for (i <- 0 until numInputChannels) {
    curInputAddrCore(i) = RegInit((inputChannelBounds(i) / inputGroupSize)
      .asUInt(util.log2Ceil(numCores / inputGroupSize).W))
    curInputBlockCore(i) = RegInit((inputChannelBounds(i) / inputGroupSize)
      .asUInt(util.log2Ceil(numCores / inputGroupSize).W))
  }
  for (i <- 0 until numOutputChannels) {
    curOutputCore(i) = RegInit((outputChannelBounds(i) / outputGroupSize)
      .asUInt(util.log2Ceil(numCores/ outputGroupSize).W))
  }

  var inputTreeLevel = 0
  val selInputMemAddr = new Array[Vec[UInt]](numInputChannels)
  val selInputMemBlockReady = new Array[Vec[Bool]](numInputChannels)
  val selInputAddrsIgnore = new Array[Vec[Bool]](numInputChannels)
  val selInputBlocksFinished = new Array[Vec[Bool]](numInputChannels)
  for (chan <- 0 until numInputChannels) {
    var curTreeLevel = new Array[Array[(UInt, Bool, Bool, Bool)]](Math.pow(2, util.log2Ceil(numCores)).toInt
      / inputGroupSize)
    for (i <- 0 until curTreeLevel.length) {
      val coreIndex = i * inputGroupSize
      if (coreIndex >= inputChannelBounds(chan) && coreIndex < inputChannelBounds(chan + 1)) {
        val curGroup = new Array[(UInt, Bool, Bool, Bool)](inputGroupSize)
        for (j <- coreIndex until coreIndex + inputGroupSize) {
          curGroup(j - coreIndex) = (cores(j).inputMemAddr, cores(j).inputMemBlockReady,
            cores(j).inputAddrsIgnore, cores(j).inputBlocksFinished)
        }
        curTreeLevel(i) = curGroup
      } else {
        curTreeLevel(i) = null
      }
    }
    inputTreeLevel = 0
    while (curTreeLevel.length > 1) {
      val newTreeLevel = new Array[Array[(UInt, Bool, Bool, Bool)]](curTreeLevel.length / 2)
      for (i <- 0 until curTreeLevel.length by 2) {
        if (curTreeLevel(i) == null && curTreeLevel(i + 1) == null) {
          newTreeLevel(i / 2) = null
        } else {
          newTreeLevel(i / 2) = new Array[(UInt, Bool, Bool, Bool)](inputGroupSize)
          for (j <- 0 until inputGroupSize) {
            // TODO make use of registers configurable (e.g. every other tree level, every 4, etc.)
            val curInputMemAddr = Reg(UInt(32.W))
            val curInputMemBlockReady = Reg(Bool())
            val curInputAddrsIgnore = Reg(Bool())
            val curInputBlocksFinished = Reg(Bool())
            if (curTreeLevel(i) == null) {
              curInputMemAddr := curTreeLevel(i + 1)(j)._1
              curInputMemBlockReady := curTreeLevel(i + 1)(j)._2
              curInputAddrsIgnore := curTreeLevel(i + 1)(j)._3
              curInputBlocksFinished := curTreeLevel(i + 1)(j)._4
            } else if (curTreeLevel(i + 1) == null) {
              curInputMemAddr := curTreeLevel(i)(j)._1
              curInputMemBlockReady := curTreeLevel(i)(j)._2
              curInputAddrsIgnore := curTreeLevel(i)(j)._3
              curInputBlocksFinished := curTreeLevel(i)(j)._4
            } else {
              curInputMemAddr := Mux(curInputAddrCore(chan)(inputTreeLevel, inputTreeLevel) === 1.U,
                curTreeLevel(i + 1)(j)._1, curTreeLevel(i)(j)._1)
              curInputMemBlockReady := Mux(curInputBlockCore(chan)(inputTreeLevel, inputTreeLevel) === 1.U,
                curTreeLevel(i + 1)(j)._2, curTreeLevel(i)(j)._2)
              curInputAddrsIgnore := Mux(curInputAddrCore(chan)(inputTreeLevel, inputTreeLevel) === 1.U,
                curTreeLevel(i + 1)(j)._3, curTreeLevel(i)(j)._3)
              curInputBlocksFinished := Mux(curInputBlockCore(chan)(inputTreeLevel, inputTreeLevel) === 1.U,
                curTreeLevel(i + 1)(j)._4, curTreeLevel(i)(j)._4)
            }
            newTreeLevel(i / 2)(j) = (curInputMemAddr, curInputMemBlockReady, curInputAddrsIgnore,
              curInputBlocksFinished)
          }
        }
      }
      curTreeLevel = newTreeLevel
      inputTreeLevel += 1
    }
    selInputMemAddr(chan) = VecInit(curTreeLevel(0).map(_._1))
    selInputMemBlockReady(chan) = VecInit(curTreeLevel(0).map(_._2))
    selInputAddrsIgnore(chan) = VecInit(curTreeLevel(0).map(_._3))
    selInputBlocksFinished(chan) = VecInit(curTreeLevel(0).map(_._4))
  }

  // TODO massive code duplication here
  var outputTreeLevel = 0
  val selOutputMemAddr = new Array[Vec[UInt]](numOutputChannels)
  val selOutputMemAddrValid = new Array[Vec[Bool]](numOutputChannels)
  val selOutputMemBlockValid = new Array[Vec[Bool]](numOutputChannels)
  val selOutputMemBlock = new Array[Vec[UInt]](numOutputChannels)
  val selOutputMemIdx = new Array[Vec[UInt]](numOutputChannels)
  val selOutputFinished = new Array[Vec[Bool]](numOutputChannels)
  for (chan <- 0 until numOutputChannels) {
    var curTreeLevel = new Array[Array[(UInt, Bool, Bool, UInt, UInt, Bool)]](Math.pow(2, util.log2Ceil(numCores)).toInt
      / outputGroupSize)
    for (i <- 0 until curTreeLevel.length) {
      val coreIndex = i * outputGroupSize
      if (coreIndex >= outputChannelBounds(chan) && coreIndex < outputChannelBounds(chan + 1)) {
        val curGroup = new Array[(UInt, Bool, Bool, UInt, UInt, Bool)](outputGroupSize)
        for (j <- coreIndex until coreIndex + outputGroupSize) {
          curGroup(j - coreIndex) = (cores(j).outputMemAddr, cores(j).outputMemAddrValid,
            cores(j).outputMemBlockValid, cores(j).outputMemBlock, cores(j).outputMemIdx,
            cores(j).outputFinished)
        }
        curTreeLevel(i) = curGroup
      } else {
        curTreeLevel(i) = null
      }
    }
    outputTreeLevel = 0
    while (curTreeLevel.length > 1) {
      val newTreeLevel = new Array[Array[(UInt, Bool, Bool, UInt, UInt, Bool)]](curTreeLevel.length / 2)
      for (i <- 0 until curTreeLevel.length by 2) {
        if (curTreeLevel(i) == null && curTreeLevel(i + 1) == null) {
          newTreeLevel(i / 2) = null
        } else {
          newTreeLevel(i / 2) = new Array[(UInt, Bool, Bool, UInt, UInt, Bool)](outputGroupSize)
          for (j <- 0 until outputGroupSize) {
            val curOutputMemAddr = Reg(UInt(32.W))
            val curOutputMemAddrValid = Reg(Bool())
            val curOutputMemBlockValid = Reg(Bool())
            val curOutputMemBlock = Reg(UInt(bramWidth.W))
            val curOutputMemIdx = Reg(UInt(bramAddrBits.W))
            val curOutputFinished = Reg(Bool())
            if (curTreeLevel(i) == null) {
              curOutputMemAddr := curTreeLevel(i + 1)(j)._1
              curOutputMemAddrValid := curTreeLevel(i + 1)(j)._2
              curOutputMemBlockValid := curTreeLevel(i + 1)(j)._3
              curOutputMemBlock := curTreeLevel(i + 1)(j)._4
              curOutputMemIdx := curTreeLevel(i + 1)(j)._5
              curOutputFinished := curTreeLevel(i + 1)(j)._6
            } else if (curTreeLevel(i + 1) == null) {
              curOutputMemAddr := curTreeLevel(i)(j)._1
              curOutputMemAddrValid := curTreeLevel(i)(j)._2
              curOutputMemBlockValid := curTreeLevel(i)(j)._3
              curOutputMemBlock := curTreeLevel(i)(j)._4
              curOutputMemIdx := curTreeLevel(i)(j)._5
              curOutputFinished := curTreeLevel(i)(j)._6
            } else {
              curOutputMemAddr := Mux(curOutputCore(chan)(outputTreeLevel, outputTreeLevel) === 1.U,
                curTreeLevel(i + 1)(j)._1, curTreeLevel(i)(j)._1)
              curOutputMemAddrValid := Mux(curOutputCore(chan)(outputTreeLevel, outputTreeLevel) === 1.U,
                curTreeLevel(i + 1)(j)._2, curTreeLevel(i)(j)._2)
              curOutputMemBlockValid := Mux(curOutputCore(chan)(outputTreeLevel, outputTreeLevel) === 1.U,
                curTreeLevel(i + 1)(j)._3, curTreeLevel(i)(j)._3)
              curOutputMemBlock := Mux(curOutputCore(chan)(outputTreeLevel, outputTreeLevel) === 1.U,
                curTreeLevel(i + 1)(j)._4, curTreeLevel(i)(j)._4)
              curOutputMemIdx := Mux(curOutputCore(chan)(outputTreeLevel, outputTreeLevel) === 1.U,
                curTreeLevel(i + 1)(j)._5, curTreeLevel(i)(j)._5)
              curOutputFinished := Mux(curOutputCore(chan)(outputTreeLevel, outputTreeLevel) === 1.U,
                curTreeLevel(i + 1)(j)._6, curTreeLevel(i)(j)._6)
            }
            newTreeLevel(i / 2)(j) = (curOutputMemAddr, curOutputMemAddrValid, curOutputMemBlockValid,
              curOutputMemBlock, curOutputMemIdx, curOutputFinished)
          }
        }
      }
      curTreeLevel = newTreeLevel
      outputTreeLevel += 1
    }
    selOutputMemAddr(chan) = VecInit(curTreeLevel(0).map(_._1))
    selOutputMemAddrValid(chan) = VecInit(curTreeLevel(0).map(_._2))
    selOutputMemBlockValid(chan) = VecInit(curTreeLevel(0).map(_._3))
    selOutputMemBlock(chan) = VecInit(curTreeLevel(0).map(_._4))
    selOutputMemIdx(chan) = VecInit(curTreeLevel(0).map(_._5))
    selOutputFinished(chan) = VecInit(curTreeLevel(0).map(_._6))
  }

  for (i <- 0 until numInputChannels) {
    val treeCycleCounterInputAddrs = RegInit(0.asUInt(util.log2Ceil(inputTreeLevel + 1).W))
    when (treeCycleCounterInputAddrs =/= inputTreeLevel.U) {
      treeCycleCounterInputAddrs := treeCycleCounterInputAddrs + 1.U
    }
    val groupCounterInputAddrs = RegInit(0.asUInt(util.log2Ceil(Math.max(inputGroupSize, 2)).W))
    when (treeCycleCounterInputAddrs === inputTreeLevel.U && (io.inputMemAddrReadys(i) ||
      selInputAddrsIgnore(i)(groupCounterInputAddrs))) {
      when (groupCounterInputAddrs === (inputGroupSize - 1).U) {
        curInputAddrCore(i) := Mux(curInputAddrCore(i) === (inputChannelBounds(i + 1) / inputGroupSize - 1).U,
          (inputChannelBounds(i) / inputGroupSize).U, curInputAddrCore(i) + 1.U)
        treeCycleCounterInputAddrs := 0.U
        groupCounterInputAddrs := 0.U
      } .otherwise {
        groupCounterInputAddrs := groupCounterInputAddrs + 1.U
      }
    }
    // TODO potentially increase transaction size now that we have BRAM buffers and/or allow out-of-order transactions
    io.inputMemAddrs(i) := selInputMemAddr(i)(groupCounterInputAddrs)
    io.inputMemAddrValids(i) := treeCycleCounterInputAddrs === inputTreeLevel.U &&
      !selInputAddrsIgnore(i)(groupCounterInputAddrs)
    io.inputMemAddrLens(i) := (bramNumNativeLines - 1).U

    val treeCycleCounterInputBlocks = RegInit(0.asUInt(util.log2Ceil(inputTreeLevel + 1).W))
    when (treeCycleCounterInputBlocks =/= inputTreeLevel.U) {
      treeCycleCounterInputBlocks := treeCycleCounterInputBlocks + 1.U
    }
    val inputBuffer = Reg(Vec(inputGroupSize, Vec(bramNumAddrs, UInt(bramWidth.W))))
    val inputBufferIdx = RegInit(VecInit((0 until inputGroupSize).map(_ => 0.asUInt(bramAddrBits.W))))
    val inputBufferValid = RegInit(VecInit((0 until inputGroupSize).map(_ => false.B)))
    val groupCounterInputBlocks = RegInit(0.asUInt(util.log2Ceil(inputGroupSize + 1).W))
    val nativeLineCounter = RegInit(0.asUInt(util.log2Ceil(Math.max(bramNumNativeLines, 2)).W))

    when (io.inputMemBlockReadys(i) && io.inputMemBlockValids(i)) {
      nativeLineCounter := Mux(nativeLineCounter === (bramNumNativeLines - 1).U, 0.U, nativeLineCounter + 1.U)
      for (j <- 0 until bramNumAddrs) {
        when ((j / bramAddrsPerNativeLine).U === nativeLineCounter) {
          inputBuffer(groupCounterInputBlocks)(j) := io.inputMemBlocks(i)(((j % bramAddrsPerNativeLine) + 1) *
            bramWidth - 1, (j % bramAddrsPerNativeLine) * bramWidth)
        }
      }
    }
    when ((io.inputMemBlockReadys(i) && io.inputMemBlockValids(i) && nativeLineCounter === (bramNumNativeLines - 1).U)
          || (treeCycleCounterInputBlocks === inputTreeLevel.U && !(groupCounterInputBlocks === inputGroupSize.U) &&
              selInputBlocksFinished(i)(groupCounterInputBlocks))) {
      inputBufferValid(groupCounterInputBlocks) := !selInputBlocksFinished(i)(groupCounterInputBlocks)
      groupCounterInputBlocks := groupCounterInputBlocks + 1.U
    }
    when (groupCounterInputBlocks === inputGroupSize.U && inputBufferValid.asUInt === 0.U) {
      curInputBlockCore(i) := Mux(curInputBlockCore(i) === (inputChannelBounds(i + 1) / inputGroupSize - 1).U,
        (inputChannelBounds(i) / inputGroupSize).U, curInputBlockCore(i) + 1.U)
      treeCycleCounterInputBlocks := 0.U
      groupCounterInputBlocks := 0.U
    }
    when (io.inputMemBlockReadys(i) && io.inputMemBlockValids(i)) {
      // TODO this concat doesn't produce the core number when groupCounterInputBlocks has only one value
      printf(p"inputBuffer: 0x${Hexadecimal(io.inputMemBlocks(i))} for core " +
        p"${curInputBlockCore(i)##groupCounterInputBlocks}, channel $i\n")
    }

    for (j <- 0 until inputGroupSize) {
      when(inputBufferValid(j)) {
        when(inputBufferIdx(j) === (bramNumAddrs - 1).U) {
          inputBufferValid(j) := false.B
          inputBufferIdx(j) := 0.U
        }.otherwise {
          inputBufferIdx(j) := inputBufferIdx(j) + 1.U
        }
      }
    }
    // TODO make sure it's fine for this to be asserted even after the block is read
    io.inputMemBlockReadys(i) := treeCycleCounterInputBlocks === inputTreeLevel.U &&
      !(groupCounterInputBlocks === inputGroupSize.U) &&
      selInputMemBlockReady(i)(groupCounterInputBlocks) && !selInputBlocksFinished(i)(groupCounterInputBlocks)

    for (j <- inputChannelBounds(i) / inputGroupSize until inputChannelBounds(i + 1) / inputGroupSize) {
      for (k <- j * inputGroupSize until (j + 1) * inputGroupSize) {
        cores(k).inputMemAddrConsumed := Mux(curInputAddrCore(i) === j.U,
          groupCounterInputAddrs === (k - j * inputGroupSize).U && io.inputMemAddrReadys(i) &&
            io.inputMemAddrValids(i), false.B)
        cores(k).inputMemBlock := inputBuffer(k - j * inputGroupSize)(inputBufferIdx(k - j * inputGroupSize))
        cores(k).inputMemIdx := inputBufferIdx(k - j * inputGroupSize)
        cores(k).inputMemBlockValid := Mux(curInputBlockCore(i) === j.U, inputBufferValid(k - j * inputGroupSize),
          false.B)
      }
    }
  }

  for (i <- 0 until numOutputChannels) {
    val treeCycleCounterOutput = RegInit(0.asUInt(util.log2Ceil(outputTreeLevel + 1).W))
    when (treeCycleCounterOutput =/= outputTreeLevel.U) {
      treeCycleCounterOutput := treeCycleCounterOutput + 1.U
    }
    val outputMemAddrValid = RegInit(VecInit((0 until outputGroupSize).map(_ => false.B)))
    val outputMemAddr = Reg(Vec(outputGroupSize, UInt(32.W)))
    val outputBuffer = Reg(Vec(outputGroupSize, Vec(bramNumAddrs, UInt(bramWidth.W))))
    val outputBufferValid = RegInit(VecInit((0 until outputGroupSize).map(_ => false.B)))
    val groupCounterOutputAddr = RegInit(0.asUInt(util.log2Ceil(Math.max(outputGroupSize, 2)).W))
    val addrsComplete = RegInit(false.B)
    val groupCounterOutputBlock = RegInit(0.asUInt(util.log2Ceil(Math.max(outputGroupSize, 2)).W))
    val nativeLineCounter = RegInit(0.asUInt(util.log2Ceil(Math.max(bramNumNativeLines, 2)).W))
    val zerothCoreDelay = RegInit(false.B) // give zeroth core in group one cycle to produce valid address
    when (treeCycleCounterOutput === outputTreeLevel.U && !zerothCoreDelay) {
      zerothCoreDelay := true.B
    }
    for (j <- 0 until outputGroupSize) {
      when (treeCycleCounterOutput === outputTreeLevel.U && selOutputMemBlockValid(i)(j) &&
        selOutputMemIdx(i)(j) === (bramNumAddrs - 1).U) {
        outputBufferValid(j) := true.B
      }
      when (selOutputMemBlockValid(i)(j)) {
        outputBuffer(j)(selOutputMemIdx(i)(j)) := selOutputMemBlock(i)(j)
      }

      when(treeCycleCounterOutput === outputTreeLevel.U && selOutputMemAddrValid(i)(j) && !outputMemAddrValid(j) &&
        (groupCounterOutputAddr < j.U || !zerothCoreDelay)) {
        outputMemAddrValid(j) := true.B
        outputMemAddr(j) := selOutputMemAddr(i)(j)
      }
    }
    io.outputMemAddrs(i) := outputMemAddr(groupCounterOutputAddr)
    io.outputMemAddrValids(i) := outputMemAddrValid(groupCounterOutputAddr) && !addrsComplete
    io.outputMemAddrLens(i) := (bramNumNativeLines - 1).U
    io.outputMemAddrIds(i) := groupCounterOutputAddr
    val fullOutputBuf = outputBuffer(groupCounterOutputBlock).asUInt()
    var selectedOutputBlock = fullOutputBuf(511, 0)
    for (j <- 1 until bramNumNativeLines) {
      selectedOutputBlock = Mux(nativeLineCounter === j.U, fullOutputBuf(512 * (j + 1) - 1, 512 * j),
        selectedOutputBlock)
    }
    io.outputMemBlocks(i) := selectedOutputBlock
    io.outputMemBlockValids(i) := outputBufferValid(groupCounterOutputBlock)
    io.outputMemBlockLasts(i) := nativeLineCounter === (bramNumNativeLines - 1).U
    when ((treeCycleCounterOutput === outputTreeLevel.U && selOutputFinished(i)(groupCounterOutputAddr)) ||
      (io.outputMemAddrValids(i) && io.outputMemAddrReadys(i)) ||
      (zerothCoreDelay && !outputMemAddrValid(groupCounterOutputAddr))) {
      when (groupCounterOutputAddr === (outputGroupSize - 1).U) {
        addrsComplete := true.B
      } .otherwise {
        groupCounterOutputAddr := groupCounterOutputAddr + 1.U
      }
    }
    when (io.outputMemBlockValids(i) && io.outputMemBlockReadys(i)) {
      nativeLineCounter := Mux(nativeLineCounter === (bramNumNativeLines - 1).U, 0.U, nativeLineCounter + 1.U)
    }
    when ((treeCycleCounterOutput === outputTreeLevel.U && selOutputFinished(i)(groupCounterOutputBlock)) ||
      (io.outputMemBlockValids(i) && io.outputMemBlockReadys(i) && nativeLineCounter === (bramNumNativeLines - 1).U) ||
      (!outputMemAddrValid(groupCounterOutputBlock) &&
        (groupCounterOutputBlock < groupCounterOutputAddr || addrsComplete))) {
      when (groupCounterOutputBlock === (outputGroupSize - 1).U) {
        curOutputCore(i) := Mux(curOutputCore(i) === (outputChannelBounds(i + 1) / outputGroupSize - 1).U,
          (outputChannelBounds(i) / outputGroupSize).U, curOutputCore(i) + 1.U)
        treeCycleCounterOutput := 0.U
        groupCounterOutputBlock := 0.U
        groupCounterOutputAddr := 0.U
        addrsComplete := false.B
        zerothCoreDelay := false.B
        for (j <- 0 until outputGroupSize) {
          outputMemAddrValid(j) := false.B
          outputBufferValid(j) := false.B
        }
      } .otherwise {
        groupCounterOutputBlock := groupCounterOutputBlock + 1.U
      }
    }
    for (j <- outputChannelBounds(i) / outputGroupSize until outputChannelBounds(i + 1) / outputGroupSize) {
      for (k <- j * outputGroupSize until (j + 1) * outputGroupSize) {
        cores(k).outputMemAddrReady := Mux(curOutputCore(i) === j.U, treeCycleCounterOutput === outputTreeLevel.U
          && selOutputMemAddrValid(i)(k - j * outputGroupSize) && !outputMemAddrValid(k - j * outputGroupSize) &&
          (groupCounterOutputAddr < (k - j * outputGroupSize).U || !zerothCoreDelay), false.B)
        cores(k).outputMemBlockReady := Mux(curOutputCore(i) === j.U,
          groupCounterOutputBlock === (k - j * outputGroupSize).U && (io.outputMemBlockValids(i)
            && io.outputMemBlockReadys(i) && nativeLineCounter === (bramNumNativeLines - 1).U), false.B)
      }
    }

    when (io.outputMemBlockReadys(i) && io.outputMemBlockValids(i)) {
      // TODO this concat might not work, see above
      printf(p"outputBuffer: 0x${Hexadecimal(io.outputMemBlocks(i))} for core " +
        p"${curOutputCore(i)##groupCounterOutputBlock}, channel $i\n")
    }
  }
  var cumFinished = cores(0).outputFinished
  for (i <- 1 until numCores) {
    cumFinished = cumFinished && cores(i).outputFinished
  }
  io.finished := cumFinished
}

object StreamingWrapperDriver extends App {
  chisel3.Driver.execute(args, () => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L,
    1000000000L, 1000000000L), 512, 16, 16, 16, 32, 8, (coreId: Int) => new PassThrough(8, coreId)))
}