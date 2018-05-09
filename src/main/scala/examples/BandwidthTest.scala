package examples

import chisel3._

class BandwidthTest extends StreamingWrapperBase(4, 4) {
  var finished = true.B
  for (i <- 0 until 4) {
    val outputStart = Reg(UInt(31.W))
    val inputAddrBound = outputStart
    val outputAddrBound = outputStart ## 0.asUInt(1.W)
    val outputNumLines = inputAddrBound(30, 6) // divide by 64
    val curInputAddr = RegInit(0.asUInt(32.W))
    val curOutputAddr = RegInit(0.asUInt(31.W))
    val outputLineCounter = RegInit(0.asUInt(26.W)) // enough bits to store outputNumLines + 1
    val initialized = RegInit(false.B)
    val initAddrSent = RegInit(false.B)
    val burstSize = Reg(UInt(6.W))
    val burstBytes = (burstSize +& 1.U) ## 0.asUInt(6.W)
    val addrIncrement = Reg(UInt(32.W))
    val produceOutput = Reg(Bool())
    when (!initialized) {
      io.inputMemAddrs(i) := 0.U
      io.inputMemAddrValids(i) := !initAddrSent
      when(io.inputMemAddrValids(i) && io.inputMemAddrReadys(i)) {
        initAddrSent := true.B
      }
      io.inputMemAddrLens(i) := 0.U

      io.inputMemBlockReadys(i) := true.B
      when(io.inputMemBlockValids(i) && io.inputMemBlockReadys(i)) {
        initialized := true.B
        burstSize := io.inputMemBlocks(i)(5, 0)
        addrIncrement := io.inputMemBlocks(i)(39, 8)
        produceOutput := io.inputMemBlocks(i)(40, 40)
        outputStart := io.inputMemBlocks(i)(94, 64)
      }

      io.outputMemAddrValids(i) := false.B
      io.outputMemBlockValids(i) := false.B
    } .otherwise {
      io.inputMemAddrs(i) := curInputAddr
      io.inputMemAddrValids(i) := curInputAddr =/= inputAddrBound
      when(io.inputMemAddrValids(i) && io.inputMemAddrReadys(i)) {
        when(curInputAddr + burstBytes =/= inputAddrBound) {
          val incrementedInputAddr = curInputAddr + addrIncrement
          curInputAddr := Mux(incrementedInputAddr >= inputAddrBound,
            incrementedInputAddr - inputAddrBound + burstBytes, incrementedInputAddr)
        } .otherwise {
          curInputAddr := inputAddrBound
        }
      }
      io.inputMemAddrLens(i) := burstSize
      io.inputMemBlockReadys(i) := Mux(produceOutput, io.outputMemBlockReadys(i), true.B)

      io.outputMemAddrs(i) := curOutputAddr
      io.outputMemAddrValids(i) := Mux(produceOutput, curOutputAddr =/= outputAddrBound, false.B)
      when(io.outputMemAddrValids(i) && io.outputMemAddrReadys(i)) {
        when(curOutputAddr + burstBytes =/= outputAddrBound) {
          val incrementedOutputAddr = curOutputAddr + addrIncrement
          curOutputAddr := Mux(incrementedOutputAddr >= outputAddrBound,
            incrementedOutputAddr - outputAddrBound + burstBytes, incrementedOutputAddr)
        } .otherwise {
          curOutputAddr := outputAddrBound
        }
      }
      io.outputMemAddrLens(i) := burstSize
      io.outputMemAddrIds(i) := 0.U
      io.outputMemBlocks(i) := io.inputMemBlocks(i)
      io.outputMemBlockValids(i) := Mux(produceOutput, io.inputMemBlockValids(i), false.B)
      io.outputMemBlockLasts(i) := ((outputLineCounter + 1.U) & burstSize) === 0.U
      val outputIncrementCond = Mux(produceOutput, io.outputMemBlockValids(i) && io.outputMemBlockReadys(i),
        io.inputMemBlockValids(i) && io.inputMemBlockReadys(i))
      when(outputIncrementCond) {
        outputLineCounter := outputLineCounter + 1.U
      }
    }
    finished = finished && (outputLineCounter === outputNumLines)
  }
  io.finished := finished
}

object BandwidthTestDriver extends App {
  chisel3.Driver.execute(args, () => new BandwidthTest)
}