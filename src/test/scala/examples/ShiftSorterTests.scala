package examples

import chisel3.iotesters.PeekPokeTester

class ShiftSorterTests(c: ShiftSorter) extends PeekPokeTester(c) {
  poke(c.io.downstreamReady, true)
  poke(c.io.blockValid, true)
  for (i <- 0 until c.numEls) {
    poke(c.io.block, i)
    expect(c.io.thisReady, true)
    step(1)
  }
  expect(c.io.thisReady, false)
  poke(c.io.blockValid, false)
  step(c.numEls - 1)
  for (i <- 0 until c.numEls) {
    expect(c.io.outValid, true)
    expect(c.io.out, i)
    step(1)
  }
  expect(c.io.outValid, false)
  expect(c.io.thisReady, true)
}
