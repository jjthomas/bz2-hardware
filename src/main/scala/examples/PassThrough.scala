package examples

import chisel3._
import chisel3.core.Bundle

class PassThrough(wordSize: Int) extends Module {
  val io = IO(new Bundle {
    val inputWord = Input(UInt(wordSize.W))
    val inputValid = Input(Bool())
    val outputWord = Output(UInt(wordSize.W))
    val outputValid = Output(Bool())
  })

  io.outputWord := io.inputWord
  io.outputValid := io.inputValid
}
