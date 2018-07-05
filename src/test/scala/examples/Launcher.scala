// See LICENSE.txt for license details.
package examples

import java.io.File

import chisel3.iotesters.Driver
import language.Builder
import utils.TutorialRunner

object Launcher {
  def runStreamingTest(c: StreamingWrapper, inputs: Array[(Int, BigInt)],
                       outputs: Array[(Int, BigInt)]): StreamingWrapperTests = {
    for (((numInputBits, inputBits), (expectedNumOutputBits, expectedOutputBits)) <- inputs.zip(outputs)) {
      val (numOutputBits, outputBits) = Builder.curBuilder.simulate(numInputBits, inputBits)
      assert(numOutputBits == expectedNumOutputBits)
      assert(outputBits == expectedOutputBits)
    }
    new StreamingWrapperTests(c, inputs, outputs)
  }
  def runStreamingTest(c: StreamingWrapper, inputs: Array[String], outputs: Array[String]): StreamingWrapperTests = {
    val bitInputs = inputs.map(i => Util.charsToBits(i.toCharArray))
    val bitOutputs = outputs.map(o => Util.charsToBits(o.toCharArray))
    runStreamingTest(c, bitInputs, bitOutputs)
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
          1000000000L), 4, 1, 1, 1, 16, 32, (coreId: Int) => new PassThrough(8, coreId)),
          backendName) {
          (c) => {
            val inputs = (0 until 4).map(i => String.valueOf((0 until 63).map(j => (i + j).toChar))).toArray
            runStreamingTest(c, inputs, inputs)
          }
        }
      },
      "StreamingWrapper2" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 1, 16, 32, (coreId: Int) => new PassThrough(8, coreId)),
          backendName) {
          (c) => {
            val inputs = (0 until 4).map(i => String.valueOf((0 until 65).map(j => (i + j).toChar))).toArray
            runStreamingTest(c, inputs, inputs)
          }
        }
      },
      "StreamingWrapper3" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(2, Array(0L, 0L), 2, Array(1000000000L, 1000000000L),
          12, 2, 2, 2, 16, 32, (coreId: Int) => new PassThrough(8, coreId)), // 12 cores so that each channel does
          // not have a power of 2
          backendName) {
          (c) => {
            val inputs = (0 until 12).map(i => String.valueOf((0 until 67).map(j => (i + j).toChar))).toArray
            runStreamingTest(c, inputs, inputs)
          }
        }
      },
      "StreamingWrapper4" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 1, 16, 32, (coreId: Int) => new PassThrough(16, coreId)),
          backendName) {
          (c) => {
            val inputs = (0 until 4).map(i => String.valueOf((0 until 62).map(j => (i + j).toChar))).toArray
            runStreamingTest(c, inputs, inputs)
          }
        }
      },
      "StreamingWrapper5" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 1, 32, 32, (coreId: Int) => new PassThrough(8, coreId)),
          backendName) {
          (c) => {
            val inputs = (0 until 4).map(i => String.valueOf((0 until 128).map(j => (i + j).toChar))).toArray
            Builder.curBuilder.genCSim(new File("pass_through.c"), false)
            runStreamingTest(c, inputs, inputs)
          }
        }
      },
      "StreamingWrapper6" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 1, 16, 32, (coreId: Int) => new CsvFieldExtractor(2, 0, coreId)),
          backendName) {
          (c) => {
            val inputs = Array("1111\"2,2\",21112\n1,2", "1,21112", "111,21112", "11,21112")
            val outputs = Array("1111\"2,2\",1,", "1,", "111,", "11,")
            Builder.curBuilder.genCSim(new File("csv_field_extractor.c"), false)
            runStreamingTest(c, inputs, outputs)
          }
        }
      },
      "StreamingWrapper7" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 1, 16, 32, (coreId: Int) =>
          new JsonFieldExtractorSpecific(Array(Array("a", "b"), Array("a", "d"), Array("x")), 2, coreId)),
          backendName) {
          (c) => {
            val longNum = String.valueOf((0 until 60).map(_ => '4').toArray) // ensure input exceeds a BRAM line
            val inputs = Array(s"""{"a":{"b":1,"c":2,"d":3},"x":$longNum}""", """{"b":{"x":5},"x":4,"a":3}""",
              "{}", "{}")
            val matchStrs = JsonFieldExtractor.genFieldMatchStrs(
              Array(Array("a", "b"), Array("a", "d"), Array("x")), -1)
            val outputs = Array(s"${matchStrs(0)}1,${matchStrs(1)}3,${matchStrs(2)}$longNum/",
              s"${matchStrs(2)}4,/", "/", "/")
            Builder.curBuilder.genCSim(new File("json_field_extractor_specific.c"), false)
            runStreamingTest(c, inputs, outputs)
          }
        }
      },
      "StreamingWrapper8" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 1, 16, 32, (coreId: Int) =>
          new JsonFieldExtractorGeneric(100, 3, 2, coreId)), backendName) {
          (c) => {
            val fields = Array(Array("a", "b"), Array("a", "d"), Array("x"))
            val (numConfigBits, configBits) = JsonFieldExtractor.genConfigBits(fields, 100)
            val longNum = String.valueOf((0 until 60).map(_ => '4').toArray) // ensure input exceeds a BRAM line
            val inputs = Array(s"""{"a":{"b":1,"c":2,"d":3},"x":$longNum}""", """{"b":{"x":5},"x":4,"a":3}""",
              "{}", "{}").map(str => {
              val (numStrBits, strBits) = Util.charsToBits(str.toCharArray)
              (numConfigBits + numStrBits, (strBits << numConfigBits) | configBits)
            })
            val matchStrs = JsonFieldExtractor.genFieldMatchStrs(fields, 100)
            val outputs = Array(s"${matchStrs(0)}1,${matchStrs(1)}3,${matchStrs(2)}$longNum/",
              s"${matchStrs(2)}4,/", "/", "/").map(str => Util.charsToBits(str.toCharArray))
            Builder.curBuilder.genCSim(new File("json_field_extractor_generic.c"), false)
            runStreamingTest(c, inputs, outputs)
          }
        }
      },
      "StreamingWrapper9" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 1, 32, 16, (coreId: Int) =>
          new IntegerCoder(32, 4, Array(1, 6, 11, 16), coreId)), backendName) {
          (c) => {
            val seed = 2956547051745311985L
            val input = IntegerCoder.genRandomWords(32, 16, seed)
            // Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 16, 20, 32)
            val output = IntegerCoder.runCoder(input._1, input._2, 32, 4, Array(1, 6, 11, 16))
            Builder.curBuilder.genCSim(new File("integer_coder.c"), false)
            runStreamingTest(c, (0 until 4).map(_ => input).toArray, (0 until 4).map(_ => output).toArray)
          }
        }
      },
      "StreamingWrapper10" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 1, 32, 16, (coreId: Int) =>
          new GBDT(100, 25, 1000, 4, coreId: Int)), backendName) {
          (c) => {
            val seed = 2956547051745311985L
            val (input, output) = GBDT.genInputAndOutput(2, 25, 7, 2, 2, seed)
            Builder.curBuilder.genCSim(new File("gbdt.c"), false)
            runStreamingTest(c, (0 until 4).map(_ => input).toArray, (0 until 4).map(_ => output).toArray)
          }
        }
      },
      "StreamingWrapper11" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 1, 32, 16, (coreId: Int) =>
          new SmithWaterman(4, 1, coreId)),
          backendName) {
          (c) => {
            val input = Util.charsToBits(s"time${2.toChar}tme".toCharArray)
            val inputs = Array(input, input, input, input)
            val output = (32, BigInt(3))
            val outputs = Array(output, output, output, output)
            Builder.curBuilder.genCSim(new File("sw.c"), false)
            runStreamingTest(c, inputs, outputs)
          }
        }
      },
      "StreamingWrapper12" -> { (backendName: String) =>
        Driver(() => new StreamingWrapper(4, Array(0L, 0L, 0L, 0L), 4, Array(1000000000L, 1000000000L, 1000000000L,
          1000000000L), 4, 1, 1, 1, 32, 16, (coreId: Int) => {
            /*
            // email regex
            val addr = Regex.RangeList(Seq(('0', '9'), ('a', 'z'), ('-', '-'), ('+', '+')))
            val addrPlus = Regex.And(addr.dup(), Regex.Star(addr.dup()))
            val dom = Regex.RangeList(Seq(('0', '9'), ('a', 'z'), ('-', '-')))
            val domPlus = Regex.And(dom.dup(), Regex.Star(dom.dup()))
            val at = Regex.RangeList(Seq(('@', '@')))
            val dot = Regex.RangeList(Seq(('.', '.')))
            new Regex(
              Regex.And(addrPlus, Regex.And(at, Regex.And(domPlus.dup(), Regex.And(dot, domPlus.dup())))),
              coreId
            )
            */
            new Regex( // a+b+
              Regex.And(
                Regex.And(Regex.RangeList(Seq(('a', 'a'))), Regex.Star(Regex.RangeList(Seq(('a', 'a'))))),
                Regex.And(Regex.RangeList(Seq(('b', 'b'))), Regex.Star(Regex.RangeList(Seq(('b', 'b')))))
              ),
              coreId
            )
          }),
          backendName) {
          (c) => {
            val input = Util.charsToBits(s"xaab".toCharArray) // need initialization character 'x'
            val inputs = Array(input, input, input, input)
            val output = (32, BigInt(3))
            val outputs = Array(output, output, output, output)
            Builder.curBuilder.genCSim(new File("regex.c"), false)
            runStreamingTest(c, inputs, outputs)
          }
        }
      }
  )
  def main(args: Array[String]): Unit = {
    TutorialRunner(examples, args)
  }
}

