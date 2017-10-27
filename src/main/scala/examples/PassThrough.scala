package examples

class PassThrough(wordSize: Int, coreId: Int) extends ProcessingUnit(wordSize) {
  io.outputWord := io.inputWord
  io.outputValid := io.inputValid
}
