package beam
import turbolift.!!
import turbolift.Extensions._
import Syntax._


object Source:
  def apply[O, U](body: (fx: SourceEffect[O]) => Unit !! (U & fx.type)): Stream[O, U] =
    case object Fx extends SourceEffect[O]
    body(Fx).asStream(Fx)
