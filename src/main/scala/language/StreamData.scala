package language

sealed abstract class StreamData(width: Int)

sealed abstract class AssignableStreamData(width: Int) extends StreamData(width)

case class StreamWire(width: Int) extends AssignableStreamData(width)

case class StreamReg(width: Int) extends AssignableStreamData(width) {
  val stateId = Builder.registerState()
}
case class StreamBRAM(width: Int, numEls: Int) {
  val stateId = Builder.registerState()
}

case class StreamVector(el: StreamData, numEls: Int)

sealed abstract class StreamBits(width: Int) extends StreamData(width) {
  def apply(upper: Int, lower: Int) = BitSelect(this, upper, lower)
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
