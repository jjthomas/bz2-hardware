// See LICENSE.txt for license details.
package examples

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
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
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 16, 32, 8, (coreId: Int) => new PassThrough(8, coreId)),
          backendName) {
          (c) => {
            val inputs = (0 until 4).map(i => Util.charsToBits((0 until 63).map(j => (i + j).toChar).toArray,
              512)).toArray
            new StreamingWrapperTests(c, inputs, inputs)
          }
        }
      },
      "StreamingWrapper2" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 16, 32, 8, (coreId: Int) => new PassThrough(8, coreId)),
          backendName) {
          (c) => {
            val inputs = (0 until 4).map(i => Util.charsToBits((0 until 65).map(j => (i + j).toChar).toArray,
              512)).toArray
            new StreamingWrapperTests(c, inputs, inputs)
          }
        }
      },
      "StreamingWrapper3" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(2, Array(0L, 0L), 2, Array(1000000000L, 1000000000L),
          8, 2, 2, 16, 32, 8, (coreId: Int) => new PassThrough(8, coreId)),
          backendName) {
          (c) => {
            val inputs = (0 until 8).map(i => Util.charsToBits((0 until 67).map(j => (i + j).toChar).toArray,
              512)).toArray
            new StreamingWrapperTests(c, inputs, inputs)
          }
        }
      },
      "StreamingWrapper4" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 16, 32, 16, (coreId: Int) => new PassThrough(16, coreId)),
          backendName) {
          (c) => {
            val inputs = (0 until 4).map(i => Util.charsToBits((0 until 62).map(j => (i + j).toChar).toArray,
              512)).toArray
            new StreamingWrapperTests(c, inputs, inputs)
          }
        }
      },
      "StreamingWrapper5" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 32, 32, 8, (coreId: Int) => new PassThrough(8, coreId)),
          backendName) {
          (c) => {
            val inputs = (0 until 4).map(i => Util.charsToBits((0 until 128).map(j => (i + j).toChar).toArray,
              1024)).toArray
            new StreamingWrapperTests(c, inputs, inputs)
          }
        }
      },
      "StreamingWrapper6" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 16, 32, 8, (coreId: Int) => new CsvFieldExtractor(2, 0, coreId)),
          backendName) {
          (c) => {
            val inputs = Array(Util.charsToBits("1111,21112".toCharArray, 512),
              Util.charsToBits("1,21112".toCharArray, 512), Util.charsToBits("111,21112".toCharArray, 512),
              Util.charsToBits("11,21112".toCharArray, 512))
            val outputs = Array(Util.charsToBits("1111".toCharArray, 512), Util.charsToBits("1".toCharArray, 512),
              Util.charsToBits("111".toCharArray, 512), Util.charsToBits("11".toCharArray, 512))
            new StreamingWrapperTests(c, inputs, outputs)
          }
        }
      }
  )
  def main(args: Array[String]): Unit = {
    TutorialRunner(examples, args)
  }
}

