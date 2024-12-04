package Cache

import chisel3._
import chisel3.util._
import scala.math._

//包含多个SRAM
class data_array(cache_line_number:Int=512,data_block_size:Int=1024,width: Int=8,numRead: Int=2, numWrite: Int=2, numReadwrite: Int=2)extends Module{

    val io = IO(new Bundle{
      val port=Vec(cache_line_number,new SRAMInterface(data_block_size, UInt(width.W), numRead, numWrite, numReadwrite))
    })
    
    for(i<-0 until cache_line_number){
        io.port(i) :<>= SRAM(data_block_size, UInt(width.W), numRead, numWrite, numReadwrite)
    }    

}

//包含多个通道进行读写----多路组相连
class tag_array(writeprots:Int=4,readprots:Int=4,roadnum:Int=4,addr_lenth:Int=32,
cache_line_number:Int=512,data_block_size:Int=1024,width: Int=8,numRead: Int=2, numWrite: Int=2, numReadwrite: Int=2) extends Module{

    //IO端口
    val io=IO(new Bundle {
        //写请求
        val wr_data = Input(Vec(writeprots,UInt(width.W)))
        val wr_addr = Input(Vec(writeprots,UInt(addr_lenth.W)))
        val wr_en   = Input(Vec(writeprots,Bool()))
        //读请求
        val rd_addr = Input(Vec(readprots,UInt(addr_lenth.W)))
        val rd_en   = Input(Vec(readprots,Bool()))
        val rd_data = Output(Vec(readprots,UInt(width.W)))
    })

    val data_array  = Module(new data_array(cache_line_number,data_block_size,width,numRead,numWrite,numReadwrite))

    //flag与tag
    //====tag=====index=====offset
    //offset=根号2(data_block_size)
    //index=根号2(cache_line_number/roadnum)
    //tag=addr_lenth-index-offset
    val numSets = cache_line_number / roadnum   // 每组的行数
    val offset_width = (log(data_block_size)/log(2)).toInt
    val index_width  = (log(cache_line_number/roadnum)/log(2)).toInt
    val tag_witdh    = (addr_lenth-offset_width-index_width).toInt

    //标志位与tag
    val valid   = RegInit(VecInit(Seq.fill(numSets - 1)(VecInit(Seq.fill(roadnum - 1)(false.B(Bool()))))))  //每组个数*组数
    val dirty   = RegInit(VecInit(Seq.fill(numSets - 1)(VecInit(Seq.fill(roadnum - 1)(false.B(Bool())))))) 
    val tag     = RegInit(VecInit(Seq.fill(numSets - 1)(VecInit(Seq.fill(roadnum - 1)(0.U(tag_witdh.W)))))) //每组个数*组数，并且根据计算得出位宽

    //IO寄存
    //写端口
    val wr_addr_r_tag       =RegInit(VecInit(Seq.fill(writeprots - 1)(0.U(tag_witdh.W))))
    val wr_addr_r_index     =RegInit(VecInit(Seq.fill(writeprots - 1)(0.U(index_width.W))))
    val wr_addr_r_offset    =RegInit(VecInit(Seq.fill(writeprots - 1)(0.U(offset_width.W))))
    val wr_data_r           =RegInit(VecInit(Seq.fill(writeprots - 1)(0.U(width.W))))
    //读端口
    val rd_addr_r_tag       =RegInit(VecInit(Seq.fill(readprots - 1)(0.U(tag_witdh.W))))
    val rd_addr_r_index     =RegInit(VecInit(Seq.fill(readprots - 1)(0.U(index_width.W))))
    val rd_addr_r_offset    =RegInit(VecInit(Seq.fill(readprots - 1)(0.U(offset_width.W))))
    
    //IO使能
    //写使能
    for(i<-0 until writeprots){
        when(io.wr_en(i)){
            wr_addr_r_tag(i):=io.wr_addr(i)(addr_lenth-1,addr_lenth-tag_witdh)
            wr_addr_r_index(i):=io.wr_addr(i)(addr_lenth-tag_witdh-1,offset_width)
            wr_addr_r_offset(i):=io.wr_addr(i)(offset_width-1,0)
            wr_data_r(i):=io.wr_addr(i)
        }
    }
    //读使能
    for(i<-0 until readprots){
        when(io.rd_en(i)){
            rd_addr_r_tag(i):=io.rd_addr(i)(addr_lenth-1,addr_lenth-tag_witdh)
            rd_addr_r_index(i):=io.rd_addr(i)(addr_lenth-tag_witdh-1,offset_width)
            rd_addr_r_offset(i):=io.rd_addr(i)(offset_width-1,0)
        }
    }


    //根据Index选择特定一组SRAM
    //write_port
    val write_hit_result          = Wire(Vec(writeprots,UInt(roadnum.W)))
    val write_Index_decoder = Wire(Vec(writeprots,UInt(numSets.W)))
    for(i<-0 until writeprots){
        write_Index_decoder(i) :=  UIntToOH(wr_addr_r_index(i))
        for(j<-0 until numSets){
            //选择特定一组Index
            when(write_Index_decoder(i)(j)){
                //比较tag
                for(k<-0 until roadnum){
                    write_hit_result(i)(k) := (wr_addr_r_tag(i)===tag(j)(k))&&(valid(j)(k)&&dirty(j)(k))
                }
            }
        }
    }
    //read_port
    val read_hit_result         = Wire(Vec(readprots,UInt(roadnum.W)))
    val read_hit_result_1bit    = Wire(Vec(readprots,Bool()))
    val read_Index_decoder      = Wire(Vec(readprots,UInt(numSets.W)))
    for(i<-0 until readprots){
        read_Index_decoder(i) :=  UIntToOH(rd_addr_r_index(i))
        for(j<-0 until numSets){
            //选择特定一组Index
            when(read_Index_decoder(i)(j)){
                //比较tag判断是否命中
                for(k<-0 until roadnum){
                    read_hit_result(i)(k) := (rd_addr_r_tag(i)===tag(j)(k))&&(valid(j)(k)&&dirty(j)(k))
                }
                read_hit_result_1bit(i) := read_hit_result(i).orR
            }
        }
    }
    
    
}