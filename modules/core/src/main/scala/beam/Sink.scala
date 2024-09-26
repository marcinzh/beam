package beam
import turbolift.!!
import turbolift.Extensions._
import Syntax._


object Sink:
  def apply[I, R, U](body: (fx: SinkEffect[I, R]) => R !! (U & fx.type)): Stream[I, U] => R !! U =
    case object Fx extends SinkEffect[I, R]
    stream => body(Fx).handleWith(Fx.handlers.dropRemainder[U](stream))
