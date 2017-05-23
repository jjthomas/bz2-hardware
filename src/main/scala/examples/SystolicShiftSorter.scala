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

  val regs = Reg(init = Vec.do_fill(size) { ((1 << cmpBits) - 1).asUInt(64.W) })
  val fillCounter = Reg(init = 0.asUInt(util.log2Up(size + 1).W))
  val waitToggle = Reg(init = false.B)
  val drainCounter = Reg(init = 0.asUInt(util.log2Up(size).W))

  val moreSpace = Wire(Bool())
  moreSpace := fillCounter < size.U
  io.thisReady := moreSpace

  val canOut = Wire(Bool())
  canOut := waitToggle && io.downstreamReady
  io.outValid := waitToggle

  io.out := regs(0)

  when (moreSpace && io.blockValid) {
    fillCounter := fillCounter + 1.U
    regs(0) := io.block
  }

  when (!moreSpace && !waitToggle) {
    waitToggle := true.B
  }

  for (i <- 1 until size) {
    when (moreSpace && io.blockValid) {
      if (i == size - 1) {
        when (regs(i - 2)(cmpBits - 1, 0) > regs(i - 1)(cmpBits - 1, 0)) {
          regs(i) := regs(i - 2)
        }.otherwise {
          regs(i) := regs(i - 1)
        }
      } else if (i == 1) {
        when (regs(i - 1)(cmpBits - 1, 0) > regs(i)(cmpBits - 1, 0)) {
          regs(i) := regs(i)
        } .otherwise {
          regs(i) := regs(i - 1)
        }
      } else {
        when (regs(i - 2)(cmpBits - 1, 0) > regs(i - 1)(cmpBits - 1, 0)) {
          regs(i) := regs(i - 2)
        } .elsewhen (regs(i - 1)(cmpBits - 1, 0) > regs(i)(cmpBits - 1, 0)) {
          regs(i) := regs(i)
        } .otherwise {
          regs(i) := regs(i - 1)
        }
      }
    }
  }

  for (i <- 0 until size - 1) {
    when (canOut) {
      if (i == size - 2) {
        when (regs(i)(cmpBits - 1, 0) > regs(i + 1)(cmpBits - 1, 0)) {
          regs(i) := regs(i)
        }.otherwise {
          regs(i) := regs(i + 1)
        }
      } else if (i == 0) {
        when (regs(i + 1)(cmpBits - 1, 0) > regs(i + 2)(cmpBits - 1, 0)) {
          regs(i) := regs(i + 2)
        } .otherwise {
          regs(i) := regs(i + 1)
        }
      } else {
        when (regs(i)(cmpBits - 1, 0) > regs(i + 1)(cmpBits - 1, 0)) {
          regs(i) := regs(i)
        } .elsewhen (regs(i + 1)(cmpBits - 1, 0) > regs(i + 2)(cmpBits - 1, 0)) {
          regs(i) := regs(i + 2)
        } .otherwise {
          regs(i) := regs(i + 1)
        }
      }
    }
  }

  when (canOut) {
    regs(size - 1) := ((1 << cmpBits) - 1).U
  }

  for (i <- 0 until size - 1) {
    when (!moreSpace && !waitToggle) {
      when (regs(i)(cmpBits - 1, 0) > regs(i + 1)(cmpBits - 1, 0)) {
        regs(i) := regs(i + 1)
        regs(i + 1) := regs(i)
      }
    }
  }

  when (canOut) {
    when (drainCounter === (size - 1).U) {
      drainCounter := 0.U
      fillCounter := 0.U
      waitToggle := false.B
    } .otherwise {
      drainCounter := drainCounter + 1.U
    }
  }

}
