package examples

object SorterTestUtils {
  def genField(vals: Array[Int], wordSize: Int): BigInt = {
    var field = BigInt(0)
    for (i <- vals) {
      field <<= wordSize
      field |= i
    }
    field
  }
}
