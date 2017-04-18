package examples

import chisel3.iotesters.PeekPokeTester

class SortingNetworkTests(c: SortingNetwork) extends PeekPokeTester(c) {
  for (i <- 0 until c.size) {
    poke(c.io.blockValid, true)
    poke(c.io.block, i)
    poke(c.io.downstreamReady, true)
    expect(c.io.thisReady, true)
    step(1)
  }
  expect(c.io.thisReady, false)
  for (i <- 0 until c.networkDepth - 1) {
    poke(c.io.blockValid, false)
    poke(c.io.downstreamReady, true)
    step(1)
    expect(c.io.thisReady, true)
  }
  expect(c.io.outValid, true)
  for (i <- 0 until c.size) {
    expect(c.io.out(i), i)
  }
}
