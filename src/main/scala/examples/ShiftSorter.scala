package examples

import chisel3._
import chisel3.core.{Bundle, Module}

// cmpSize must be set so that there is an unused max value of all 1's
// size must be greater than 1
class ShiftSorter(size: Int, cmpBits: Int) extends Module { // transferSize: Int, wordBits: Int
  val numEls = size

  val io = IO(new Bundle {
    val blockValid = Input(Bool())
    val block = Input(UInt(64.W))
    val downstreamReady = Input(Bool())
    val thisReady = Output(Bool())
    val outValid = Output(Bool())
    val out = Output(UInt(64.W))
  })

  val regs = Reg(init = Vec.do_fill(size) { ((1 << cmpBits) - 1).asUInt(64.W) })
  val fillCounter = Reg(init = 0.asUInt(util.log2Up(size + 1).W))
  val settleCounter = Reg(init = 0.asUInt(util.log2Up(size).W))
  val drainCounter = Reg(init = (size - 1).asUInt(util.log2Up(size).W))

  val moreSpace = Wire(Bool())
  moreSpace := fillCounter < size.U
  io.thisReady := moreSpace

  val canOut = Wire(Bool())
  canOut := settleCounter === (size - 1).U && io.downstreamReady
  io.outValid := canOut
  io.out := regs(drainCounter)

  when (moreSpace && io.blockValid) {
    fillCounter := fillCounter + 1.U
    regs((size - 1).U - fillCounter) := io.block
  }

  when (!moreSpace && settleCounter < (size - 1).U) {
    settleCounter := settleCounter + 1.U
  }

  for (i <- 0 until size - 1) {
    when (regs(i) < regs(i + 1)) {
      regs(i) := regs(i + 1)
      regs(i + 1) := regs(i)
    }
  }

  when (canOut) {
    when (drainCounter === 0.U) {
      drainCounter := (size - 1).U
      fillCounter := 0.U
      settleCounter := 0.U
    } .otherwise {
      drainCounter := drainCounter - 1.U
    }
  }

  for (i <- 0 until size) {
    when (drainCounter === 0.U) {
      regs(i) := ((1 << cmpBits) - 1).U
    }
  }

}
