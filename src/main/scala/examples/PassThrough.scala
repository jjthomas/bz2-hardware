package examples

import language.{onInput, Builder, StreamInput, Emit}

class PassThrough(wordSize: Int, coreId: Int) extends ProcessingUnit(wordSize, coreId) {
  onInput {
    Emit(StreamInput)
  }
  Builder.curBuilder.compile()
}
