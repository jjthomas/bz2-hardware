package examples

import chisel3.util
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer

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

  var curStateId = 1 // space for the 0 init state
  val sequentialStateIds = new Array[Int](sequentialBranches.length)
  for ((branch, i) <- sequentialBranches.zipWithIndex) {
    sequentialStateIds(i) = curStateId
    curStateId += branch.length
  }
  val stateBits = util.log2Ceil(curStateId)

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

}
