// See LICENSE.txt for license details.
package examples

import java.io.File

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import language.Builder
import utils.TutorialRunner

object Launcher {
  def runStreamingTest(c: StreamingWrapper, inputs: Array[String], outputs: Array[String]): StreamingWrapperTests = {
    val testInputs = inputs.map(o => Util.charsToBits(o.toCharArray, c.bramLineSize))
    for (((numInputBits, inputBits), expectedOutput) <- testInputs.zip(outputs)) {
      val output = Builder.curBuilder.simulate(numInputBits, c.bramLineSize, inputBits)
      assert(String.valueOf(Util.bitsToChars(output._1, output._2, output._3)) == expectedOutput)
    }
    val testOutputs = outputs.map(o => Util.charsToBits(o.toCharArray, c.bramLineSize))
    new StreamingWrapperTests(c, testInputs, testOutputs)
  }
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
            val inputs = (0 until 4).map(i => String.valueOf((0 until 63).map(j => (i + j).toChar))).toArray
            runStreamingTest(c, inputs, inputs)
          }
        }
      },
      "StreamingWrapper2" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 16, 32, 8, (coreId: Int) => new PassThrough(8, coreId)),
          backendName) {
          (c) => {
            val inputs = (0 until 4).map(i => String.valueOf((0 until 65).map(j => (i + j).toChar))).toArray
            runStreamingTest(c, inputs, inputs)
          }
        }
      },
      "StreamingWrapper3" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(2, Array(0L, 0L), 2, Array(1000000000L, 1000000000L),
          8, 2, 2, 16, 32, 8, (coreId: Int) => new PassThrough(8, coreId)),
          backendName) {
          (c) => {
            val inputs = (0 until 8).map(i => String.valueOf((0 until 67).map(j => (i + j).toChar))).toArray
            runStreamingTest(c, inputs, inputs)
          }
        }
      },
      "StreamingWrapper4" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 16, 32, 16, (coreId: Int) => new PassThrough(16, coreId)),
          backendName) {
          (c) => {
            val inputs = (0 until 4).map(i => String.valueOf((0 until 62).map(j => (i + j).toChar))).toArray
            runStreamingTest(c, inputs, inputs)
          }
        }
      },
      "StreamingWrapper5" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 32, 32, 8, (coreId: Int) => new PassThrough(8, coreId)),
          backendName) {
          (c) => {
            val inputs = (0 until 4).map(i => String.valueOf((0 until 128).map(j => (i + j).toChar))).toArray
            Builder.curBuilder.genCSim(new File("pass_through.c"))
            runStreamingTest(c, inputs, inputs)
          }
        }
      },
      "StreamingWrapper6" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 16, 32, 8, (coreId: Int) => new CsvFieldExtractor(2, 0, coreId)),
          backendName) {
          (c) => {
            val inputs = Array("1111\"2,2\",21112\n1,2", "1,21112", "111,21112", "11,21112")
            val outputs = Array("1111\"2,2\",1,", "1,", "111,", "11,")
            Builder.curBuilder.genCSim(new File("csv_field_extractor.c"))
            runStreamingTest(c, inputs, outputs)
          }
        }
      },
      "StreamingWrapper7" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 16, 32, 8, (coreId: Int) =>
          new JsonFieldExtractorSpecific(Array(Array("f1")), 1, coreId)),
          backendName) {
          (c) => {
            val inputs = Array("""{"f1":1}""", "{}", "{}", "{}")
            val outputs = Array("1,", "", "", "")
            Builder.curBuilder.genCSim(new File("json_field_extractor.c"))
            runStreamingTest(c, inputs, outputs)
          }
        }
      }
  )
  def main(args: Array[String]): Unit = {
    TutorialRunner(examples, args)
  }
}

