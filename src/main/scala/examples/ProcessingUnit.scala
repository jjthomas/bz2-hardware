package examples

import chisel3._
import chisel3.core.Bundle
import language.Builder

class ProcessingUnitIO(wordSize: Int) extends Bundle {
  val inputWord = Input(UInt(wordSize.W))
  val inputValid = Input(Bool())
  val inputFinished = Input(Bool()) // asserted on all cycles after the last valid input word
  val inputReady = Output(Bool())
  val outputWord = Output(UInt(wordSize.W))
  val outputValid = Output(Bool())
  val outputFinished = Output(Bool()) // asserted on all cycles after the last valid output word
  val outputReady = Input(Bool())

  override def cloneType(): this.type = new ProcessingUnitIO(wordSize).asInstanceOf[this.type]
}
abstract class ProcessingUnit(wordSize: Int) extends Module {
  val io = IO(new ProcessingUnitIO(wordSize))
  Builder.nextBuilder(wordSize, wordSize, io)
}
