package examples

import chisel3._

import scala.collection.mutable.ArrayBuffer

class CsvFieldExtractor(numFields: Int, targetField: Int) extends ProcessingUnit(8) {

  def produceOutput(numBits: Int, bits: Array[BigInt]): (Int, Array[BigInt]) = {
    val input = Util.bitsToChars(numBits, bits)
    val output = new ArrayBuffer[Char]

    var curField = 0
    var inQuote = false
    var lastChar = ' '

    for (c <- input) {
      val inQuoteNext =
        if (c == '"') {
          if (!inQuote) {
            true
          } else {
            lastChar == '\\'
          }
        } else {
          inQuote
        }
      val curFieldNext =
        if (c == ',') {
          if (!inQuote) {
            curField + 1
          } else {
            curField
          }
        } else if (c == '\n') {
          if (!inQuote) {
            0
          } else {
            curField
          }
        } else {
          curField
        }
      val lastCharNext = c

      if (curField == targetField && curFieldNext == targetField) {
        output.append(c)
      }

      curField = curFieldNext
      inQuote = inQuoteNext
      lastChar = lastCharNext
    }
    Util.charsToBits(output.toArray)
  }

  // state
  val curField = RegInit(0.asUInt(util.log2Ceil(numFields).W))
  val inQuote = RegInit(false.B)
  val lastChar = RegInit(' '.toInt.asUInt(8.W))

  // new states
  val inQuoteNext = Wire(Bool())
  when (io.inputWord === '"'.toInt.U) {
    when (!inQuote) {
      inQuoteNext := true.B
    } .otherwise {
      inQuoteNext := lastChar === '\\'.toInt.U
    }
  } .otherwise {
    inQuoteNext := inQuote
  }

  val curFieldNext = Wire(UInt(util.log2Ceil(numFields).W))
  when (io.inputWord === ','.toInt.U) {
    when (!inQuote) {
      curFieldNext := curField + 1.U
    } .otherwise {
      curFieldNext := curField
    }
  } .elsewhen (io.inputWord === '\n'.toInt.U) {
    when (!inQuote) {
      curFieldNext := 0.U
    } .otherwise {
      curFieldNext := curField
    }
  } .otherwise {
    curFieldNext := curField
  }

  val lastCharNext = Wire(UInt(8.W))
  lastCharNext := io.inputWord

  // output
  io.outputWord := io.inputWord
  io.outputValid := curField === targetField.U && curFieldNext === targetField.U

  // commit new states
  inQuote := inQuoteNext
  curField := curFieldNext
  lastChar := lastCharNext
}
