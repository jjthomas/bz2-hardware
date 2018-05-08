package examples

import chisel3._

class BandwidthTest extends StreamingWrapperBase(4, 4) {
  val outputStart = 536870912 // 2 ** 29
  val inputAddrBound = outputStart
  val outputAddrBound = outputStart + inputAddrBound
  val outputNumLines = inputAddrBound / 64
  var finished = true.B
  for (i <- 0 until 4) {
    val curInputAddr = RegInit(0.asUInt(32.W))
    val curOutputAddr = RegInit(outputStart.asUInt(32.W))
    val outputLineCounter = RegInit(0.asUInt(util.log2Ceil(outputNumLines + 1).W))
    val initialized = RegInit(false.B)
    val initAddrSent = RegInit(false.B)
    val burstSize = Reg(UInt(util.log2Ceil(64).W))
    val burstBytes = (burstSize + 1.U) ## 0.asUInt(6.W)
    val addrIncrement = Reg(UInt(32.W))
    val produceOutput = Reg(Bool())
    when (!initialized) {
      io.inputMemAddrs(i) := 0.U
      io.inputMemAddrValids(i) := !initAddrSent
      when (io.inputMemAddrReadys(i)) {
        initAddrSent := true.B
      }
      io.inputMemAddrLens(i) := 0.U

      io.inputMemBlockReadys(i) := true.B
      when (io.inputMemBlockValids(i)) {
        initialized := true.B
        var burstConverted = 0.U
        for (j <- 1 until 7) {
          burstConverted = Mux(io.inputMemBlocks(i)(2, 0) === j.U, ((1 << j) - 1).U, burstConverted)
        }
        burstSize := burstConverted
        addrIncrement := io.inputMemBlocks(i)(39, 8)
        produceOutput := io.inputMemBlocks(i)(40, 40)
      }

      io.outputMemAddrValids(i) := false.B
      io.outputMemBlockValids(i) := false.B
    } .otherwise {
      io.inputMemAddrs(i) := curInputAddr
      when(io.inputMemAddrReadys(i)) {
        when(curInputAddr + burstBytes =/= inputAddrBound.U) {
          val incrementedInputAddr = curInputAddr + addrIncrement
          curInputAddr := Mux(incrementedInputAddr >= inputAddrBound.U, incrementedInputAddr - inputAddrBound.U,
            incrementedInputAddr)
        } .otherwise {
          curInputAddr := curInputAddr + burstBytes
        }
      }
      io.inputMemAddrValids(i) := curInputAddr < inputAddrBound.U
      io.inputMemAddrLens(i) := burstSize
      io.inputMemBlockReadys(i) := Mux(produceOutput, io.outputMemBlockReadys(i), true.B)

      io.outputMemAddrs(i) := curOutputAddr
      when(io.outputMemAddrReadys(i)) {
        when(curOutputAddr + burstBytes =/= outputAddrBound.U) {
          val incrementedOutputAddr = curOutputAddr + addrIncrement
          curOutputAddr := Mux(incrementedOutputAddr >= outputAddrBound.U, incrementedOutputAddr - outputAddrBound.U,
            incrementedOutputAddr)
        } .otherwise {
          curOutputAddr := curOutputAddr + burstBytes
        }
      }
      io.outputMemAddrValids(i) := Mux(produceOutput, curOutputAddr < outputAddrBound.U, false.B)
      io.outputMemAddrLens(i) := burstSize
      io.outputMemAddrIds(i) := 0.U
      io.outputMemBlocks(i) := io.inputMemBlocks(i)
      io.outputMemBlockValids(i) := Mux(produceOutput, io.inputMemBlockValids(i), false.B)
      io.outputMemBlockLasts(i) := (outputLineCounter + 1.U) & burstSize === 0.U
      val outputIncrementCond = Mux(produceOutput, io.outputMemBlockValids(i) && io.outputMemBlockReadys(i),
        io.inputMemBlockValids(i))
      when(outputIncrementCond) {
        outputLineCounter := outputLineCounter + 1.U
      }
    }
    finished = finished && (outputLineCounter === outputNumLines.U)
  }
  io.finished := finished
}

object BandwidthTestDriver extends App {
  chisel3.Driver.execute(args, () => new BandwidthTest)
}