package Cache

import chisel3._
import chisel3.util._

class PLRU(val WAY_N: Int) extends Module {
  // 计算树的高度
  val LEVEL = math.ceil(math.log(WAY_N) / math.log(2)).toInt
  val TREE_W = WAY_N - 1

  // 输入输出定义
  val io = IO(new Bundle {
    val i_updt      = Input(Bool())                          // 更新信号
    val hit_result  = Input(Bool())
  })

  // 内部信号定义
  val tree_btm      = RegInit(0.U(TREE_W.W))                 // 树的底层节点
  val tree_btm_nxt  = RegInit(0.U(TREE_W.W))               // 树的下一状态
  val out           = Wire(UInt(WAY_N.W))

  //未命中情况下进行替换
  when(~io.hit_result){
    
  }

  for(i<- 0 until LEVEL){

  }

  
  




}

