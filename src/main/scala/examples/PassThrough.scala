package examples

import language.{onInput, Builder, StreamInput, Emit}

class PassThrough(wordSize: Int, coreId: Int) extends ProcessingUnit(wordSize, coreId) {
  onInput {
    Emit(0, StreamInput(0))
  }
  Builder.curBuilder.compile()
}
