
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import Cache._

class MyModuleSpec extends AnyFlatSpec {
   behavior of "data_array"
  it should "success" in{
    simulate(new data_array(2,512)) { c =>
      c.io.port(1).writePorts(1).address.poke(2.U)
      c.io.port(1).writePorts(1).data.poke(4.U)
      c.io.port(1).writePorts(1).enable.poke(true.B)
      c.clock.step()
      c.io.port(1).readPorts(1).address.poke(2.U)
      c.io.port(1).readPorts(1).enable.poke(true.B)
      c.clock.step()
      c.io.port(1).readPorts(1).data.expect(4.U)

      println("PASS" )
    }
  }
}