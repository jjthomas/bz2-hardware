package examples

import scala.collection.mutable.ArrayBuffer

class JsonFieldExtractor(fields: Array[Array[String]], coreId: Int) extends ProcessingUnit(8) {
  val fieldsQuoted = fields.map(f => f.map(id => "\"" + id + "\""))

  // char, is next branch sequential, next branch, idx for next branch
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
}
