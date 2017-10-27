package examples

class PassThrough(wordSize: Int) extends ProcessingUnit(wordSize) {
  io.outputWord := io.inputWord
  io.outputValid := io.inputValid

  override def produceOutput(numBits: Int, bits: Array[BigInt]): (Int, Array[BigInt]) = {
    (numBits, bits)
  }
}
