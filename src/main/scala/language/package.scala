import scala.language.implicitConversions

package object language {
  implicit def B(b: Boolean) = BigInt(if (b) { 1 } else { 0 })
  implicit def B(i: Int) = BigInt(i)
  implicit def B(l: Long) = BigInt(l)
  implicit class BooleanToLiteral(b: Boolean) {
    def L = Literal(b)
  }
  implicit class IntToLiteral(i: Int) {
    def L = Literal(i)
  }
  implicit class LongToLiteral(l: Long) {
    def L = Literal(l)
  }
  implicit class BigIntToLiteral(b: BigInt) {
    def L = Literal(b)
  }
}
