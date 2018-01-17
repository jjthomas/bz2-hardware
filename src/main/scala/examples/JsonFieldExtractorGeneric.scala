package examples

import chisel3.util
import language._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer

class JsonFieldExtractorGeneric(maxFieldChars: Int, maxFields: Int, maxNestDepth: Int,
                                coreId: Int) extends ProcessingUnit(8) {
  object ParseState extends Enumeration {
    type ParseState = Value
    val CONF_SEQ, CONF_SPLIT, EXP_KEY, IN_KEY, EXP_COL, EXP_VAL, IN_VAL, EXP_COM = Value
  }
  import ParseState._

  val stateBits = util.log2Ceil(maxFieldChars + 1)
  val numWordsForConfigToken = ((2 * stateBits + 8) + 8 - 1) / 8

  val configToken = StreamReg(numWordsForConfigToken * 8, null)
  val configWordNum = StreamReg(util.log2Ceil(numWordsForConfigToken), 0)
  val configTokenNum = StreamReg(util.log2Ceil(maxFieldChars), 0)
  val inStringValue = StreamReg(1, false)
  val lastChar = StreamReg(8, ' '.toInt)
  val nestDepth = StreamReg(util.log2Ceil(maxNestDepth + 1), 0)
  val parseState = StreamReg(util.log2Ceil(ParseState.maxId), CONF_SEQ.id)
  val matchState = StreamReg(stateBits, 0)
  val seqTransVec = StreamBRAM(stateBits + 8, maxFieldChars)
  val splitTransVec = StreamVectorReg(2 * stateBits + 8, maxFields, (0 until maxFields).map(_ => BigInt(0)))
  val stateStack = (0 until maxNestDepth).map(i => StreamReg(stateBits, null))

  swhen (parseState === CONF_SEQ.id.L || parseState === CONF_SPLIT.id.L) {
    swhen (configWordNum === (numWordsForConfigToken - 1).L) {
      val finalConfigToken = StreamInput(0)##configToken((numWordsForConfigToken - 1) * 8 - 1, 0)
      swhen (finalConfigToken === ((BigInt(1) << (numWordsForConfigToken * 8)) - 1).L) {
        swhen (parseState === CONF_SEQ.id.L) {
          parseState := CONF_SPLIT.id.L
        } .otherwise {
          parseState := EXP_VAL.id.L
        }
        configTokenNum := 0.L
      } .otherwise {
        swhen (parseState === CONF_SEQ.id.L) {
          seqTransVec(configTokenNum) := finalConfigToken(stateBits + 7, 0)
        } .otherwise {
          splitTransVec(configTokenNum) := finalConfigToken(2 * stateBits + 7, 0)
        }
        configTokenNum := configTokenNum + 1.L
      }
      configWordNum := 0.L
    } .otherwise {
      for (i <- 0 until numWordsForConfigToken - 1) {
        swhen (configWordNum === i.L) {
          if (i == 0) {
            configToken := configToken(numWordsForConfigToken * 8 - 1, 8)##StreamInput(0)
          } else {
            configToken := configToken(numWordsForConfigToken * 8 - 1, 8 * (i + 1))##StreamInput(0)##
              configToken(8 * i - 1, 0)
          }
        }
      }
      configWordNum := configWordNum + 1.L
    }
  }

  def isWhitespace(c: StreamBits) = c === ' '.toInt.L || c === '\n'.toInt.L || c === '\t'.toInt.L

  def popStateStack = {
    swhen (matchState === maxFieldChars.L) {
      Emit(0, ','.toInt.L)
    }
    matchState := stateStack(0)
    for (i <- 0 until stateStack.length - 1) {
      stateStack(i) := stateStack(i + 1)
    }
  }

  def emitCurToken = {
    swhen (matchState === maxFieldChars.L) {
      Emit(0, StreamInput(0))
    }
  }

  swhen (parseState === EXP_VAL.id.L) {
    swhen (StreamInput(0) === '{'.toInt.L) {
      parseState := EXP_KEY.id.L
      nestDepth := nestDepth + 1.L
    } .elsewhen (nestDepth =/= 0.L && !isWhitespace(StreamInput(0))) { // at nestDepth of 0 we only accept new records
      emitCurToken
      parseState := IN_VAL.id.L
      inStringValue := StreamInput(0) === '"'.toInt.L
    }
  }
  swhen (parseState === IN_VAL.id.L) {
    swhen (inStringValue.B) {
      emitCurToken
      swhen (StreamInput(0) === '"'.toInt.L && lastChar =/= '\\'.toInt.L) {
        inStringValue := false.L
      }
    } .elsewhen (StreamInput(0) =/= '}'.toInt.L) {
      swhen (StreamInput(0) === ','.toInt.L) {
        parseState := EXP_KEY.id.L
        popStateStack
      } .otherwise {
        emitCurToken
      }
    }
  }
  swhen (StreamInput(0) === ','.toInt.L && parseState === EXP_COM.id.L) {
    parseState := EXP_KEY.id.L
    popStateStack
  }
  swhen (StreamInput(0) === '}'.toInt.L &&
    (parseState === EXP_KEY.id.L || parseState === EXP_COM.id.L || (parseState === IN_VAL.id.L && !inStringValue.B))) {
    swhen (nestDepth === 1.L) {
      parseState := EXP_VAL.id.L
    } .otherwise {
      parseState := EXP_COM.id.L
    }
    swhen (parseState === EXP_COM.id.L || parseState === IN_VAL.id.L) {
      popStateStack
    }
    nestDepth := nestDepth - 1.L
  }

  val enteringKey = StreamInput(0) === '"'.toInt.L && parseState === EXP_KEY.id.L
  swhen (enteringKey) {
    parseState := IN_KEY.id.L
    stateStack(0) := matchState
    for (i <- 1 until stateStack.length) {
      stateStack(i) := stateStack(i - 1)
    }
  }
  swhen ((parseState === IN_KEY.id.L || enteringKey) && matchState =/= maxFieldChars.L &&
    (matchState =/= 0.L || nestDepth === 1.L)) { // only allow match to start at top level
  val selectedSeqEl = seqTransVec(matchState)
    swhen (StreamInput(0) === selectedSeqEl(stateBits + 7, stateBits)) {
      matchState := selectedSeqEl(stateBits - 1, 0)
    } .otherwise {
      var noSplit: StreamBool = true.L.B
      for (i <- 0 until splitTransVec.numEls) {
        val selectedSplitEl = splitTransVec(i.L)
        val splitMatch = matchState === selectedSplitEl(stateBits - 1, 0) &&
          StreamInput(0) === selectedSplitEl(2 * stateBits + 7, 2 * stateBits)
        noSplit = noSplit && !splitMatch
        swhen(splitMatch) {
          matchState := selectedSplitEl(2 * stateBits - 1, stateBits)
        }
      }
      swhen (noSplit) {
        matchState := 0.L
      }
    }
  }
  swhen (StreamInput(0) === '"'.toInt.L && parseState === IN_KEY.id.L) {
    parseState := EXP_COL.id.L
  }
  swhen (StreamInput(0) === ':'.toInt.L && parseState === EXP_COL.id.L) {
    parseState := EXP_VAL.id.L
  }
  lastChar := StreamInput(0)

  Builder.curBuilder.compile()
}
