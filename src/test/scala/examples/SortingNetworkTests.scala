package examples

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

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

class SortingNetworkTester extends ChiselFlatSpec {
  behavior of "SortingNetwork"
  backends foreach {backend =>
    it should s"correctly sort already sorted numbers in $backend" in {
      Driver(() => new SortingNetwork(1, 64), backend)(c => new SortingNetworkTests(c)) should be (true)
    }
  }
}
