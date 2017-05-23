package examples

import chisel3._
import chisel3.core.{Bundle, Module}
import chisel3.util.Cat

// 0 must be an unused min value
// size must be greater than 1
class ParallelShiftSorter(size: Int, ioEls: Int, cmpBits: Int, systolic: Boolean,
                          treeSelector: Boolean) extends Module { // wordBits: Int
  val ioElsPub = ioEls
  val numEls = size

  val io = IO(new Bundle {
    val blockValid = Input(Bool())
    val block = Input(UInt((64 * ioEls).W))
    val downstreamReady = Input(Bool())
    val thisReady = Output(Bool())
    val outValid = Output(Bool())
    val out = Output(UInt((64 * ioEls).W))
  })

  // TODO get rid of the code duplication here
  if (!systolic) {
    val sorters = new Array[ShiftSorter](ioEls)
    for (i <- 0 until ioEls) {
      sorters(i) = Module(new ShiftSorter(size, cmpBits, treeSelector))
      sorters(i).io.block := io.block((i + 1) * 64 - 1, i * 64)
      sorters(i).io.blockValid := io.blockValid
      sorters(i).io.downstreamReady := io.downstreamReady
    }

    io.out := Cat((ioEls - 1 to 0 by -1).map(i => sorters(i).io.out))
    io.outValid := sorters(0).io.outValid
    io.thisReady := sorters(0).io.thisReady
  } else {
    val sorters = new Array[SystolicShiftSorter](ioEls)
    for (i <- 0 until ioEls) {
      sorters(i) = Module(new SystolicShiftSorter(size, cmpBits))
      sorters(i).io.block := io.block((i + 1) * 64 - 1, i * 64)
      sorters(i).io.blockValid := io.blockValid
      sorters(i).io.downstreamReady := io.downstreamReady
    }

    io.out := Cat((ioEls - 1 to 0 by -1).map(i => sorters(i).io.out))
    io.outValid := sorters(0).io.outValid
    io.thisReady := sorters(0).io.thisReady
  }
}
