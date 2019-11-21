// See LICENSE.txt for license details.
package examples

import chisel3.iotesters.Driver
import scala.collection.mutable.ArrayBuffer
import utils.TutorialRunner

object Launcher {
  val examples = Map(
      "Combinational" -> { (backendName: String) =>
        Driver(() => new Combinational(), backendName) {
          (c) => new CombinationalTests(c)
        }
      },
      "Functionality" -> { (backendName: String) =>
        Driver(() => new Functionality(), backendName) {
          (c) => new FunctionalityTests(c)
        }
      },
      "Parity" -> { (backendName: String) =>
        Driver(() => new Parity(), backendName) {
          (c) => new ParityTests(c)
        }
      },
      "Tbl" -> { (backendName: String) =>
        Driver(() => new Tbl(), backendName) {
          (c) => new TblTests(c)
        }
      },
      "Life" -> { (backendName: String) =>
        Driver(() => new Life(12), backendName) {
          (c) => new LifeTests(c)
        }
      },
      "Risc" -> { (backendName: String) =>
        Driver(() => new Risc(), backendName) {
          (c) => new RiscTests(c)
        }
      },
      "Darken" -> { (backendName: String) =>
        Driver(() => new Darken(), backendName) {
          (c) => new DarkenTests(c, getClass.getResourceAsStream("/in.im24"), "o" + "u,t.im24")
        }
      },
      "Adder" -> { (backendName: String) =>
        Driver(() => new Adder(8), backendName) {
          (c) => new AdderTests(c)
        }
      },
      "Adder4" -> { (backendName: String) =>
        Driver(() => new Adder4(), backendName) {
          (c) => new Adder4Tests(c)
        }
      },
      "SimpleALU" -> { (backendName: String) =>
        Driver(() => new SimpleALU(), backendName) {
          (c) => new SimpleALUTests(c)
        }
      },
      "FullAdder" -> { (backendName: String) =>
        Driver(() => new FullAdder(), backendName) {
          (c) => new FullAdderTests(c)
        }
      },
    "ByteSelector" -> { (backendName: String) =>
      Driver(() => new ByteSelector(), backendName) {
        (c) => new ByteSelectorTests(c)
      }
    },
    "GCD" -> { (backendName: String) =>
      Driver(() => new GCD, backendName) {
        (c) => new GCDTests(c)
      }
    },
      "HiLoMultiplier" -> { (backendName: String) =>
        Driver(() => new HiLoMultiplier(), backendName) {
          (c) => new HiLoMultiplierTests(c)
        }
      },
      "ShiftRegister" -> { (backendName: String) =>
        Driver(() => new ShiftRegister(), backendName) {
          (c) => new ShiftRegisterTests(c)
        }
      },
      "ResetShiftRegister" -> { (backendName: String) =>
        Driver(() => new ResetShiftRegister(), backendName) {
          (c) => new ResetShiftRegisterTests(c)
        }
      },
      "EnableShiftRegister" -> { (backendName: String) =>
        Driver(() => new EnableShiftRegister(), backendName) {
          (c) => new EnableShiftRegisterTests(c)
        }
      },
      "LogShifter" -> { (backendName: String) =>
        Driver(() => new LogShifter(), backendName) {
          (c) => new LogShifterTests(c)
        }
      },
      "VecSearch" -> { (backendName: String) =>
        Driver(() => new VecSearch(), backendName) {
          (c) => new VecSearchTests(c)
        }
      },
      "Stack" -> { (backendName: String) =>
        Driver(() => new Stack(8), backendName) {
          (c) => new StackTests(c)
        }
      },
      "SortingNetwork" -> { (backendName: String) =>
        Driver(() => new SortingNetwork(3, 8, 21), backendName) {
          (c) => new SortingNetworkTests(c)
        }
      },
      "Merger" -> { (backendName: String) =>
        Driver(() => new Merger(4, 4, 21), backendName) {
          (c) => new MergerTests(c)
        }
      },
      "ShiftSorter" -> { (backendName: String) =>
        Driver(() => new ParallelShiftSorter(512, 8, 64, 0, 40, true, false), backendName) {
          (c) => new ShiftSorterTests(c)
        }
      },
      "InsertionSorter" -> { (backendName: String) =>
        Driver(() => new InsertionSorter(32, 21), backendName) {
          (c) => new InsertionSorterTests(c)
        }
      },
      "StreamingWrapper1" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(0, 1000000000, 1,
          (coreId: Int) => new Summer(coreId)), backendName) {
          (c) => {
            new StreamingWrapperTests(c, Util.arrToBits(Array(0, 2, 10, 10), 32),
              Util.arrToBits(Array(20), 32))
          }
        }
      },
      "StreamingWrapper2" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(0, 1000000000, 2,
          (coreId: Int) => new Summer(coreId)), backendName) {
          (c) => {
            new StreamingWrapperTests(c, Util.arrToBits(Array(1, 10, 1, 20, 1, 10), 32),
              Util.arrToBits(Array(20, 30), 32))
          }
        }
      },
      "StreamingWrapper3" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(0, 1000000000, 1,
          (coreId: Int) => new Counter(coreId, 1)), backendName) {
          (c) => {
            new StreamingWrapperTests(c, Util.arrToBits(Array(1, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0), 8),
              Util.arrToBits(Array(2), 8))
          }
        }
      },
      "StreamingWrapper4" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(0, 1000000000, 2,
          (coreId: Int) => new Counter(coreId, 2)), backendName) {
          (c) => {
            new StreamingWrapperTests(c, Util.arrToBits(Array(2, 0, 0, 0, 0, 1, 2, 0, 0, 0, 2, 3, 2, 0, 0, 0, 0, 1), 8),
              Util.arrToBits(Array(1, 2, 3, 4), 8))
          }
        }
      },
      "StreamingWrapper5" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(0, 1000000000, 3,
          (coreId: Int) => new Counter(coreId, 30)), backendName) {
          (c) => {
            val input = Array(6, 0, 0, 0, 0, 0, 1, 1, 2, 2)
            val config = new ArrayBuffer[Int]
            val output = new ArrayBuffer[Int]
            for (i <- 0 until 3) {
              config.appendAll(Array(30, 0, 0, 0))
              for (j <- 0 until 30) {
                config.append(i)
                output.append(if (j < 3) i + 2 else i)
              }
            }
            new StreamingWrapperTests(c, Util.arrToBits(config.toArray ++ input, 8),
              Util.arrToBits(output.toArray, 8))
          }
        }
      }
  )
  def main(args: Array[String]): Unit = {
    TutorialRunner(examples, args)
  }
}

