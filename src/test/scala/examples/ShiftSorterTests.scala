package examples

import chisel3.iotesters.PeekPokeTester

class ShiftSorterTests(c: ParallelShiftSorter) extends PeekPokeTester(c) {
  poke(c.io.downstreamReady, true)
  poke(c.io.blockValid, true)
  for (i <- 0 until c.numEls) {
    poke(c.io.block, SorterTestUtils.genField((0 until c.ioElsPub).map(_ => i).toArray, c.wordBitsPub))
    expect(c.io.thisReady, true)
    step(1)
  }
  expect(c.io.thisReady, false)
  poke(c.io.blockValid, false)
  for (i <- 0 until c.numEls) {
    expect(c.io.outValid, true)
    expect(c.io.out, SorterTestUtils.genField((0 until c.ioElsPub).map(_ => i).toArray, c.wordBitsPub))
    step(1)
  }
  expect(c.io.outValid, false)
  expect(c.io.thisReady, true)
}
