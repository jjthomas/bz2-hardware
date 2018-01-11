package examples

import chisel3.util
import language._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer

// fields must be bottom-level (i.e. non-record types), and must all be present in every input record
class JsonFieldExtractor(fields: Array[Array[String]], maxNestDepth: Int, coreId: Int) extends ProcessingUnit(8) {
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
  val splitTransitions = new ArrayBuffer[(Int, ArrayBuffer[BigInt])] // (corresponding state ID, list of transitions)
  for (((seqBranch, seqIdx), splits) <- splitBranches.iterator) {
    val curTrans = new ArrayBuffer[BigInt]
    splitTransitions.append((sequentialStateIds(seqBranch) + seqIdx, curTrans))
    for ((c, nextBranch) <- splits) {
      var trans = BigInt(c.toInt) << stateBits
      if (nextBranch == -1) {
        trans |= curStateId // field complete
      } else {
        trans |= sequentialStateIds(nextBranch)
      }
      curTrans.append(trans)
    }
  }

  val numParseStates = 6 // expecting key (0), in key (1), expecting colon (2), expecting value (3), in value (4),
  // expecting comma (5)
  val inStringValue = StreamReg(1, false)
  val lastChar = StreamReg(8, ' '.toInt)
  val nestDepth = StreamReg(util.log2Ceil(maxNestDepth), 0)
  val parseState = StreamReg(util.log2Ceil(numParseStates), 3)
  val matchState = StreamReg(stateBits, 0)
  val seqTransVec = StreamVectorReg(stateBits + 8, sequentialTransitions.length, sequentialTransitions)
  val splitTransVecs = splitTransitions.map { case (stateId, trans) =>  (stateId,
    StreamVectorReg(stateBits + 8, trans.length, trans)) }
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

  swhen (parseState === 3.L) {
    swhen (StreamInput(0) === '{'.toInt.L) {
      parseState := 0.L
      nestDepth := nestDepth + 1.L
    } .elsewhen (nestDepth =/= 0.L && !isWhitespace(StreamInput(0))) { // at nestDepth of 0 we only accept new records
      Emit(0, StreamInput(0))
      parseState := 4.L
      inStringValue := StreamInput(0) === '"'.toInt.L
    }
  }
  swhen (parseState === 4.L) {
    swhen (inStringValue.B) {
      swhen (StreamInput(0) === '"'.toInt.L && lastChar =/= '\\'.toInt.L) {
        parseState := 5.L
        popStateStack
        inStringValue := false.L
      } .otherwise {
        Emit(0, StreamInput(0))
      }
    } .otherwise {
      swhen (StreamInput(0) === ','.toInt.L) {
        parseState := 0.L
        popStateStack
      } .elsewhen (isWhitespace(StreamInput(0))) {
        parseState := 5.L
        popStateStack
      } .otherwise {
        Emit(0, StreamInput(0))
      }
    }
  }
  swhen (StreamInput(0) === ','.toInt.L && parseState === 5.L) {
    parseState := 0.L
  }
  swhen (StreamInput(0) === '}'.toInt.L &&
    (parseState === 0.L || parseState === 5.L || (parseState === 4.L && !inStringValue.B))) {
    swhen (nestDepth === 1.L) {
      parseState := 3.L
    } .otherwise {
      parseState := 5.L
      popStateStack
    }
    nestDepth := nestDepth - 1.L
  }

  val enteringKey = StreamInput(0) === '"'.toInt.L && parseState === 0.L
  swhen (enteringKey) {
    parseState := 1.L
    stateStack(0) := matchState
    for (i <- 1 until stateStack.length) {
      stateStack(i) := stateStack(i - 1)
    }
  }
  swhen ((parseState === 1.L || enteringKey) && matchState =/= curStateId.L &&
    (matchState =/= 0.L || nestDepth === 1.L)) { // only allow match to start at top level
    val selectedSeqEl = seqTransVec(matchState)
    swhen (StreamInput(0) === selectedSeqEl(stateBits + 7, stateBits)) {
      matchState := selectedSeqEl(stateBits - 1, 0)
    } .otherwise {
      var noSplit: StreamBool = true.L.B
      for ((stateId, vec) <- splitTransVecs) {
        noSplit = noSplit && !(matchState === stateId.L)
        swhen (matchState === stateId.L) {
          var noMatch: StreamBool = true.L.B
          for (i <- 0 until vec.numEls) {
            val selectedSplitEl = vec(i.L)
            noMatch = noMatch && !(StreamInput(0) === selectedSplitEl(stateBits + 7, stateBits))
            swhen (StreamInput(0) === selectedSplitEl(stateBits + 7, stateBits)) {
              matchState := selectedSplitEl(stateBits - 1, 0)
            }
          }
          swhen (noMatch) {
            matchState := 0.L
          }
        }
      }
      swhen (noSplit) {
        matchState := 0.L
      }
    }
  }
  swhen (StreamInput(0) === '"'.toInt.L && parseState === 1.L) {
    parseState := 2.L
  }
  swhen (StreamInput(0) === ':'.toInt.L && parseState === 2.L) {
    parseState := 3.L
  }
  lastChar := StreamInput(0)
}
