package examples

import chisel3.util
import language._

class CsvFieldExtractor(numFields: Int, targetField: Int, coreId: Int) extends ProcessingUnit(8, coreId) {
  val curField = StreamReg(util.log2Ceil(numFields), 0)
  val inQuote = StreamReg(1, false)
  val lastChar = StreamReg(8, ' '.toInt)

  onInput {
    swhen(StreamInput(0) === '"'.toInt.L) {
      swhen(!inQuote.B) {
        inQuote := true.L
      }.otherwise {
        inQuote := lastChar === '\\'.toInt.L
      }
    }
    swhen(StreamInput(0) === ','.toInt.L) {
      swhen(!inQuote.B) {
        curField := curField + 1.L
      }
    }.elsewhen(StreamInput(0) === '\n'.toInt.L) {
      swhen(!inQuote.B) {
        curField := 0.L
      }
    }
    lastChar := StreamInput(0)

    swhen(curField === targetField.L) {
      swhen(StreamInput(0) === ','.toInt.L && !inQuote.B) {
        Emit(0, ','.toInt.L)
      }.otherwise {
        Emit(0, StreamInput(0))
      }
    }
  }
  Builder.curBuilder.compile()
}
