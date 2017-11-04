package examples

import chisel3._

class CsvFieldExtractor(numFields: Int, targetField: Int, coreId: Int) extends ProcessingUnit(8) {
  // state
  val curField = RegInit(0.asUInt(util.log2Ceil(numFields).W))
  val inQuote = RegInit(false.B)
  val lastChar = RegInit(' '.toInt.asUInt(8.W))

  // new states
  val inQuoteNext = Wire(Bool())
  when (io.inputWord === '"'.toInt.U) {
    when (!inQuote) {
      inQuoteNext := true.B
    } .otherwise {
      inQuoteNext := lastChar === '\\'.toInt.U
    }
  } .otherwise {
    inQuoteNext := inQuote
  }

  val curFieldNext = Wire(UInt(util.log2Ceil(numFields).W))
  when (io.inputWord === ','.toInt.U) {
    when (!inQuote) {
      curFieldNext := curField + 1.U
    } .otherwise {
      curFieldNext := curField
    }
  } .elsewhen (io.inputWord === '\n'.toInt.U) {
    when (!inQuote) {
      curFieldNext := 0.U
    } .otherwise {
      curFieldNext := curField
    }
  } .otherwise {
    curFieldNext := curField
  }

  val lastCharNext = Wire(UInt(8.W))
  lastCharNext := io.inputWord

  // output
  io.outputWord := io.inputWord
  io.outputValid := io.inputValid && curField === targetField.U && curFieldNext === targetField.U
  // (standard outputs for processing units that can only produce valid output on a cycle where input is valid)
  io.outputFinished := io.inputFinished
  io.inputReady := io.outputReady

  // commit new states
  when (io.inputValid) {
    inQuote := inQuoteNext
    curField := curFieldNext
    lastChar := lastCharNext
  }
}
