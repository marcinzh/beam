package beam
import turbolift.!!
import turbolift.Extensions._
import Syntax._


object Pipe:
  def apply[I, O, U](body: (fx: PipeEffect[I, O]) => Unit !! (U & fx.type)): Stream[I, U] => Stream[O, U] =
    case object Fx1 extends PipeEffect[I, O]
    case object Fx2 extends SourceEffect[O]
    input => Fx1.defaultHandler(Fx2, input).handle(body(Fx1)).asStream(Fx2)
