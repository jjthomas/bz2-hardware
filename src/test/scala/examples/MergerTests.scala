package examples

import chisel3.iotesters.PeekPokeTester

class MergerTests(c: Merger) extends PeekPokeTester(c) {
  poke(c.io.downstreamReady, true)
  poke(c.io.blockValid, true)
  for (i <- 0 until c.totalEls) {
    poke(c.io.block, i)
    expect(c.io.thisReady, true)
    step(1)
  }
  poke(c.io.blockValid, false)
  for (i <- 0 until c.totalEls) {
    expect(c.io.outValid, true)
    expect(c.io.out, i)
    expect(c.io.thisReady, false)
    step(1)
  }
  expect(c.io.outValid, false)
  expect(c.io.thisReady, true)
}
