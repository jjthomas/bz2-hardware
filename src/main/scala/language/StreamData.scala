package language

sealed abstract class StreamBits(width: Int) extends Product {
  def getWidth = width
  def apply(upper: Int, lower: Int) = BitSelect(this, upper, lower)
  def +(other: StreamBits) = Add(this, other)
  def -(other: StreamBits) = Subtract(this, other)
  def ===(other: StreamBits) = Equal(this, other)
  def =/=(other: StreamBits) = NotEqual(this, other)
  def <(other: StreamBits) = LessThan(this, other)
  def >(other: StreamBits) = GreaterThan(this, other)
  def <=(other: StreamBits) = LessThanEqual(this, other)
  def >=(other: StreamBits) = GreaterThanEqual(this, other)
  def ##(other: StreamBits) = Concat(this, other)
  def B = BoolCast(this)
}

case class Add(first: StreamBits, second: StreamBits) extends StreamBits(math.max(first.getWidth, second.getWidth))
case class Subtract(first: StreamBits, second: StreamBits) extends StreamBits(math.max(first.getWidth, second.getWidth))
case class Concat(first: StreamBits, second: StreamBits) extends StreamBits(first.getWidth + second.getWidth)
case class StreamMux(cond: StreamBool, t: StreamBits, f: StreamBits)
  extends StreamBits(math.max(t.getWidth, f.getWidth))

case class Equal(first: StreamBits, second: StreamBits) extends StreamBool
case class NotEqual(first: StreamBits, second: StreamBits) extends StreamBool
case class LessThan(first: StreamBits, second: StreamBits) extends StreamBool
case class GreaterThan(first: StreamBits, second: StreamBits) extends StreamBool
case class LessThanEqual(first: StreamBits, second: StreamBits) extends StreamBool
case class GreaterThanEqual(first: StreamBits, second: StreamBits) extends StreamBool

case class Literal(l: BigInt) extends StreamBits(l.bitLength)

case class StreamInput(chan: Int) extends StreamBits(Builder.curBuilder.inputWidth) {
  require(chan == 0, "multiple input channels currently not supported")
}

case class BitSelect(arg: StreamBits, upper: Int, lower: Int) extends StreamBits(upper - lower + 1)

sealed abstract class StreamBool extends StreamBits(1) {
  def unary_! = Negate(this)
  def &&(that: StreamBool) = And(this, that)
  def ||(that: StreamBool) = Or(this, that)
}

case class Negate(arg: StreamBool) extends StreamBool
case class And(arg1: StreamBool, arg2: StreamBool) extends StreamBool
case class Or(arg1: StreamBool, arg2: StreamBool) extends StreamBool
case class BoolCast(arg: StreamBits) extends StreamBool {
  require(arg.getWidth == 1)
}

sealed abstract class AssignableStreamData(width: Int) extends StreamBits(width) {
  def :=(rhs: StreamBits) = Assign(this, rhs)
}

case class StreamWire(width: Int) extends AssignableStreamData(width) {
  val stateId = Builder.curBuilder.registerAssignable(this)
}

case class StreamReg(width: Int, init: BigInt) extends AssignableStreamData(width) {
  val stateId = Builder.curBuilder.registerAssignable(this)
}

case class StreamBRAM(width: Int, numEls: Int) {
  val stateId = Builder.curBuilder.registerAssignable(this)

  def apply(idx: StreamBits) = BRAMSelect(this, idx)
}

case class BRAMSelect(arg: StreamBRAM, idx: StreamBits) extends AssignableStreamData(arg.width)

case class StreamVectorReg(width: Int, numEls: Int, init: Seq[BigInt]) {
  require(init == null || init.size == numEls, "init must be null or have size equal to numEls")
  val stateId = Builder.curBuilder.registerAssignable(this)

  def apply(idx: StreamBits) = VectorRegSelect(this, idx)
}

case class StreamVectorWire(width: Int, numEls: Int) {
  val stateId = Builder.curBuilder.registerAssignable(this)

  def apply(idx: StreamBits) = VectorWireSelect(this, idx)
}

case class VectorRegSelect(arg: StreamVectorReg, idx: StreamBits) extends AssignableStreamData(arg.width)
case class VectorWireSelect(arg: StreamVectorWire, idx: StreamBits) extends AssignableStreamData(arg.width)
