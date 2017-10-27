package examples

import chisel3.iotesters.PeekPokeTester

class StreamingWrapperTests(c: StreamingWrapper, numInputBits: Int, inputBits: Array[BigInt])
  extends PeekPokeTester(c) {
  val (numOutputBits, outputBits) = c.puFactory().produceOutput(numInputBits, inputBits)
  val inputLines = (numInputBits - 1) / 512 + 1
  val outputLines = (numOutputBits - 1) / 512 + 1
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
    step(1)
    poke(c.io.inputMemAddrReadys(channel), false)
    poke(c.io.inputMemBlocks(channel), block)
    poke(c.io.inputMemBlockValids(channel), true)
    while (peek(c.io.inputMemBlockReadys(channel)).toInt == 0) {
      step(1)
      poke(c.io.inputMemBlocks(channel), block)
      poke(c.io.inputMemBlockValids(channel), true)
    }
    step(1)
    poke(c.io.inputMemBlockValids(channel), false)
  }
  for (i <- 0 until c.numInputChannels) {
    poke(c.io.inputMemAddrReadys(i), false)
    poke(c.io.inputMemBlockValids(i), false)
  }
  for (i <- 0 until c.numOutputChannels) {
    poke(c.io.outputMemAddrReadys(i), false)
    poke(c.io.outputMemBlockReadys(i), false)
  }
  while (peek(c.io.finished).toInt == 0) {
    for (i <- 0 until c.numInputChannels) {
      if (peek(c.io.inputMemAddrValids(i)).toInt == 1) {
        step(1)
        poke(c.io.inputMemAddrReadys(i), true)
        val curAddr = peek(c.io.inputMemAddrs(i)).toLong
        println("valid input addr from channel: " + i + ", addr: " + curAddr)
        if (curAddr < c.inputChannelStartAddrs(i) + 64 * c.numCoresForInputChannel(i)) {
          assert((curAddr - c.inputChannelStartAddrs(i)) % 64 == 0)
          val inputCore = (curAddr - c.inputChannelStartAddrs(i)) / 64
          assert(perCoreInputCounters(i)(inputCore) == 0)
          val (outputChannel, outputCore) = getOutputLocForInputLoc(i, inputCore)
          val outputAddr = c.outputChannelStartAddrs(outputChannel) +
            64 * (inputLines + 1) * outputCore
          val inputAddr = c.inputChannelStartAddrs(i) + 64 * c.numCoresForInputChannel(i) +
            64 * inputLines * inputCore
          val memBlock = (((BigInt(outputAddr) << 64) | numInputBits) << 64) | inputAddr
          pushBlockToInputChannel(memBlock, i)
          perCoreInputCounters(i)(inputCore) += 1
        } else {
          val offset = curAddr - (c.inputChannelStartAddrs(i) + 64 * c.numCoresForInputChannel(i))
          assert(offset < inputLines * 64 * c.numCoresForInputChannel(i))
          assert(offset % 64 == 0)
          val inputCore = offset / (inputLines * 64)
          val inputElement = (offset / 64) % inputLines
          assert(inputElement == perCoreInputCounters(i)(inputCore) - 1)
          pushBlockToInputChannel(inputBits(inputElement), i)
          perCoreInputCounters(i)(inputCore) += 1
        }
        println("pushed valid input block to channel: " + i)
      }
    }
    for (i <- 0 until c.numOutputChannels) {
      if (peek(c.io.outputMemAddrValids(i)).toInt == 1) {
        step(1)
        poke(c.io.outputMemAddrReadys(i), true)
        val curAddr = peek(c.io.outputMemAddrs(i)).toLong
        println("valid output addr from channel: " + i + ", addr: " + curAddr)
        val offset = curAddr - c.outputChannelStartAddrs(i)
        assert(offset < (inputLines + 1) * 64 * c.numCoresForOutputChannel(i))
        assert(offset % 64 == 0)
        val outputCore = offset / ((inputLines + 1) * 64)
        val outputElement = (offset / 64) % (inputLines + 1)
        if (outputElement == 0) {
          assert(perCoreOutputCounters(i)(outputCore) == outputLines)
        } else {
          assert(perCoreOutputCounters(i)(outputCore) + 1 == outputElement)
        }
        step(1)
        poke(c.io.outputMemAddrReadys(i), false)
        poke(c.io.outputMemBlockReadys(i), true)
        while (peek(c.io.outputMemBlockValids(i)).toInt == 0) {
          step(1)
          poke(c.io.outputMemBlockReadys(i), true)
        }
        println("read valid output element from channel: " + i + ", element: " + peek(c.io.outputMemBlocks(i)).toInt)
        if (outputElement == 0) {
          assert(peek(c.io.outputMemBlocks(i)).toInt == numOutputBits)
        } else {
          val (_, inputCore) = getInputLocForOutputLoc(i, outputCore)
          assert(peek(c.io.outputMemBlocks(i)).toInt == outputBits(outputElement - 1))
        }
        step(1)
        poke(c.io.outputMemBlockReadys(i), false)
        perCoreOutputCounters(i)(outputCore) += 1
      }
    }
    step(1)
  }
  for (chan <- perCoreInputCounters) {
    for (counter <- chan) {
      assert(counter == inputLines + 1)
    }
  }
  for (chan <- perCoreOutputCounters) {
    for (counter <- chan) {
      assert(counter == outputLines + 1)
    }
  }
}
