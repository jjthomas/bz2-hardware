package language

case class Assign(lhs: AssignableStreamData, rhs: StreamBits) {
  Builder.curBuilder.registerAssignment(this)
}

case class Emit(data: StreamBits) {
  Builder.curBuilder.registerEmit(this)
}