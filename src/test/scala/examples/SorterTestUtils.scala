package examples

object SorterTestUtils {
  def genField(vals: Array[Int]): BigInt = {
    var field = BigInt(0)
    for (i <- vals) {
      field <<= 64
      field |= i
    }
    field
  }
}
