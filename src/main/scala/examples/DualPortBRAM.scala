package examples

import chisel3._
import chisel3.core.Bundle

class DualPortBRAM(dataWidth: Int, addrWidth: Int)  extends Module /* extends BlackBox(Map("DATA" -> IntParam(dataWidth),
                                                                        "ADDR" -> IntParam(addrWidth))) */ {
  val io = IO(new Bundle {
    val a_addr = Input(UInt(addrWidth.W))
    val a_din = Input(UInt(dataWidth.W))
    val a_wr = Input(Bool())
    val a_dout = Output(UInt(dataWidth.W))
    val b_addr = Input(UInt(addrWidth.W))
    val b_din = Input(UInt(dataWidth.W))
    val b_wr = Input(Bool())
    val b_dout = Output(UInt(dataWidth.W))
  })

  // simulation model for BRAM
  // there's no guarantee about what happens on
  // collisions (sim access to same address with two memory ports)
  val mem = SyncReadMem(1 << addrWidth, UInt(dataWidth.W))

  io.a_dout := mem.read(io.a_addr)
  when (io.a_wr) {
    mem.write(io.a_addr, io.a_din)
  }

  io.b_dout := mem.read(io.b_addr)
  when (io.b_wr) {
    mem.write(io.b_addr, io.b_din)
  }
}