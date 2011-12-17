package Top
{

import Chisel._
import Node._;
import scala.math._;

object log2up
{
  def apply(in: Int) = ceil(log(in)/log(2)).toInt
}

object FillInterleaved
{
  def apply(n: Int, in: Bits) =
  {
    var out = Fill(n, in(0))
    for (i <- 1 until in.width)
      out = Cat(Fill(n, in(i)), out)
    out
  }
}

class Mux1H(n: Int, w: Int) extends Component
{
  val io = new Bundle {
    val sel = Vec(n) { Bool(dir = 'input) }
    val in  = Vec(n) { Bits(width = w, dir = 'input) }
    val out = Bits(width = w, dir = 'output)
  }

  if (n > 1) {
    var out = io.in(0) & Fill(w, io.sel(0))
    for (i <- 1 to n-1)
      out = out | (io.in(i) & Fill(w, io.sel(i)))
    io.out := out
  } else {
    io.out := io.in(0)
  }
}

class ioDecoupled[T <: Data]()(data: => T) extends Bundle
{
  val valid = Bool('input)
  val ready = Bool('output)
  val bits  = data.asInput
}

class ioArbiter[T <: Data](n: Int)(data: => T) extends Bundle {
  val in  = Vec(n) { (new ioDecoupled()) { data } }
  val out = (new ioDecoupled()) { data }.flip()
}

class Arbiter[T <: Data](n: Int)(data: => T) extends Component {
  val io = new ioArbiter(n)(data)
  val vout = Wire { Bool() }

  io.in(0).ready := io.out.ready
  for (i <- 1 to n-1) {
    io.in(i).ready := !io.in(i-1).valid && io.in(i-1).ready
  }

  var dout = io.in(n-1).bits
  for (i <- 1 to n-1)
    dout = Mux(io.in(n-1-i).valid, io.in(n-1-i).bits, dout)

  for (i <- 0 to n-2) {
    when (io.in(i).valid) { vout <== Bool(true) }
  }
  vout <== io.in(n-1).valid

  vout ^^ io.out.valid
  dout ^^ io.out.bits
}

class ioPriorityDecoder(in_width: Int, out_width: Int) extends Bundle
{
  val in  = UFix(in_width, 'input);
  val out = Bits(out_width, 'output);
}

class priorityDecoder(width: Int) extends Component
{
  val in_width = ceil(log10(width)/log10(2)).toInt;  
  val io = new ioPriorityEncoder(in_width, width);
  val l_out = Wire() { Bits() };
  
  for (i <- 0 to width-1) {
    when (io.in === UFix(i, in_width)) {
      l_out <== Bits(1,1) << UFix(i);
    }
  }
  
  l_out <== Bits(0, width);
  io.out := l_out;
}

class ioPriorityEncoder(in_width: Int, out_width: Int) extends Bundle
{
  val in  = Bits(in_width, 'input);
  val out = UFix(out_width, 'output);
}

class priorityEncoder(width: Int) extends Component
{
  val out_width = ceil(log10(width)/log10(2)).toInt;  
  val io = new ioPriorityDecoder(width, out_width);
  val l_out = Wire() { UFix() };
  
  for (i <- 0 to width-1) {
    when (io.in(i).toBool) {
      l_out <== UFix(i, out_width);
    }
  }
  
  l_out <== UFix(0, out_width);
  io.out := l_out;
}

}
