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
  // TODO make BRAM width configurable throughout circuit
  // TODO starting reading from inputBram before writes are complete, especially for byte-wise ops
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
  when (inputBlockLoaded && inputPieceBitsRemaining === 0.U && !(inputBitsRemaining === 0.U)) {
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
  inputAdvance := outputPieceBits =/= 16.U && !(outputBits === 512.U) && !(inputPieceBitsRemaining === 0.U)
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
  val inputMemAddr = Output(UInt(32.W))
  val inputMemAddrConsumed = Input(Bool())
  val inputMemBlock = Input(UInt(16.W))
  val inputMemIdx = Input(UInt(5.W))
  val inputMemBlockValid = Input(Bool())
  val inputMemBlockReady = Output(Bool())
  val outputMemAddr = Output(UInt(32.W))
  val outputMemAddrValid = Output(Bool())
  val outputMemAddrReady = Input(Bool())
  val outputMemBlock = Output(UInt(16.W))
  val outputMemIdx = Output(UInt(5.W))
  val outputMemBlockValid = Output(Bool())
  val outputMemBlockReady = Input(Bool())
  val inputAddrsIgnore = Output(Bool())
  val inputBlocksFinished = Output(Bool())
  val outputFinished = Output(Bool())
}

// TODO current limitation: all addresses must be 512-bit aligned
class StreamingCore(metadataPtr: Long, coreId: Int) extends Module {
  val io = IO(new StreamingCoreIO)
  val core = Module(new PassThrough)

  val isInit = RegInit(true.B)
  val initDone = RegInit(false.B)
  val inputBitsRemaining = RegInit(1.asUInt(32.W)) // init nonzero so that inputFinished isn't immediately asserted
  val coreInputFinished = RegInit(false.B)
  val outputBits = RegInit(0.asUInt(32.W))
  val outputBlockCounter = RegInit(0.asUInt(5.W))
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
    inputMemAddr := inputMemAddr + 64.U
  }

  io.inputAddrsIgnore := (isInit && initAddressAcked) || (initDone && inputMemAddr === inputMemBound)
  // TODO with some changes to the StreamingWrapper the inputBitsRemaining === 0.U check may not be needed
  io.inputMemBlockReady := isInit || (initDone && core.io.inputMemConsumed && !(inputBitsRemaining === 0.U))
  core.io.inputMemBlock := io.inputMemBlock
  core.io.inputMemIdx := io.inputMemIdx
  core.io.inputMemBlockValid := io.inputMemBlockValid && initDone
  core.io.inputBits := Mux(inputBitsRemaining > 512.U, 512.U, inputBitsRemaining)
  when (io.inputMemBlockValid) {
    when (isInit) {
      for (i <- 0 until 2) {
        when (io.inputMemIdx === i.U) {
          val result =
            if (i == 0) {
              inputMemAddr(31, 16)##io.inputMemBlock
            } else {
              io.inputMemBlock##inputMemAddr(15, 0)
            }
          inputMemAddr := result
        }
      }
      for (i <- 0 until 2) {
        when (io.inputMemIdx === (i + 4).U) {
          val result =
            if (i == 0) {
              inputBitsRemaining(31, 16)##io.inputMemBlock
            } else {
              io.inputMemBlock##inputBitsRemaining(15, 0)
            }
          inputBitsRemaining := result
          inputMemBound := inputMemAddr + result(31, 9)##0.asUInt(6.W) + Mux(result(8, 0) =/= 0.U, 64.U, 0.U)
        }
      }
      for (i <- 0 until 2) {
        when (io.inputMemIdx === (i + 8).U) {
          val result =
            if (i == 0) {
              outputMemAddr(31, 16)##io.inputMemBlock
            } else {
              io.inputMemBlock##outputMemAddr(15, 0)
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
      }
    }
  }
  io.inputBlocksFinished := inputBitsRemaining === 0.U
  when (core.io.inputMemConsumed && inputBitsRemaining === 0.U) {
    coreInputFinished := true.B
  }
  core.io.inputFinished := coreInputFinished

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
    outputBits(31, 16), 0.U))
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
                       val outputChannelStartAddrs: Array[Long], numCores: Int, inputGroupSize: Int,
                       outputGroupSize: Int) extends Module {
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
  assert(numCores % numInputChannels == 0)
  assert((numCores / numInputChannels) % inputGroupSize == 0)
  assert(numCores >= 2 * inputGroupSize)
  assert(numCores % numOutputChannels == 0)
  assert((numCores / numOutputChannels) % outputGroupSize == 0)
  assert(numCores >= 2 * outputGroupSize)

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
      (i - inputChannelBounds(curInputChannel)) * 64, i))
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
            val curOutputMemBlock = Reg(UInt(16.W))
            val curOutputMemIdx = Reg(UInt(5.W))
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

    val treeCycleCounterInputBlocks = RegInit(0.asUInt(util.log2Ceil(inputTreeLevel + 1).W))
    when (treeCycleCounterInputBlocks =/= inputTreeLevel.U) {
      treeCycleCounterInputBlocks := treeCycleCounterInputBlocks + 1.U
    }
    val inputBuffer = Reg(Vec(inputGroupSize, Vec(32, UInt(16.W))))
    val inputBufferIdx = RegInit(VecInit((0 until inputGroupSize).map(_ => 0.asUInt(5.W))))
    val inputBufferValid = RegInit(VecInit((0 until inputGroupSize).map(_ => false.B)))
    val groupCounterInputBlocks = RegInit(0.asUInt(util.log2Ceil(inputGroupSize + 1).W))
    when ((treeCycleCounterInputBlocks === inputTreeLevel.U) &&
      !(groupCounterInputBlocks === inputGroupSize.U) &&
      ((io.inputMemBlockReadys(i) && io.inputMemBlockValids(i)) ||
        selInputBlocksFinished(i)(groupCounterInputBlocks))) {
      for (j <- 0 until 32) {
        inputBuffer(groupCounterInputBlocks)(j) := io.inputMemBlocks(i)((j + 1) * 16 - 1, j * 16)
      }
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
        when(inputBufferIdx(j) === 31.U) {
          inputBufferValid(j) := false.B
          inputBufferIdx(j) := 0.U
        }.otherwise {
          inputBufferIdx(j) := inputBufferIdx(j) + 1.U
        }
      }
    }
    // TODO make sure it's fine for this to be asserted even after the block is read
    io.inputMemBlockReadys(i) := (treeCycleCounterInputBlocks === inputTreeLevel.U) &&
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
    val outputBuffer = Reg(Vec(outputGroupSize, Vec(32, UInt(16.W))))
    val outputBufferValid = RegInit(VecInit((0 until outputGroupSize).map(_ => false.B)))
    val groupCounterOutput = RegInit(0.asUInt(util.log2Ceil(Math.max(outputGroupSize, 2)).W))
    val curAddrComplete = RegInit(false.B)
    for (j <- 0 until outputGroupSize) {
      when (treeCycleCounterOutput === outputTreeLevel.U && selOutputMemBlockValid(i)(j) &&
        selOutputMemIdx(i)(j) === 31.U) {
        outputBufferValid(j) := true.B
      }
      when (selOutputMemBlockValid(i)(j)) {
        outputBuffer(j)(selOutputMemIdx(i)(j)) := selOutputMemBlock(i)(j)
      }

      when(treeCycleCounterOutput === outputTreeLevel.U && selOutputMemAddrValid(i)(j) && !outputMemAddrValid(j)) {
        outputMemAddrValid(j) := true.B
        outputMemAddr(j) := selOutputMemAddr(i)(j)
      }
    }
    io.outputMemAddrs(i) := outputMemAddr(groupCounterOutput)
    io.outputMemAddrValids(i) := outputMemAddrValid(groupCounterOutput) && !curAddrComplete
    io.outputMemBlocks(i) := outputBuffer(groupCounterOutput).asUInt
    io.outputMemBlockValids(i) := outputBufferValid(groupCounterOutput) && curAddrComplete
    val validSelOutputFinished = Wire(Bool())
    validSelOutputFinished := treeCycleCounterOutput === outputTreeLevel.U && selOutputFinished(i)(groupCounterOutput)
    when (validSelOutputFinished || (io.outputMemAddrValids(i) && io.outputMemAddrReadys(i))) {
      curAddrComplete := true.B
    }
    when (validSelOutputFinished || (io.outputMemBlockValids(i) && io.outputMemBlockReadys(i))) {
      curAddrComplete := false.B
      when (groupCounterOutput === (outputGroupSize - 1).U) {
        curOutputCore(i) := Mux(curOutputCore(i) === (outputChannelBounds(i + 1) / outputGroupSize - 1).U,
          (outputChannelBounds(i) / outputGroupSize).U, curOutputCore(i) + 1.U)
        treeCycleCounterOutput := 0.U
        groupCounterOutput := 0.U
        for (j <- 0 until outputGroupSize) {
          outputMemAddrValid(j) := false.B
          outputBufferValid(j) := false.B
        }
      } .otherwise {
        groupCounterOutput := groupCounterOutput + 1.U
      }
    }
    for (j <- outputChannelBounds(i) / outputGroupSize until outputChannelBounds(i + 1) / outputGroupSize) {
      for (k <- j * outputGroupSize until (j + 1) * outputGroupSize) {
        cores(k).outputMemAddrReady := Mux(curOutputCore(i) === j.U, treeCycleCounterOutput === outputTreeLevel.U
          && selOutputMemAddrValid(i)(k - j * outputGroupSize) && !outputMemAddrValid(k - j * outputGroupSize), false.B)
        cores(k).outputMemBlockReady := Mux(curOutputCore(i) === j.U,
          groupCounterOutput === (k - j * outputGroupSize).U && io.outputMemBlockValids(i)
            && io.outputMemBlockReadys(i), false.B)
      }
    }

    when (io.outputMemBlockReadys(i) && io.outputMemBlockValids(i)) {
      // TODO this concat might not work, see above
      printf(p"outputBuffer: 0x${Hexadecimal(io.outputMemBlocks(i))} for core " +
        p"${curOutputCore(i)##groupCounterOutput}, channel $i\n")
    }
  }
  var cumFinished = cores(0).outputFinished
  for (i <- 1 until numCores) {
    cumFinished = cumFinished && cores(i).outputFinished
  }
  io.finished := cumFinished
}

object StreamingWrapperDriver extends App {
  chisel3.Driver.execute(args, () => new StreamingWrapper(2, Array(0L, 0L), 2, Array(0L, 0L), 224, 16, 16))
}