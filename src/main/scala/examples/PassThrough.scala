package examples

import chisel3._

class PassThrough(wordSize: Int) extends Module {
  val io = IO(new ProcessingUnitIO(wordSize))

  io.outputWord := io.inputWord
  io.outputValid := io.inputValid
}
