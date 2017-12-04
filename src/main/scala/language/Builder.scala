package language

import scala.collection.mutable.ArrayBuilder

object Builder {
  val defaultContext = new StreamWhenContext(StreamTrue, null, {})

  var context = defaultContext
  var stateCounter = 0
  var statements = new ArrayBuilder[()]

  def startContext(c: StreamWhenContext): Unit = {
    context = c
  }

  def endContext(): Unit = {
    context = defaultContext
  }

  def registerState(): Int = {
    require(context == defaultContext, "circuit state cannot be declared inside a conditional")
    val retVal = stateCounter
    stateCounter += 1
    retVal
  }
}
