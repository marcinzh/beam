package beam
import turbolift.!!
import turbolift.Extensions._
import Syntax._


object Pipe:
  def apply[I, O, U](body: (fx: PipeEffect[I, O]) => Unit !! (U & fx.type)): Stream[I, U] => Stream[O, U] =
    case object Fx extends PipeEffect[I, O]
    stream => body(Fx).handleWith[U](Fx.defaultHandler[U](stream)).flattenAsStream
