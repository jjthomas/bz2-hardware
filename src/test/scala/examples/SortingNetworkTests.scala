package examples

import chisel3.iotesters.PeekPokeTester

class SortingNetworkTests(c: SortingNetwork) extends PeekPokeTester(c) {
  poke(c.io.reset, false)
  poke(c.io.downstreamReady, true)
  poke(c.io.blockValid, true)
  for (i <- 0 until c.size) {
    poke(c.io.block, i)
    expect(c.io.thisReady, true)
    step(1)
  }
  expect(c.io.thisReady, false)
  poke(c.io.blockValid, false)
  for (i <- 0 until c.networkDepth - 1) {
    step(1)
    expect(c.io.thisReady, true)
  }
  for (i <- 0 until c.size) {
    expect(c.io.outValid, true)
    expect(c.io.out, i)
    step(1)
  }
  expect(c.io.outValid, false)
}
