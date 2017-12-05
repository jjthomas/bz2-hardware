package language

sealed abstract class StreamBits(width: Int) {
  def getWidth = width
  def apply(upper: Int, lower: Int) = BitSelect(this, upper, lower)
}

case class StreamInput(chan: Int) extends StreamBits(Builder.curBuilder.inputWidth) {
  require(chan == 0, "multiple input channels currently not supported")
}

case class BitSelect(arg: StreamBits, upper: Int, lower: Int) extends StreamBits(upper - lower + 1)

sealed abstract class StreamBool extends StreamBits(1) {
  def unary_! = Negate(this)
  def &&(that: StreamBool) = And(this, that)
}

case object StreamTrue extends StreamBool
case object StreamFalse extends StreamBool

case class Negate(arg: StreamBool) extends StreamBool
case class And(arg1: StreamBool, arg2: StreamBool) extends StreamBool

sealed abstract class AssignableStreamData(width: Int) extends StreamBits(width) {
  def :=(rhs: StreamBits) = Assign(this, rhs)
}

case class StreamWire(width: Int) extends AssignableStreamData(width) {
  val stateId = Builder.curBuilder.registerAssignable(this)
}

case class StreamReg(width: Int) extends AssignableStreamData(width) {
  val stateId = Builder.curBuilder.registerAssignable(this)
}

case class StreamBRAM(val width: Int, numEls: Int) {
  val stateId = Builder.curBuilder.registerAssignable(this)

  def apply(idx: Int) = BRAMSelect(this, idx)
}

case class BRAMSelect(arg: StreamBRAM, idx: Int) extends AssignableStreamData(arg.width)

case class StreamVector(val el: AssignableStreamData, numEls: Int) {
  val stateId = Builder.curBuilder.registerAssignable(this)

  def apply(idx: Int) = VectorSelect(this, idx)
}

case class VectorSelect(arg: StreamVector, idx: Int) extends AssignableStreamData(arg.el.getWidth)
