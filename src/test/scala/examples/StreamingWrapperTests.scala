package examples

import chisel3.iotesters.PeekPokeTester

class StreamingWrapperTests(c: StreamingWrapper) extends PeekPokeTester(c) {
  poke(c.io.init, true)
  step(1)

}
