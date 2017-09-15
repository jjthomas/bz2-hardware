package examples

import chisel3._
import chisel3.core.{Bundle, Module}

// 0 must be an unused min value
// size must be greater than 1
class ShiftSorter(size: Int, cmpBits: Int, treeSelector: Boolean) extends Module { // wordBits: Int
  val numEls = size
  assert(util.isPow2(size))

  val io = IO(new Bundle {
    val blockValid = Input(Bool())
    val block = Input(UInt(64.W))
    val downstreamReady = Input(Bool())
    val thisReady = Output(Bool())
    val outValid = Output(Bool())
    val out = Output(UInt(64.W))
  })

  val regs = RegInit(VecInit((0 until size).map(_ => 0.asUInt(64.W))))
  val fillCounter = RegInit(0.asUInt(util.log2Up(size + 1).W))
  val waitToggle = RegInit(false.B)
  val drainCounter = RegInit(0.asUInt(util.log2Up(size).W))

  val moreSpace = Wire(Bool())
  moreSpace := fillCounter < size.U
  io.thisReady := moreSpace

  val canOut = Wire(Bool())
  canOut := waitToggle && io.downstreamReady
  io.outValid := waitToggle

  def selectRegs(start: Int, end: Int): UInt = {
    val mid = (start + end) / 2
    val base = (end - start) == 2
    Mux(drainCounter(util.log2Up(end - start) - 1), if (base) regs(start + 1) else selectRegs(mid, end),
      if (base) regs(start) else selectRegs(start, mid))
  }
  if (treeSelector) {
    io.out := selectRegs(0, size)
  } else {
    io.out := regs(drainCounter)
  }

  when (moreSpace && io.blockValid) {
    fillCounter := fillCounter + 1.U
    regs((size - 1).U - fillCounter) := io.block
  }

  when (!moreSpace && !waitToggle) {
    waitToggle := true.B
  }

  for (i <- 0 until size - 1) {
    when (regs(i)(cmpBits - 1, 0) > regs(i + 1)(cmpBits - 1, 0)) {
      regs(i) := regs(i + 1)
      regs(i + 1) := regs(i)
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

  for (i <- 0 until size) {
    when (canOut && drainCounter === (size - 1).U) {
      regs(i) := 0.U
    }
  }

}
