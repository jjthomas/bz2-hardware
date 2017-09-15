package examples

import chisel3._
import chisel3.core.{Bundle, Module}

// size must be greater than 1
class InsertionSorter(size: Int, cmpBits: Int) extends Module { // transferSize: Int, wordBits: Int
val numEls = size

  val io = IO(new Bundle {
    val blockValid = Input(Bool())
    val block = Input(UInt(64.W))
    val downstreamReady = Input(Bool())
    val thisReady = Output(Bool())
    val outValid = Output(Bool())
    val out = Output(UInt(64.W))
  })

  val regs = RegInit(VecInit((0 until size).map(_ => ((1 << cmpBits) - 1).asUInt(64.W))))
  val fillCounter = RegInit(0.asUInt(util.log2Up(size + 1).W))
  val drainCounter = RegInit(0.asUInt(util.log2Up(size).W))

  val moreSpace = Wire(Bool())
  moreSpace := fillCounter < size.U
  io.thisReady := moreSpace

  val canOut = Wire(Bool())
  canOut := fillCounter === size.U && io.downstreamReady
  io.outValid := canOut
  io.out := regs(drainCounter)

  when (moreSpace && io.blockValid) {
    fillCounter := fillCounter + 1.U
  }

  for (i <- 0 until size - 1) {
    when (moreSpace && io.blockValid) {
      when(regs(i)(cmpBits - 1, 0) >= io.block(cmpBits - 1, 0)) {
        regs(i + 1) := regs(i)
      }
    }
  }

  val minSize = util.log2Up(size) + 1
  val minValue = Wire(UInt(minSize.W))
  val comps = Wire(Vec(size, UInt(minSize.W)))
  minValue := comps.reduce((a, b) => Mux(a < b, a, b))
  for (i <- 0 until size) {
    comps(i) := (regs(i)(cmpBits - 1, 0) < io.block(cmpBits - 1, 0))##i.asUInt(util.log2Up(size).W)
  }

  when (moreSpace && io.blockValid) {
    regs(minValue(minSize - 2, 0)) := io.block
  }

  when (canOut) {
    when (drainCounter === (size - 1).U) {
      drainCounter := 0.U
      fillCounter := 0.U
    } .otherwise {
      drainCounter := drainCounter + 1.U
    }
  }

  for (i <- 0 until size) {
    when (drainCounter === (size - 1).U) {
      regs(i) := ((1 << cmpBits) - 1).U
    }
  }

}
