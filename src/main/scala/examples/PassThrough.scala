package examples

import language.{Builder, StreamInput, Emit}

class PassThrough(wordSize: Int, coreId: Int) extends ProcessingUnit(wordSize, coreId) {
  Emit(0, StreamInput(0))
  Builder.curBuilder.compile()
}
