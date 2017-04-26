package examples

import chisel3._
import chisel3.core.{Bundle, Module}

// cmpSize must be set so that there is an unused max value of all 1's
class Merger(pieceSize: Int, numPieces: Int, cmpSize: Int) extends Module {
  val totalEls = pieceSize * numPieces

  val io = IO(new Bundle {
    val blockValid = Input(Bool())
    val block = Input(UInt(64.W))
    val downstreamReady = Input(Bool())
    val thisReady = Output(Bool())
    val outValid = Output(Bool())
    val out = Output(UInt(64.W))
  })

  val pieces = new Array[Mem[UInt]](numPieces)
  for (i <- 0 until numPieces) {
    // TODO convert to SyncReadMem so BRAMs can be used
    pieces(i) = Mem(pieceSize, UInt(64.W))
  }

  val counters = new Array[UInt](numPieces)
  for (i <- 0 until numPieces) {
    counters(i) = Reg(init = 0.asUInt(util.log2Up(pieceSize + 1).W))
  }

  val fillCounter = Reg(init = 0.asUInt(util.log2Up(numPieces + 1).W))
  val drainCounter = Reg(init = 0.asUInt(util.log2Up(totalEls).W))

  val fillSpace = Wire(Bool())
  fillSpace := fillCounter < numPieces.U
  io.thisReady := fillSpace
  // TODO decoder optimization to gate the BRAMs

  for (i <- 0 until numPieces) {
    when(fillSpace && io.blockValid) {
      when(fillCounter === i.U) {
        pieces(i)(counters(i)) := io.block
        when(counters(i) === (pieceSize - 1).U) {
          fillCounter := fillCounter + 1.U
          counters(i) := 0.U
        } .otherwise {
          counters(i) := counters(i) + 1.U
        }
      }
    }
  }

  val canOutput = Wire(Bool())
  canOutput := !fillSpace && drainCounter < totalEls.U
  io.outValid := canOutput

  val minSize = util.log2Up(numPieces) + 64
  val pieceOuts = Wire(Vec(numPieces, UInt(minSize.W)))
  val minValue = Wire(UInt(minSize.W))
  minValue := pieceOuts.reduce((a, b) => Mux(a(cmpSize - 1, 0) < b(cmpSize - 1, 0), a, b))
  io.out := minValue(63, 0)

  when (canOutput) {
    when (drainCounter === (totalEls - 1).U) {
      drainCounter := 0.U
      fillCounter := 0.U
    } .otherwise {
      drainCounter := drainCounter + 1.U
    }
  }

  for (i <- 0 until numPieces) {
    when (canOutput) {
      when (drainCounter === (totalEls - 1).U) {
        counters(i) := 0.U
      } .elsewhen (minValue(minSize - 1, 64) === i.U) {
        counters(i) := counters(i) + 1.U
      }
    }
    pieceOuts(i) := i.asUInt(util.log2Up(numPieces).W)##
      Mux(counters(i) < pieceSize.U, pieces(i)(counters(i)), ((1 << cmpSize) - 1).asUInt(64.W))
  }
}
