package examples

import chisel3.iotesters.PeekPokeTester

class StreamingWrapperTests(c: StreamingWrapper, linesPerChunk: Int) extends PeekPokeTester(c) {
  val perCoreInputCounters = new Array[Array[Int]](c.numInputChannels)
  val perCoreOutputCounters = new Array[Array[Int]](c.numOutputChannels)
  for (i <- 0 until c.numInputChannels) {
    perCoreInputCounters(i) = new Array[Int](c.numCoresForInputChannel(i))
    for (j <- 0 until perCoreInputCounters(i).length) {
      perCoreInputCounters(i)(j) = 0
    }
  }
  for (i <- 0 until c.numOutputChannels) {
    perCoreOutputCounters(i) = new Array[Int](c.numCoresForOutputChannel(i))
    for (j <- 0 until perCoreOutputCounters(i).length) {
      perCoreOutputCounters(i)(j) = 0
    }
  }
  def getInputLocForOutputLoc(outputChannel: Int, outputCore: Int): (Int, Int) = {
    val absoluteOutputCore = c.outputChannelBounds(outputChannel) + outputCore
    var curInputChannel = 0
    while (c.inputChannelBounds(curInputChannel) <= absoluteOutputCore) {
      curInputChannel += 1
    }
    (curInputChannel - 1, absoluteOutputCore - c.inputChannelBounds(curInputChannel - 1))
  }
  def getOutputLocForInputLoc(inputChannel: Int, inputCore: Int): (Int, Int) = {
    val absoluteInputCore = c.inputChannelBounds(inputChannel) + inputCore
    var curOutputChannel = 0
    while (c.outputChannelBounds(curOutputChannel) <= absoluteInputCore) {
      curOutputChannel += 1
    }
    (curOutputChannel - 1, absoluteInputCore - c.outputChannelBounds(curOutputChannel - 1))
  }
  def pushBlockToInputChannel(block: BigInt, channel: Int): Unit = {
    poke(c.io.inputMemBlocks(channel), block)
    poke(c.io.inputMemBlockValids(channel), true)
    while (peek(c.io.inputMemBlockReadys(channel)).toInt == 0) {
      step(1)
      poke(c.io.inputMemBlocks(channel), block)
      poke(c.io.inputMemBlockValids(channel), true)
    }
  }
  while (peek(c.io.finished).toInt == 0) {
    for (i <- 0 until c.numInputChannels) {
      if (peek(c.io.inputMemAddrValids(i)).toInt == 1) {
        poke(c.io.inputMemAddrReadys(i), true)
        val curAddr = peek(c.io.inputMemAddrs(i)).toLong
        if (curAddr < c.inputChannelStartAddrs(i) + 64 * c.numCoresForInputChannel(i)) {
          assert((curAddr - c.inputChannelStartAddrs(i)) % 64 == 0)
          val inputCore = (curAddr - c.inputChannelStartAddrs(i)) / 64
          assert(perCoreInputCounters(i)(inputCore) == 0)
          val (outputChannel, outputCore) = getOutputLocForInputLoc(i, inputCore)
          val outputAddr = c.outputChannelStartAddrs(outputChannel) +
            64 * c.numCoresForOutputChannel(outputChannel) + 64 * (linesPerChunk + 1) * outputCore
          val inputAddr = c.inputChannelStartAddrs(i) + 64 * c.numCoresForInputChannel(i) +
            64 * linesPerChunk * inputCore
          val inputBits = linesPerChunk * 64 * 8
          val memBlock = (((BigInt(outputAddr) << 64) | inputBits) << 64) | inputAddr
          pushBlockToInputChannel(memBlock, i)
          perCoreInputCounters(i)(inputCore) += 1
        } else {
          val offset = curAddr - (c.inputChannelStartAddrs(i) + 64 * c.numCoresForInputChannel(i))
          assert(offset < linesPerChunk * 64 * c.numCoresForInputChannel(i))
          assert(offset % 64 == 0)
          val inputCore = offset / (linesPerChunk * 64)
          val inputElement = (offset / 64) % linesPerChunk
          assert(inputElement == perCoreInputCounters(i)(inputCore) - 1)
          pushBlockToInputChannel(BigInt(inputCore + inputElement), i)
          perCoreInputCounters(i)(inputCore) += 1
        }
      }
    }
    for (i <- 0 until c.numOutputChannels) {
      if (peek(c.io.outputMemAddrValids(i)).toInt == 1) {
        poke(c.io.outputMemAddrReadys(i), true)
        val curAddr = peek(c.io.outputMemAddrs(i)).toLong
        val offset = curAddr - c.outputChannelStartAddrs(i)
        assert(offset < (linesPerChunk + 1) * 64 * c.numCoresForOutputChannel(i))
        assert(offset % 64 == 0)
        val outputCore = offset / ((linesPerChunk + 1) * 64)
        val outputElement = (offset / 64) % (linesPerChunk + 1)
        if (outputElement == 0) {
          assert(perCoreOutputCounters(i)(outputCore) == linesPerChunk)
        } else {
          assert(perCoreOutputCounters(i)(outputCore) + 1 == outputElement)
        }
        poke(c.io.outputMemBlockReadys(i), true)
        while (peek(c.io.outputMemBlockValids(i)).toInt == 0) {
          step(1)
          poke(c.io.outputMemBlockReadys(i), true)
        }
        if (outputElement == 0) {
          assert(peek(c.io.outputMemBlocks(i)).toInt == linesPerChunk * 64 * 8)
        } else {
          val (_, inputCore) = getInputLocForOutputLoc(i, outputCore)
          assert(peek(c.io.outputMemBlocks(i)).toInt == inputCore + outputElement - 1)
        }
        perCoreOutputCounters(i)(outputCore) += 1
      }
    }
    step(1)
  }
}
