package examples

import scala.collection.mutable.ArrayBuffer

object Util {
  def charsToBits(chars: Array[Char], bramLineSize: Int): (Int, Array[BigInt]) = { // number of bits, bits
    val buf = new ArrayBuffer[BigInt]
    val bytesInLine = bramLineSize / 8
    for (i <- 0 until chars.length by bytesInLine) {
      var result = BigInt(0)
      for (j <- i until Math.min(chars.length, i + bytesInLine)) {
        result = (BigInt(chars(j)) << ((j - i) * 8)) | result
      }
      buf.append(result)
    }
    (chars.length * 8, buf.toArray)
  }

  def bitsToChars(numBits: Int, bits: Array[BigInt], bramLineSize: Int): Array[Char] = {
    assert(numBits % 8 == 0)
    val bytesInLine = bramLineSize / 8
    val buf = new ArrayBuffer[Char]
    val mask = BigInt(255)
    val numBytes = numBits / 8
    var bytesRead = 0
    for (b <- bits) {
      for (i <- 0 until Math.min(bytesInLine, numBytes - bytesRead)) {
        buf.append(((b >> (i * 8)) & mask).toChar)
        bytesRead += 1
      }
    }
    buf.toArray
  }
}
