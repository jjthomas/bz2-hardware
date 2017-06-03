package examples

import chisel3._
import chisel3.core.{Bundle, Module}

// cmpBits sequence of 1's must be an unused max value
// size must be greater than 2
class SystolicShiftSorter(size: Int, cmpBits: Int) extends Module { // wordBits: Int
val numEls = size

  val io = IO(new Bundle {
    val blockValid = Input(Bool())
    val block = Input(UInt(64.W))
    val downstreamReady = Input(Bool())
    val thisReady = Output(Bool())
    val outValid = Output(Bool())
    val out = Output(UInt(64.W))
  })

  val regs = Reg(init = Vec.do_fill(size) { ((1L << cmpBits) - 1).asUInt(64.W) })
  val fillCounter = Reg(init = 0.asUInt(util.log2Up(size + 1).W))
  val drainCounter = Reg(init = 0.asUInt(util.log2Up(size).W))

  val moreSpace = Wire(Bool())
  moreSpace := fillCounter < size.U
  io.thisReady := moreSpace
  io.outValid := !moreSpace

  val right = Wire(Bool())
  val left = Wire(Bool())
  right := moreSpace && io.blockValid
  left := !moreSpace && io.downstreamReady

  io.out := regs(0)

  when (right) {
    fillCounter := fillCounter + 1.U
  }

  def r(i: Int): UInt = {
    if (i == -1) io.block(cmpBits - 1, 0) else regs(i)(cmpBits - 1, 0)
  }

  for (i <- 0 until size) {
    if (i == 0) {
      regs(i) :=
        Mux(right,
          Mux(r(-1) > r(i), regs(i), io.block),
          Mux(left,
            Mux(r(i + 1) > r(i + 2), regs(i + 2), regs(i + 1)),
            regs(i)))
    } else if (i == 1) {
      regs(i) :=
        Mux(right,
          Mux(r(-1) > r(i - 1),
            io.block,
            Mux(r(i - 1) > r(i), regs(i), regs(i - 1))),
          Mux(left,
            Mux(r(i + 1) > r(i + 2),
              regs(i + 2),
              Mux(r(i) > r(i + 1), regs(i), regs(i + 1))),
            regs(i)))
    } else if (i == size - 2) {
      regs(i) :=
        Mux(right,
          Mux(r(i - 2) > r(i - 1),
            regs(i - 2),
            Mux(r(i - 1) > r(i), regs(i), regs(i - 1))),
          Mux(left,
            Mux(r(i) > r(i + 1), regs(i), regs(i + 1)),
            regs(i)))
    } else if (i == size - 1) {
      regs(i) :=
        Mux(right,
          Mux(r(i - 2) > r(i - 1), regs(i - 2), regs(i - 1)),
          Mux(left, ((1L << cmpBits) - 1).U, regs(i)))
    } else {
      regs(i) :=
        Mux(right,
          Mux(r(i - 2) > r(i - 1),
            regs(i - 2),
            Mux(r(i - 1) > r(i), regs(i), regs(i - 1))),
          Mux(left,
            Mux(r(i + 1) > r(i + 2), regs(i + 2),
              Mux(r(i) > r(i + 1), regs(i), regs(i + 1))),
            regs(i)))
    }
  }

  when (left) {
    when (drainCounter === (size - 1).U) {
      drainCounter := 0.U
      fillCounter := 0.U
    } .otherwise {
      drainCounter := drainCounter + 1.U
    }
  }

}
