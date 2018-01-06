package examples

import chisel3.util
import scala.collection.mutable.ArrayBuffer

class JsonFieldExtractor(fields: Array[Array[String]], maxNestDepth: Int, coreId: Int) extends ProcessingUnit(8) {
  val fieldsQuoted = fields.map(f => f.map(id => "\"" + id + "\""))

  // char, is next branch sequential, next branch, idx for next branch
  // TODO "idx for next branch" can be eliminated because it is always 0 or -1
  val sequentialBranches = new ArrayBuffer[ArrayBuffer[(Char, Boolean, Int, Int)]]
  val splitBranches = new ArrayBuffer[ArrayBuffer[(Char, Boolean, Int, Int)]]
  splitBranches.append(new ArrayBuffer[(Char, Boolean, Int, Int)])
  for (field <- fieldsQuoted) {
    var curBranchInSequential = false
    var curBranch = 0
    var curIdx = 0
    var prevBranchSequential = false
    var prevBranch = 0
    var prevIdx = 0
    for ((id, i) <- field.zipWithIndex) {
      for ((c, j) <- id.zipWithIndex) {
        if (curBranchInSequential) {
          if (curIdx == sequentialBranches(curBranch).length) {
            sequentialBranches(curBranch).append((c, true, 0, 0)) // if next branch sequential, must be the next
            // element in the same branch
            curIdx += 1
          } else {
            if (sequentialBranches(curBranch)(curIdx)._1 == c) {
              prevBranchSequential = curBranchInSequential
              prevBranch = curBranch
              prevIdx = curIdx
              val next = sequentialBranches(curBranch)(curIdx)
              curBranchInSequential = next._2
              curBranch = next._3
              curIdx = next._4
            } else {
              if (prevBranchSequential) {
                val prev = sequentialBranches(prevBranch)(prevIdx)
                sequentialBranches(prevBranch)(prevIdx) = (prev._1, false, splitBranches.length, 0)
              } else {
                val prev = splitBranches(prevBranch)(prevIdx)
                splitBranches(prevBranch)(prevIdx) = (prev._1, false, splitBranches.length, 0)
              }
              val split = sequentialBranches(curBranch).remove(curIdx)
              splitBranches.append(new ArrayBuffer[(Char, Boolean, Int, Int)])
              splitBranches(splitBranches.length - 1).append((split._1, true, curBranch, curIdx))
              if (!(i == field.length - 1 && j == id.length - 1)) {
                sequentialBranches.append(new ArrayBuffer[(Char, Boolean, Int, Int)])
              }
              splitBranches(splitBranches.length - 1).append((c, true, sequentialBranches.length - 1,
                if (i == field.length - 1 && j == id.length - 1) -1 else 0))
              curBranch = sequentialBranches.length - 1
              curIdx = 0
            }
          }
        } else {
          while (curIdx < splitBranches(curBranch).length && c != splitBranches(curBranch)(curIdx)._1) {
            curIdx += 1
          }
          if (curIdx == splitBranches(curBranch).length) {
            if (!(i == field.length - 1 && j == id.length - 1)) {
              sequentialBranches.append(new ArrayBuffer[(Char, Boolean, Int, Int)])
            }
            splitBranches(curBranch).append((c, true, sequentialBranches.length -1,
              if (i == field.length - 1 && j == id.length - 1) -1 else 0))
            curBranchInSequential = true
            curBranch = sequentialBranches.length - 1
            curIdx = 0
          } else {
            prevBranchSequential = curBranchInSequential
            prevBranch = curBranch
            prevIdx = curIdx
            val next = splitBranches(curBranch)(curIdx)
            curBranchInSequential = next._2
            curBranch = next._3
            curIdx = next._4
          }
        }
      }
    }
  }

  var curStateId = 1 // space for the 0 init state
  val sequentialStateIds = new Array[Int](sequentialBranches.length)
  for ((branch, i) <- sequentialBranches.zipWithIndex) {
    sequentialStateIds(i) = curStateId
    curStateId += branch.length
  }
  val maxNeededState = curStateId + splitBranches.length // final state indicates a matched field
  val stateBits = util.log2Ceil(maxNeededState)

  val sequentialTransitions = new ArrayBuffer[BigInt]
  val splitTransitions = (0 until splitBranches.length).map(_ => new ArrayBuffer[BigInt])
  for ((branch, i) <- sequentialBranches.zipWithIndex) {
    for (((c, isNextSeq, nextBranch, nextIdx), j) <- branch.zipWithIndex) {
      var trans = BigInt(c.toInt) << stateBits
      if (isNextSeq) {
        if (j == branch.length - 1) {
          trans |= maxNeededState // field complete
        } else {
          trans |= (sequentialStateIds(i) + j + 1) // must be the state ID after the current one
        }
      } else {
        trans |= (curStateId + nextBranch) // state ID for the split branch
      }
      sequentialTransitions.append(trans)
    }
  }
  for ((branch, i) <- splitBranches.zipWithIndex) {
    for (((c, isNextSeq, nextBranch, nextIdx), j) <- branch.zipWithIndex) {
      var trans = BigInt(c.toInt) << stateBits
      if (isNextSeq) {
        if (nextIdx == -1) {
          trans |= maxNeededState // field complete
        } else {
          trans |= sequentialStateIds(nextBranch)
        }
      } else {
        trans |= (curStateId + nextBranch) // state ID for the split branch
      }
      splitTransitions(i).append(trans)
    }
  }

}
