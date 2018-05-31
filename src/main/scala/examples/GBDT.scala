package examples

import chisel3.util
import language._

object GBDT {

}

class GBDT(maxFields: Int, maxFieldBitWidth: Int, maxTotalNodes: Int, maxTreeDepth: Int, coreId: Int)
  extends ProcessingUnit(32, 32, coreId) {
  assert(maxTreeDepth > 1)
  assert(Math.pow(2, maxTreeDepth).toInt - 1 <= maxTotalNodes) // total nodes can fit at least one maximally sized tree
  val treeNodeSize = util.log2Ceil(maxFields) + maxFieldBitWidth
  assert(treeNodeSize <= 32) // size of tree node
  assert(util.log2Ceil(maxTotalNodes + 1) <= 32)

  val fields = NewStreamBRAM(maxFieldBitWidth, maxFields)
  val nodes = NewStreamBRAM(util.log2Ceil(maxFields) + maxFieldBitWidth, maxTotalNodes)
  val numFields = NewStreamReg(util.log2Ceil(maxFields + 1), null)
  val singleTreeNodes = NewStreamReg(maxTreeDepth, null)
  val totalNodes = NewStreamReg(util.log2Ceil(maxTotalNodes + 1), null)
  val curTreeIdx = NewStreamReg(maxTreeDepth, 0)
  val curNodesIdx = NewStreamReg(util.log2Ceil(maxTotalNodes + 1), 0)
  val curFieldIdx = NewStreamReg(util.log2Ceil(maxFields), 0)
  val sum = NewStreamReg(32, 0)
  val readFirstInput = NewStreamReg(1, false)

  object InputState extends Enumeration {
    type InputState = Value
    val READ_NUM_FIELDS, READ_NUM_SINGLE_TREE_NODES, READ_NUM_TOTAL_TREE_NODES, READ_TREE_NODES, READ_INPUTS = Value
  }
  import InputState._
  val curState = NewStreamReg(util.log2Ceil(InputState.maxId), READ_NUM_FIELDS.id)

  swhen (curState === READ_NUM_FIELDS.id.L) {
    numFields := StreamInput
    curState := READ_NUM_SINGLE_TREE_NODES.id.L
  } .elsewhen (curState === READ_NUM_SINGLE_TREE_NODES.id.L) {
    singleTreeNodes := StreamInput
    curState := READ_NUM_TOTAL_TREE_NODES.id.L
  } .elsewhen (curState === READ_NUM_TOTAL_TREE_NODES.id.L) {
    totalNodes := StreamInput
    curState := READ_TREE_NODES.id.L
  } .elsewhen (curState === READ_TREE_NODES.id.L) {
    nodes(curNodesIdx) := StreamInput
    swhen (curNodesIdx === totalNodes - 1.L) {
      curNodesIdx := 0.L
      curState := READ_INPUTS.id.L
    } .otherwise {
      curNodesIdx := curNodesIdx + 1.L
    }
  } .elsewhen (curState === READ_INPUTS.id.L) {
    readFirstInput := true.L
    swhen (readFirstInput.B && curFieldIdx === 0.L) {
      swhile (curNodesIdx < totalNodes) {
        val curNode = nodes(curNodesIdx + curTreeIdx)
        swhen (curTreeIdx < singleTreeNodes(maxTreeDepth - 1, 1)) { // curTreeIdx < singleTreeNodes / 2
          swhen (fields(curNode(treeNodeSize - 1, maxFieldBitWidth)) < curNode(maxFieldBitWidth - 1, 0)) {
            curTreeIdx := (curTreeIdx ## 0.L(1)) + 1.L
          } .otherwise {
            curTreeIdx := (curTreeIdx ## 0.L(1)) + 2.L
          }
        } .otherwise {
          sum := sum + curNode(maxFieldBitWidth - 1, 0)
          curTreeIdx := 0.L
          curNodesIdx := curNodesIdx + singleTreeNodes
        }
      }
      Emit(sum)
      sum := 0.L
      curNodesIdx := 0.L
    }
    fields(curFieldIdx) := StreamInput
    swhen (curFieldIdx === numFields - 1.L) {
      curFieldIdx := 0.L
    } .otherwise {
      curFieldIdx := curFieldIdx + 1.L
    }
  }
}

