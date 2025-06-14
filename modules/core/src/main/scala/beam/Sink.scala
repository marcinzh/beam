package beam
import turbolift.!!
import turbolift.Extensions._
import turbolift.data.Chunk
import Syntax._


object Sink:
  def apply[I, R, U](body: (fx: SinkEffect[I, R]) => R !! (U & fx.type)): Stream[I, U] => R !! U =
    case object Fx extends SinkEffect[I, R]
    input => Fx.defaultHandler(input).handle(body(Fx))

  def unchunked[I, R, U](body: (fx: SinkEffect[I, R]) => R !! (U & fx.type)): Stream[I, U] => R !! U =
    case object Fx extends SinkEffect[I, R]
    input => Fx.defaultHandler(input).handle(body(Fx))

  def chunked[I, R, U](body: (fx: SinkEffect[Chunk[I], R]) => R !! (U & fx.type)): Stream[I, U] => R !! U =
    case object Fx extends SinkEffect[Chunk[I], R]
    input => Fx.defaultHandler(input.chunks).handle(body(Fx))
