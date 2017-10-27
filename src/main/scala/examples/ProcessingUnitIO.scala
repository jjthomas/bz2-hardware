package examples

import chisel3.core.Bundle
import chisel3._

class ProcessingUnitIO(wordSize: Int) extends Bundle {
  val inputWord = Input(UInt(wordSize.W))
  val lastInputWord = Input(Bool())
  val inputValid = Input(Bool())
  val outputWord = Output(UInt(wordSize.W))
  val outputValid = Output(Bool())

  override def cloneType(): this.type = new ProcessingUnitIO(wordSize).asInstanceOf[this.type]
}
