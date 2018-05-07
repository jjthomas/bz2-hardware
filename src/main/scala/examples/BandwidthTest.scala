package examples

import chisel3._

class BandwidthTest(produceOutput: Boolean) extends StreamingWrapperBase(4, 4) {
  val burstSize = 64
  assert(util.isPow2(burstSize))
  assert(burstSize >= 2)
  val addrIncrement = 64 * burstSize
  val outputStart = 1000000000 / addrIncrement * addrIncrement
  val inputAddrBound = outputStart
  val outputAddrBound = outputStart + inputAddrBound
  val outputNumLines = inputAddrBound / 64
  val curInputAddrs = (0 until 4).map(_ => RegInit(0.asUInt(32.W)))
  val curOutputAddrs = (0 until 4).map(_ => RegInit(outputStart.asUInt(32.W)))
  val outputLineCounters = (0 until 4).map(_ => RegInit(0.asUInt(util.log2Ceil(outputNumLines + 1).W)))
  for (i <- 0 until 4) {
    io.inputMemAddrs(i) := curInputAddrs(i)
    when (io.inputMemAddrReadys(i)) {
      when (curInputAddrs(i) < inputAddrBound.U) {
        curInputAddrs(i) := curInputAddrs(i) + addrIncrement.U
      }
    }
    io.inputMemAddrValids(i) := curInputAddrs(i) < inputAddrBound.U
    io.inputMemAddrLens(i) := (burstSize - 1).U
    io.inputMemBlockReadys(i) := (if (produceOutput) io.outputMemBlockReadys(i) else true.B)

    io.outputMemAddrs(i) := curOutputAddrs(i)
    when (io.outputMemAddrReadys(i)) {
      when (curOutputAddrs(i) < outputAddrBound.U) {
        curOutputAddrs(i) := curOutputAddrs(i) + addrIncrement.U
      }
    }
    io.outputMemAddrValids(i) := (if (produceOutput) curOutputAddrs(i) < outputAddrBound.U else false.B)
    io.outputMemAddrLens(i) := (burstSize - 1).U
    io.outputMemAddrIds(i) := curOutputAddrs(i)(27, 12) // bottom 12 bits are always 0
    io.outputMemBlocks(i) := io.inputMemBlocks(i)
    io.outputMemBlockValids(i) := (if (produceOutput) io.inputMemBlockValids(i) else false.B)
    io.outputMemBlockLasts(i) := outputLineCounters(i)(util.log2Ceil(burstSize) - 1, 0) === (burstSize - 1).U
    val outputIncrementCond = if (produceOutput) io.outputMemBlockValids(i) && io.outputMemBlockReadys(i)
      else io.inputMemBlockValids(i)
    when (outputIncrementCond) {
      outputLineCounters(i) := outputLineCounters(i) + 1.U
    }
  }
  var finished = true.B
  for (i <- 0 until 4) {
    finished = finished && (outputLineCounters(i) === outputNumLines.U)
  }
  io.finished := finished
}

object BandwidthTestDriver extends App {
  chisel3.Driver.execute(args, () => new BandwidthTest(true))
}