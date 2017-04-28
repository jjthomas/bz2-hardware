package examples

import chisel3.iotesters.PeekPokeTester

class InsertionSorterTests(c: InsertionSorter) extends PeekPokeTester(c) {
  poke(c.io.downstreamReady, true)
  poke(c.io.blockValid, true)
  for (i <- 0 until c.numEls) {
    poke(c.io.block, i)
    expect(c.io.thisReady, true)
    step(1)
  }
  poke(c.io.blockValid, false)
  for (i <- 0 until c.numEls) {
    expect(c.io.thisReady, false)
    expect(c.io.outValid, true)
    expect(c.io.out, i)
    step(1)
  }
  expect(c.io.outValid, false)
  expect(c.io.thisReady, true)
}