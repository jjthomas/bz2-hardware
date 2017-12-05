package language

import examples.ProcessingUnitIO

import scala.collection.mutable.ArrayBuffer

object Builder {
  val defaultContext = new StreamWhenContext(StreamTrue, null, {})
  var curBuilder: Builder = null

  def nextBuilder(inputWidth: Int, outputWidth: Int, io: ProcessingUnitIO): Unit = {
    curBuilder = new Builder(inputWidth, outputWidth, io)
  }
}

class Builder(val inputWidth: Int, val outputWidth: Int, io: ProcessingUnitIO) {
  var context = Builder.defaultContext

  var assignables = new ArrayBuffer[Any]
  var assignments = new ArrayBuffer[(StreamWhenContext, Assign)]
  var emits = new ArrayBuffer[(StreamWhenContext, Emit)]

  def startContext(c: StreamWhenContext): Unit = {
    context = c
  }

  def endContext(): Unit = {
    context = Builder.defaultContext
  }

  def registerAssignable(assignable: Any): Int = {
    require(context == Builder.defaultContext, "assignable cannot be declared inside a conditional")
    val id = assignables.length
    assignables.append(assignable)
    id
  }

  def registerAssignment(assignment: Assign): Unit = {
    assignments.append((context, assignment))
  }

  def registerEmit(emit: Emit): Unit = {
    emits.append((context, emit))
  }

  def compile(): Unit = {

  }

}
