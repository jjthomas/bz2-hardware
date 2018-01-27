package language

case class Assign(lhs: AssignableStreamData, rhs: StreamBits) {
  Builder.curBuilder.registerAssignment(this)
}

case class Emit(chan: Int, data: StreamBits*) {
  require(chan == 0, "multiple output channels currently not supported")
  Builder.curBuilder.registerEmit(this)
}