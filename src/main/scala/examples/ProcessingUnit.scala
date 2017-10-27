package examples

import chisel3._
import chisel3.core.Bundle

abstract class ProcessingUnit(wordSize: Int) extends Module {
  val io = IO(new Bundle {
    val inputWord = Input(UInt(wordSize.W))
    val lastInputWord = Input(Bool())
    val inputValid = Input(Bool())
    val outputWord = Output(UInt(wordSize.W))
    val outputValid = Output(Bool())
  })

  def produceOutput(numBits: Int, bits: Array[BigInt]): (Int, Array[BigInt])
}
