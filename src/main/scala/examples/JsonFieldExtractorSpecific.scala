package examples

import chisel3.util
import language._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer

// fields must be bottom-level (i.e. non-record types), and must all be present in every input record
class JsonFieldExtractorSpecific(fields: Array[Array[String]], maxNestDepth: Int,
                                 coreId: Int) extends ProcessingUnit(8) {
  val fieldsQuoted = fields.map(f => f.map(id => "\"" + id.replace("\"", "\\\"") + "\""))

  val sequentialBranches = new ArrayBuffer[ArrayBuffer[Char]]
  // map from (sequential branch, idx in branch) to (char, sequential branch for that char)
  val splitBranches = new HashMap[(Int, Int), ArrayBuffer[(Char, Int)]]
  sequentialBranches.append(new ArrayBuffer[Char])
  for (field <- fieldsQuoted) {
    var curBranch = 0
    var curIdx = 0
    for ((id, i) <- field.zipWithIndex) {
      for ((c, j) <- id.zipWithIndex) {
        if (curIdx == sequentialBranches(curBranch).length) {
          sequentialBranches(curBranch).append(c)
          curIdx += 1
        } else {
          if (sequentialBranches(curBranch)(curIdx) == c) {
            curIdx += 1
          } else {
            val split = splitBranches.getOrElseUpdate((curBranch, curIdx), new ArrayBuffer[(Char, Int)])
            var splitIdx = 0
            while (splitIdx < split.length && c != split(splitIdx)._1) {
              splitIdx += 1
            }
            if (splitIdx == split.length) {
              if (i == field.length - 1 && j == id.length - 1) {
                split.append((c, -1))
              } else {
                split.append((c, sequentialBranches.length))
                sequentialBranches.append(new ArrayBuffer[Char])
              }
            }
            curBranch = split(splitIdx)._2
            curIdx = 0
          }
        }
      }
    }
  }

  var curStateId = 0
  val sequentialStateIds = new Array[Int](sequentialBranches.length)
  for ((branch, i) <- sequentialBranches.zipWithIndex) {
    sequentialStateIds(i) = curStateId
    curStateId += branch.length
  }
  val stateBits = util.log2Ceil(curStateId + 1) // need space for curStateId as well

  val sequentialTransitions = new ArrayBuffer[BigInt]
  for ((branch, i) <- sequentialBranches.zipWithIndex) {
    for ((c, j) <- branch.zipWithIndex) {
      var trans = BigInt(c.toInt) << stateBits
      if (j == branch.length - 1) {
        trans |= curStateId // field complete
      } else {
        trans |= (sequentialStateIds(i) + j + 1) // must be the state ID after the current one
      }
      sequentialTransitions.append(trans)
    }
  }
  val splitTransitions = new ArrayBuffer[BigInt]
  for (((seqBranch, seqIdx), splits) <- splitBranches.iterator) {
    val stateForSplit = sequentialStateIds(seqBranch) + seqIdx
    for ((c, nextBranch) <- splits) {
      var trans = BigInt(c.toInt) << stateBits
      if (nextBranch == -1) {
        trans |= curStateId // field complete
      } else {
        trans |= sequentialStateIds(nextBranch)
      }
      splitTransitions.append((trans << stateBits) | stateForSplit)
    }
  }

  object ParseState extends Enumeration {
    type ParseState = Value
    val EXP_KEY, IN_KEY, EXP_COL, EXP_VAL, IN_VAL, EXP_COM = Value
  }
  import ParseState._

  val inStringValue = StreamReg(1, false)
  val lastChar = StreamReg(8, ' '.toInt)
  val nestDepth = StreamReg(util.log2Ceil(maxNestDepth + 1), 0)
  val parseState = StreamReg(util.log2Ceil(ParseState.maxId), EXP_VAL.id)
  val matchState = StreamReg(stateBits, 0)
  val seqTransVec = StreamVectorReg(stateBits + 8, sequentialTransitions.length, sequentialTransitions)
  val splitTransVec = if (splitTransitions.length > 0)
    StreamVectorReg(2 * stateBits + 8, splitTransitions.length, splitTransitions) else null
  val stateStack = (0 until maxNestDepth).map(i => StreamReg(stateBits, null))

  def isWhitespace(c: StreamBits) = c === ' '.toInt.L || c === '\n'.toInt.L || c === '\t'.toInt.L

  def popStateStack = {
    swhen (matchState === curStateId.L) {
      Emit(0, ','.toInt.L)
    }
    matchState := stateStack(0)
    for (i <- 0 until stateStack.length - 1) {
      stateStack(i) := stateStack(i + 1)
    }
  }

  def emitCurToken = {
    swhen (matchState === curStateId.L) {
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
  swhen ((parseState === IN_KEY.id.L || enteringKey) && matchState =/= curStateId.L &&
    (matchState =/= 0.L || nestDepth === 1.L)) { // only allow match to start at top level
    val selectedSeqEl = seqTransVec(matchState)
    swhen (StreamInput(0) === selectedSeqEl(stateBits + 7, stateBits)) {
      matchState := selectedSeqEl(stateBits - 1, 0)
    } .otherwise {
      var noSplit: StreamBool = true.L.B
      if (splitTransVec != null) {
        for (i <- 0 until splitTransVec.numEls) {
          val selectedSplitEl = splitTransVec(i.L)
          val splitMatch = matchState === selectedSplitEl(stateBits - 1, 0) &&
            StreamInput(0) === selectedSplitEl(2 * stateBits + 7, 2 * stateBits)
          noSplit = noSplit && !splitMatch
          swhen(splitMatch) {
            matchState := selectedSplitEl(2 * stateBits - 1, stateBits)
          }
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
