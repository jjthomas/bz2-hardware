package examples

import language._

class Filter(coreId: Int) extends ProcessingUnit(8, 8, coreId) {
  onInput {
    swhen(StreamInput > 127.L) {
      Emit(StreamInput)
    }
  }
  Builder.curBuilder.compile()
}