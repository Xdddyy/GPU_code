package top

import chisel3._
import chisel3.util._
import chisel3.stage._


import _root_.circt.stage.ChiselStage
import Cache.data_array

object GenerateVerilog extends App {
  // 生成 Verilog 文件
  //val verilogString = chisel3.getVerilogString(new data_array)
  //println(verilogString)



  // 如果你希望指定文件名和路径，可以这样做：
   (new ChiselStage).execute(
  Array("--target", "verilog"),
  Seq(ChiselGeneratorAnnotation(() => new data_array()))
)
}
