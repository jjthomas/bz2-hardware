package examples

import chisel3.util
import language._

import scala.collection.mutable.ArrayBuffer

class IntegerCoder(wordSize: Int, batchWords: Int, coreId: Int) extends ProcessingUnit(8, coreId) {
  assert(wordSize % 8 == 0)
  assert(wordSize <= 64)
  val wordBytes = wordSize / 8
  val wordIdx = StreamReg(util.log2Ceil(batchWords + 1), 0)
  val byteIdx = StreamReg(Math.max(util.log2Ceil(wordBytes), 1), 0)
  val curWord = StreamReg(wordSize, 0)

  val maxVarIntBits = (wordSize + 7 - 1) / 7 * 8
  val bitWidths = Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, if (wordSize <= 32) 12 else 64, 13, 16, 20, 32)
  assert(util.isPow2(bitWidths.length))
  val maxWidth = bitWidths.max
  val idToBitWidth = StreamVectorReg(util.log2Ceil(maxWidth + 1), bitWidths.length, bitWidths.map(b => BigInt(b)))
  val bitCounts = bitWidths.map(_ => (StreamReg(util.log2Ceil(batchWords + 1), 0) /* number of words that fit */,
    StreamReg(util.log2Ceil(wordSize), wordSize - 1) /* min leading zeros in exceptions */,
    StreamReg(util.log2Ceil(maxVarIntBits + 1), 0) /* number of bits needed for varint encoding of
    exceptions */))
  val lzToVarIntBits = StreamVectorReg(util.log2Ceil(maxVarIntBits + 1), wordSize,
    (0 until wordSize).map(lz => BigInt(((wordSize - lz) + 7 - 1) / 7 * 8)))
  object CodingState extends Enumeration {
    type CodingState = Value
    val READ_INPUT, EMIT_MAIN, EMIT_EXCEPT = Value
  }
  import CodingState._
  val curState = StreamReg(util.log2Ceil(CodingState.maxId), 0)

  val outputWord = StreamReg(8, 0)
  val outputWordBits = StreamReg(3, 0)
  val wordSlice = StreamReg(util.log2Ceil(maxVarIntBits / 8), 0) // used when slicing word into chunks of 8 or 7

  val buffer = StreamBRAM(wordSize, batchWords)

  def leadingZeros(word: StreamBits): StreamBits = { // maximum return value is wordSize - 1
    assert(word.getWidth >= 4 && util.isPow2(word.getWidth))
    var curLevel = new ArrayBuffer[(StreamBool, StreamBits)] // (is all zeroes, num leading zeros)
    for (i <- 0 until word.getWidth by 4) {
      val curSlice = word(word.getWidth - i - 1, word.getWidth - i - 4)
      curLevel.append((curSlice === 0.L,
        StreamMux(curSlice(3, 3) === 1.L, 0.L, StreamMux(curSlice(2, 2) === 1.L, 1.L,
          StreamMux(curSlice(1, 1) === 1.L, 2.L, StreamMux(curSlice(0, 0) === 1.L, 3.L, 4.L))))))
    }
    while (curLevel.length > 1) {
      val nextLevel = new ArrayBuffer[(StreamBool, StreamBits)]
      for (i <- 0 until curLevel.length by 2) {
        nextLevel.append((curLevel(i)._1 && curLevel(i + 1)._1, StreamMux(curLevel(i)._1, curLevel(i)._2 +
          curLevel(i + 1)._2, curLevel(i)._2)))
      }
      curLevel = nextLevel
    }
    StreamMux(curLevel(0)._2 === word.getWidth.L, (word.getWidth - 1).L, curLevel(0)._2)
  }

  def shiftByConst(arg: StreamBits, const: Int): StreamBits = {
    if (const == 0) {
      arg
    } else {
      arg##0.L(const)
    }
  }

  def mulByConst(arg: StreamBits, const: Int): StreamBits = {
    assert(const >= 1)
    var result = shiftByConst(arg, util.log2Floor(const))
    var addsSoFar = 1 << util.log2Floor(const)
    while (addsSoFar < const) {
      result = result + shiftByConst(arg, util.log2Floor(const - addsSoFar))
      addsSoFar += 1 << util.log2Floor(const - addsSoFar)
    }
    result
  }

  def mulWithConstMethod(arg: StreamBits, smallArg: StreamBits, maxSmallArg: Int): StreamBits = {
    var cur: StreamBits = 0.L
    for (i <- 1 to maxSmallArg) {
      cur = StreamMux(smallArg === i.L, mulByConst(arg, i), cur)
    }
    cur
  }

  var bestWidths = new ArrayBuffer[(StreamBits, StreamBool, StreamBits)] // (cost in bits, is varint cheaper, width ID)
  for ((w, i) <- bitWidths.zipWithIndex) {
    val numExceptions = batchWords.L - bitCounts(i)._1
    val exceptionFixedCost = util.log2Ceil(wordSize).L + mulWithConstMethod(wordSize.L - bitCounts(i)._2,
      numExceptions, batchWords)
    bestWidths.append((mulWithConstMethod(w.L, bitCounts(i)._1, batchWords) /* main size */ +
      mulByConst(numExceptions, util.log2Ceil(batchWords)) + StreamMux(numExceptions > 0.L, 1.L, 0.L) /* exception
      metadata size (extra bit for encoding type) */ + StreamMux(bitCounts(i)._3 < exceptionFixedCost, bitCounts(i)._3,
      exceptionFixedCost) /* exception size */, bitCounts(i)._3 < exceptionFixedCost, i.L))
  }
  while (bestWidths.length > 1) {
    val nextWidths = new ArrayBuffer[(StreamBits, StreamBool, StreamBits)]
    for (i <- 0 until bestWidths.length by 2) {
      val firstLess = bestWidths(i)._1 < bestWidths(i + 1)._1
      nextWidths.append((StreamMux(firstLess, bestWidths(i)._1, bestWidths(i + 1)._1),
        StreamMux(firstLess, bestWidths(i)._2, bestWidths(i + 1)._2).B,
        StreamMux(firstLess, bestWidths(i)._3, bestWidths(i + 1)._3)))
    }
    bestWidths = nextWidths
  }
  val bestWidth = idToBitWidth(bestWidths(0)._3)
  val useVarInt = bestWidths(0)._2

  def addBitsToOutputWord(bits: StreamBits, topBit: StreamBits): Unit = { // topBit is highest valid bit in bits
    assert(bits.getWidth <= 8)
    var updatedOut = bits
    for (i <- 1 until 8) { // outputWordBits = 0 means updatedOut is just bits
      updatedOut = StreamMux(outputWordBits === i.L, bits(Math.min(bits.getWidth - 1, 7 - i), 0)##outputWord(i - 1, 0),
        updatedOut)
    }
    var bitsRemainder: StreamBits = bits(bits.getWidth - 1, bits.getWidth - 1)
    for (i <- (8 - bits.getWidth + 2) until 8) { // outputWordBits in [0, 8 - bits.getWidth] is the 0 bits remaining
      // case, and outputWordBits = 8 - bits.getWidth + 1 is the 1 bit remaining case, which is covered in the first
      // value of bitsRemainder above .. so start with 8 - bits.getWidth + 2
      bitsRemainder = StreamMux(outputWordBits === i.L,
        bits(bits.getWidth - 1, 8 - i /* 8 - i - 1 is the top bit that goes into updatedOut */), bitsRemainder)
    }
    swhen (outputWordBits + topBit >= 7.L) {
      Emit(0, updatedOut)
      outputWord := bitsRemainder
      outputWordBits := outputWordBits + topBit - 7.L
    } .otherwise {
      outputWord := updatedOut
      outputWordBits := outputWordBits + topBit + 1.L
    }
  }

  val finalWord = if (wordSize > 8) StreamInput(0) ## curWord(8 * (wordBytes - 1) - 1, 0) else StreamInput(0)
  val curLeadingZeros = leadingZeros(StreamMux(curState === READ_INPUT.id.L, finalWord, buffer(wordIdx)))
  swhen (curState === READ_INPUT.id.L) {
    for (i <- 0 until wordBytes - 1) {
      swhen(byteIdx === i.L) {
        curWord := (if (i == 0) curWord(8 * wordBytes - 1, 8) ## StreamInput(0)
        else curWord(8 * wordBytes - 1, 8 * (i + 1)) ## StreamInput(0) ## curWord(8 * i - 1, 0))
      }
    }
    swhen(byteIdx === (wordBytes - 1).L) {
      buffer(wordIdx) := finalWord
      byteIdx := 0.L
      swhen(wordIdx === (batchWords - 1).L) {
        wordIdx := 0.L
        curState := EMIT_MAIN.id.L
      } .otherwise {
        wordIdx := wordIdx + 1.L
      }
      for ((w, i) <- bitWidths.zipWithIndex) {
        swhen (curLeadingZeros >= (wordSize - w).L) {
          bitCounts(i)._1 := bitCounts(i)._1 + 1.L
        } .otherwise {
          swhen (curLeadingZeros < bitCounts(i)._2) {
            bitCounts(i)._2 := curLeadingZeros
          }
          bitCounts(i)._3 := bitCounts(i)._3 + lzToVarIntBits(curLeadingZeros)
        }
      }
    }
  } .otherwise {
    swhile (wordIdx < batchWords.L) {

    }
    wordIdx := 0.L
    curState := StreamMux(curState === EMIT_MAIN.id.L, EMIT_EXCEPT.id.L, READ_INPUT.id.L)
  }
  Builder.curBuilder.compile()
}
