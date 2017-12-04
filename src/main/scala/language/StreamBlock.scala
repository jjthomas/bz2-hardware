package language

object swhen {
  def apply(cond: StreamBool)(block: => Unit): StreamWhenContext = {
    new StreamWhenContext(cond, !cond, block)
  }
}

class StreamWhenContext(cond: StreamBool, prevCond: StreamBool, block: => Unit) {
  def elsewhen(elseCond: StreamBool)(block: => Unit): StreamWhenContext = {
    new StreamWhenContext(prevCond && elseCond, prevCond && !elseCond, block)
  }

  def otherwise(block: => Unit): Unit = {
    new StreamWhenContext(prevCond, null, block)
  }

  Builder.startContext(this)
  block
  Builder.endContext()
}
