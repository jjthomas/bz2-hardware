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
    val inputFinished = Input(Bool())
    val outputMemBlock = Output(UInt(bramWidth.W))
    val outputMemBlockValid = Output(Bool())
    val outputMemBlockReady = Input(Bool())
    val outputBits = Output(UInt(util.log2Ceil(bramLineSize + 1).W))
    val outputFinished = Output(Bool())
  })
  assert(wordBits <= bramWidth)
  assert(bramWidth % wordBits == 0)
  val inner = Module(puFactory(coreId))

  // TODO this does not need to be coupled with the bramWidth (same with outputMemBlock)
  // TODO why do we need this register block at all? Just directly select the output from the BRAM
  val inputMemBlock = Reg(Vec(bramWidth, Bool()))
  val inputPieceBitsRemaining = RegInit(0.asUInt(util.log2Ceil(bramWidth + 1).W))
  val inputBitsRemaining = RegInit(0.asUInt(util.log2Ceil(bramLineSize + 1).W))
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
    inputBitsRemaining := io.inputBits
  }

  inputBram.io.b_wr := false.B
  val inputReadAddrFinal = Wire(UInt(bramAddrBits.W))
  inputBram.io.b_addr := inputReadAddrFinal
  // inputPieceBitsRemaining === 0.U is true at the start and end of the reading of a block, here
  // we ensure that we capture it only in the start case
  when ((inputPieceBitsRemaining === 0.U && !(inputBitsRemaining === 0.U)) ||
        (inner.io.inputValid && inner.io.inputReady && ((inputPieceBitsRemaining - wordBits.U) === 0.U))) {
    val newInputBitsRemaining = Mux(inputPieceBitsRemaining === 0.U, inputBitsRemaining,
      inputBitsRemaining - wordBits.U)
    inputPieceBitsRemaining := Mux(newInputBitsRemaining < bramWidth.U, newInputBitsRemaining, bramWidth.U)
    inputBitsRemaining := newInputBitsRemaining
    inputReadAddr := Mux(newInputBitsRemaining === 0.U, inputReadAddr, inputReadAddr + 1.U)
    inputReadAddrFinal := inputReadAddr + 1.U
    for (i <- 0 until bramWidth) {
      inputMemBlock(i) := inputBram.io.b_dout(i)
    }
  } .otherwise {
    inputReadAddrFinal := inputReadAddr
  }
  when (inner.io.inputValid && inner.io.inputReady && !((inputPieceBitsRemaining - wordBits.U) === 0.U)) {
    inputPieceBitsRemaining := inputPieceBitsRemaining - wordBits.U
    inputBitsRemaining := inputBitsRemaining - wordBits.U
    for (i <- 0 until (bramWidth - wordBits)) {
      inputMemBlock(i) := inputMemBlock(i + wordBits)
    }
  }

  val nextWord = inputMemBlock.asUInt()(wordBits - 1, 0)
  inner.io.inputValid := !(inputPieceBitsRemaining === 0.U)
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
  when (io.outputMemBlockReady && outputReadingStarted && !(outputReadAddr === (bramNumAddrs - 1).U)) {
    outputReadAddr := outputReadAddr + 1.U
    when (outputReadAddr === 0.U) {
      outputBits := 0.U // allow new output bits to be written into the output BRAM right away
    }
  }
  io.inputMemConsumed := inputBitsRemaining === 0.U
  io.outputMemBlockValid := outputReadingStarted
  io.outputMemBlock := outputBram.io.b_dout
  io.outputBits := outputBits
  io.outputFinished := inner.io.outputFinished && outputBits === 0.U && outputReadAddr === 0.U // last term needed
  // so that this signal is only asserted after outputMemFlushed signaled for last block
  when (outputReadingStarted && outputReadAddr === (bramNumAddrs - 1).U) {
    outputReadingStartedPrev := false.B
    outputReadingStarted := false.B
    outputReadAddr := 0.U
  }
}

class StreamingCoreIO(bramWidth: Int, bramNumAddrs: Int) extends Bundle {
  val bramAddrBits = util.log2Ceil(bramNumAddrs)

  val inputMemAddr = Output(UInt(32.W))
  val inputMemAddrValid = Output(Bool())
  val inputMemBlock = Input(UInt(bramWidth.W))
  val inputMemIdx = Input(UInt(bramAddrBits.W))
  val inputMemBlockValid = Input(Bool())
  val outputMemAddr = Output(UInt(32.W))
  val outputMemAddrValid = Output(Bool())
  val outputMemAddrReady = Input(Bool())
  val outputMemBlock = Output(UInt(bramWidth.W))
  val outputMemIdx = Output(UInt(bramAddrBits.W))
  val outputMemBlockValid = Output(Bool())
  val outputFinished = Output(Bool())

  override def cloneType(): this.type = new StreamingCoreIO(bramWidth, bramNumAddrs).asInstanceOf[this.type]
}

// TODO current limitation: all addresses must be 512-bit aligned
class StreamingCore(metadataPtr: Long, bramWidth: Int, bramNumAddrs: Int, wordSize: Int,
                    puFactory: (Int) => ProcessingUnit, coreId: Int) extends Module {
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
  val outputLengthSent = RegInit(false.B)
  val inputMemAddr = RegInit(metadataPtr.asUInt(32.W))
  val outputMemAddr = Reg(UInt(32.W))
  val outputLenAddr = Reg(UInt(32.W))
  val outputMemFlushed = RegInit(false.B)

  io.inputMemAddr := inputMemAddr
  // TODO with some changes to the StreamingWrapper the inputBitsRemaining === 0.U check may not be needed
  io.inputMemAddrValid := isInit || (initDone && core.io.inputMemConsumed && !(inputBitsRemaining === 0.U))
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
        inputMemAddr := inputMemAddr + bytesInLine.U
      }
    }
  }
  core.io.inputFinished := initDone && inputBitsRemaining === 0.U

  val outputAddressAccepted = RegInit(false.B)
  val outputAddressAcceptedNext = RegInit(false.B) // because it takes a cycle after core.io.outputMemBlockReady
  // signalled for output to actually arrive
  when (outputAddressAccepted) {
    outputAddressAcceptedNext := true.B
  }
  io.outputMemAddr := Mux(core.io.outputFinished, outputLenAddr, outputMemAddr)
  io.outputMemAddrValid := !outputAddressAccepted && (core.io.outputMemBlockValid ||
    (core.io.outputFinished && !outputLengthSent))
  when (io.outputMemAddrValid && io.outputMemAddrReady) {
    outputAddressAccepted := true.B
    when (!core.io.outputFinished) {
      outputBits := outputBits + core.io.outputBits
      outputMemAddr := outputMemAddr + bytesInLine.U
    } .otherwise {
      outputLengthSent := true.B
    }
  }
  core.io.outputMemBlockReady := outputAddressAccepted
  when (outputAddressAcceptedNext) {
    when (outputBlockCounter === (bramNumAddrs - 1).U) {
      outputBlockCounter := 0.U
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
  io.outputMemBlock := Mux(outputLengthSent, outputBitsBlock, core.io.outputMemBlock)
  io.outputMemIdx := outputBlockCounter

  io.outputMemBlockValid := outputAddressAcceptedNext
  when (outputAddressAcceptedNext && outputBlockCounter === (bramNumAddrs - 1).U) {
    outputAddressAccepted := false.B
    outputAddressAcceptedNext := false.B
  }
  io.outputFinished := outputLengthSent && !outputAddressAccepted
}

class StreamingWrapperIO(numInputChannels: Int, numOutputChannels: Int) extends Bundle {
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

  override def cloneType(): this.type =
    new StreamingWrapperIO(numInputChannels, numOutputChannels).asInstanceOf[this.type]
}

class StreamingWrapperBase(numInputChannels: Int, numOutputChannels: Int) extends Module {
  val io = IO(new StreamingWrapperIO(numInputChannels, numOutputChannels))
}

class StreamingWrapper(val numInputChannels: Int, val inputChannelStartAddrs: Array[Long], val numOutputChannels: Int,
                       val outputChannelStartAddrs: Array[Long], val numCores: Int, inputGroupSize: Int,
                       inputNumReadAheadGroups: Int, outputGroupSize: Int, bramWidth: Int, bramNumAddrs: Int,
                       wordSize: Int, val puFactory: (Int) => ProcessingUnit)
                       extends StreamingWrapperBase(numInputChannels, numOutputChannels) {
  assert(numCores % numInputChannels == 0)
  assert((numCores / numInputChannels) % inputGroupSize == 0)
  assert(numCores >= 2 * inputGroupSize)
  assert(inputNumReadAheadGroups >= 1)
  assert(util.isPow2(inputNumReadAheadGroups))
  assert(numCores / numInputChannels >= inputNumReadAheadGroups * inputGroupSize)
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
  val curInputDataCore = new Array[UInt](numInputChannels)
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
    curInputDataCore(i) = RegInit((inputChannelBounds(i) / inputGroupSize)
      .asUInt(util.log2Ceil(numCores / inputGroupSize).W))
  }
  for (i <- 0 until numOutputChannels) {
    curOutputCore(i) = RegInit((outputChannelBounds(i) / outputGroupSize)
      .asUInt(util.log2Ceil(numCores/ outputGroupSize).W))
  }

  var inputTreeLevel = 0
  val selInputMemAddr = new Array[Vec[UInt]](numInputChannels)
  val selInputMemAddrValid = new Array[Vec[Bool]](numInputChannels)
  for (chan <- 0 until numInputChannels) {
    var curTreeLevel = new Array[Array[(UInt, Bool)]](Math.pow(2, util.log2Ceil(numCores)).toInt
      / inputGroupSize)
    for (i <- 0 until curTreeLevel.length) {
      val coreIndex = i * inputGroupSize
      if (coreIndex >= inputChannelBounds(chan) && coreIndex < inputChannelBounds(chan + 1)) {
        val curGroup = new Array[(UInt, Bool)](inputGroupSize)
        for (j <- coreIndex until coreIndex + inputGroupSize) {
          curGroup(j - coreIndex) = (cores(j).inputMemAddr, cores(j).inputMemAddrValid)
        }
        curTreeLevel(i) = curGroup
      } else {
        curTreeLevel(i) = null
      }
    }
    inputTreeLevel = 0
    while (curTreeLevel.length > 1) {
      val newTreeLevel = new Array[Array[(UInt, Bool)]](curTreeLevel.length / 2)
      for (i <- 0 until curTreeLevel.length by 2) {
        if (curTreeLevel(i) == null && curTreeLevel(i + 1) == null) {
          newTreeLevel(i / 2) = null
        } else {
          newTreeLevel(i / 2) = new Array[(UInt, Bool)](inputGroupSize)
          for (j <- 0 until inputGroupSize) {
            // TODO make use of registers configurable (e.g. every other tree level, every 4, etc.)
            val curInputMemAddr = Reg(UInt(32.W))
            val curInputMemAddrValid = Reg(Bool())
            if (curTreeLevel(i) == null) {
              curInputMemAddr := curTreeLevel(i + 1)(j)._1
              curInputMemAddrValid := curTreeLevel(i + 1)(j)._2
            } else if (curTreeLevel(i + 1) == null) {
              curInputMemAddr := curTreeLevel(i)(j)._1
              curInputMemAddrValid := curTreeLevel(i)(j)._2
            } else {
              curInputMemAddr := Mux(curInputAddrCore(chan)(inputTreeLevel, inputTreeLevel) === 1.U,
                curTreeLevel(i + 1)(j)._1, curTreeLevel(i)(j)._1)
              curInputMemAddrValid := Mux(curInputAddrCore(chan)(inputTreeLevel, inputTreeLevel) === 1.U,
                curTreeLevel(i + 1)(j)._2, curTreeLevel(i)(j)._2)
            }
            newTreeLevel(i / 2)(j) = (curInputMemAddr, curInputMemAddrValid)
          }
        }
      }
      curTreeLevel = newTreeLevel
      inputTreeLevel += 1
    }
    selInputMemAddr(chan) = VecInit(curTreeLevel(0).map(_._1))
    selInputMemAddrValid(chan) = VecInit(curTreeLevel(0).map(_._2))
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
            val curOutputFinished = RegInit(false.B)
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
              curOutputFinished := curTreeLevel(i + 1)(j)._6 && curTreeLevel(i)(j)._6
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
    val treeCycleCounterInput = RegInit(0.asUInt(util.log2Ceil(inputTreeLevel + 1).W))
    when (!(treeCycleCounterInput === inputTreeLevel.U)) {
      treeCycleCounterInput := treeCycleCounterInput + 1.U
    }
    // required because valid may go from false to true during the processing of a group, and we
    // want to use only the valid value we originally saw when consuming the group's addresses
    val inputMemAddrValid = RegInit(VecInit((0 until inputGroupSize * inputNumReadAheadGroups).map(_ => false.B)))
    val inputBuffer = Reg(Vec(inputGroupSize, Vec(bramNumAddrs, UInt(bramWidth.W))))
    val inputBufferIdx = RegInit(VecInit((0 until inputGroupSize).map(_ => 0.asUInt(bramAddrBits.W))))
    val inputBufferValid = RegInit(VecInit((0 until inputGroupSize).map(_ => false.B)))
    val groupCounterInputAddr = RegInit(0.asUInt(util.log2Ceil(Math.max(inputGroupSize, 2)).W))
    val addrsComplete = RegInit(false.B)
    val groupCounterInputBlock = RegInit(0.asUInt(util.log2Ceil(Math.max(inputGroupSize, 2)).W))
    val blocksComplete = RegInit(false.B)
    val nativeLineCounter = RegInit(0.asUInt(util.log2Ceil(Math.max(bramNumNativeLines, 2)).W))
    val zerothCoreDelay = RegInit(false.B) // give zeroth core in group one cycle to write inputMemAddrValid
    val addrReadAheadCounter = RegInit(0.asUInt(Math.max(1, util.log2Ceil(inputNumReadAheadGroups)).W))
    val dataReadAheadCounter = RegInit(0.asUInt(Math.max(1, util.log2Ceil(inputNumReadAheadGroups)).W))
    def inputMemAddrValidAtConst(c: Int, readAheadCounter: UInt): Bool = {
      if (inputNumReadAheadGroups > 1) {
        var result = inputMemAddrValid(c.U ## 0.asUInt(util.log2Ceil(inputNumReadAheadGroups).W))
        for (i <- 1 until inputNumReadAheadGroups) {
          result = Mux(readAheadCounter === i.U,
            inputMemAddrValid(c.U ## i.asUInt(util.log2Ceil(inputNumReadAheadGroups).W)), result)
        }
        result
      } else {
        inputMemAddrValid(c)
      }
    }
    def writeInputMemAddrValidAtConst(c: Int, readAheadCounter: UInt, result: Bool): Unit = {
      if (inputNumReadAheadGroups > 1) {
        for (i <- 0 until inputNumReadAheadGroups) {
          when(readAheadCounter === i.U) {
            inputMemAddrValid(c.U ## i.asUInt(util.log2Ceil(inputNumReadAheadGroups).W)) := result
          }
        }
      } else {
        inputMemAddrValid(c) := result
      }
    }
    val curInputMemAddrValidAddr = if (inputNumReadAheadGroups > 1)
      inputMemAddrValid(groupCounterInputAddr##addrReadAheadCounter) else inputMemAddrValid(groupCounterInputAddr)
    val curInputMemAddrValidData = if (inputNumReadAheadGroups > 1)
      inputMemAddrValid(groupCounterInputBlock##dataReadAheadCounter) else inputMemAddrValid(groupCounterInputBlock)

    when (treeCycleCounterInput === inputTreeLevel.U && !zerothCoreDelay) {
      zerothCoreDelay := true.B
    }
    for (j <- 0 until inputGroupSize) {
      when(treeCycleCounterInput === inputTreeLevel.U && selInputMemAddrValid(i)(j) &&
        !inputMemAddrValidAtConst(j, addrReadAheadCounter) && (groupCounterInputAddr < j.U || !zerothCoreDelay)) {
        writeInputMemAddrValidAtConst(j, addrReadAheadCounter, true.B)
      }
    }
    io.inputMemAddrs(i) := selInputMemAddr(i)(groupCounterInputAddr)
    io.inputMemAddrValids(i) := treeCycleCounterInput === inputTreeLevel.U && curInputMemAddrValidAddr && !addrsComplete
    io.inputMemAddrLens(i) := (bramNumNativeLines - 1).U
    when ((io.inputMemAddrValids(i) && io.inputMemAddrReadys(i)) ||
      (zerothCoreDelay && !addrsComplete && !curInputMemAddrValidAddr)) {
      when (groupCounterInputAddr === (inputGroupSize - 1).U) {
        addrsComplete := true.B
      } .otherwise {
        groupCounterInputAddr := groupCounterInputAddr + 1.U
      }
    }
    val addrsAdvanceCond = if (inputNumReadAheadGroups > 1) addrReadAheadCounter + 1.U =/= dataReadAheadCounter else
      blocksComplete && inputBufferValid.asUInt === 0.U
    when (addrsComplete && addrsAdvanceCond) {
      curInputAddrCore(i) := Mux(curInputAddrCore(i) === (inputChannelBounds(i + 1) / inputGroupSize - 1).U,
        (inputChannelBounds(i) / inputGroupSize).U, curInputAddrCore(i) + 1.U)
      if (inputNumReadAheadGroups > 1) {
        addrReadAheadCounter := addrReadAheadCounter + 1.U // wraps around
      }
      treeCycleCounterInput := 0.U
      groupCounterInputAddr := 0.U
      addrsComplete := false.B
      zerothCoreDelay := false.B
    }
    for (j <- 0 until inputGroupSize) {
      when(inputBufferValid(j)) {
        when(inputBufferIdx(j) === (bramNumAddrs - 1).U) {
          writeInputMemAddrValidAtConst(j, dataReadAheadCounter, false.B)
          inputBufferValid(j) := false.B
          inputBufferIdx(j) := 0.U
        }.otherwise {
          inputBufferIdx(j) := inputBufferIdx(j) + 1.U
        }
      }
    }
    when (io.inputMemBlockReadys(i) && io.inputMemBlockValids(i)) {
      nativeLineCounter := Mux(nativeLineCounter === (bramNumNativeLines - 1).U, 0.U, nativeLineCounter + 1.U)
      for (j <- 0 until bramNumAddrs) {
        when ((j / bramAddrsPerNativeLine).U === nativeLineCounter) {
          inputBuffer(groupCounterInputBlock)(j) := io.inputMemBlocks(i)(((j % bramAddrsPerNativeLine) + 1) *
            bramWidth - 1, (j % bramAddrsPerNativeLine) * bramWidth)
        }
      }
    }
    when (blocksComplete && inputBufferValid.asUInt === 0.U) {
      curInputDataCore(i) := Mux(curInputDataCore(i) === (inputChannelBounds(i + 1) / inputGroupSize - 1).U,
        (inputChannelBounds(i) / inputGroupSize).U, curInputDataCore(i) + 1.U)
      if (inputNumReadAheadGroups > 1) {
        dataReadAheadCounter := dataReadAheadCounter + 1.U // wraps around
      }
      groupCounterInputBlock := 0.U
      blocksComplete := false.B
    }
    when ((io.inputMemBlockValids(i) && io.inputMemBlockReadys(i) && nativeLineCounter === (bramNumNativeLines - 1).U)
      || (!curInputMemAddrValidData && !blocksComplete &&
          (addrReadAheadCounter =/= dataReadAheadCounter || groupCounterInputBlock < groupCounterInputAddr ||
            addrsComplete))) {
      when (curInputMemAddrValidData) {
        inputBufferValid(groupCounterInputBlock) := true.B
      }
      when (groupCounterInputBlock === (inputGroupSize - 1).U) {
        blocksComplete := true.B
      } .otherwise {
        groupCounterInputBlock := groupCounterInputBlock + 1.U
      }
    }
    io.inputMemBlockReadys(i) := curInputMemAddrValidData && !blocksComplete
    for (j <- inputChannelBounds(i) / inputGroupSize until inputChannelBounds(i + 1) / inputGroupSize) {
      for (k <- j * inputGroupSize until (j + 1) * inputGroupSize) {
        cores(k).inputMemBlock := inputBuffer(k - j * inputGroupSize)(inputBufferIdx(k - j * inputGroupSize))
        cores(k).inputMemIdx := inputBufferIdx(k - j * inputGroupSize)
        cores(k).inputMemBlockValid := Mux(curInputDataCore(i) === j.U, inputBufferValid(k - j * inputGroupSize),
          false.B)
      }
    }
  }

  val outputChannelsComplete = new Array[Bool](numOutputChannels)
  for (i <- 0 until numOutputChannels) {
    val treeCycleCounterOutput = RegInit(0.asUInt(util.log2Ceil(outputTreeLevel + 1).W))
    when (treeCycleCounterOutput =/= outputTreeLevel.U) {
      treeCycleCounterOutput := treeCycleCounterOutput + 1.U
    }
    // need the following two fields because, unlike in the input case, address may not have been emitted to
    // the shell even when the block is fully written to the internal buffer, so we need to save the address information
    // here
    val outputMemAddrValid = RegInit(VecInit((0 until outputGroupSize).map(_ => false.B)))
    outputChannelsComplete(i) = outputMemAddrValid.asUInt === 0.U
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
    when ((io.outputMemAddrValids(i) && io.outputMemAddrReadys(i)) ||
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
    when ((io.outputMemBlockValids(i) && io.outputMemBlockReadys(i) && nativeLineCounter === (bramNumNativeLines - 1).U)
      || (!outputMemAddrValid(groupCounterOutputBlock) &&
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
      }
    }
  }
  var cumFinished = true.B
  for (i <- 0 until numOutputChannels) {
    for (j <- 0 until outputGroupSize) {
      cumFinished = cumFinished && selOutputFinished(i)(j)
    }
  }
  for (i <- 0 until numOutputChannels) {
    cumFinished = cumFinished && outputChannelsComplete(i)
  }
  io.finished := cumFinished
}

object StreamingWrapperDriver extends App {
  chisel3.Driver.execute(args, () => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L,
    1000000000L, 1000000000L), 512, 16, 2, 16, 32, 32, 8, (coreId: Int) => new PassThrough(8, coreId)))
}