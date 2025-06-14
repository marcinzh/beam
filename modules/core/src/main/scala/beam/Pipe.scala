package beam
import turbolift.!!
import turbolift.Extensions._
import turbolift.data.Chunk
import Syntax._


object Pipe:
  def apply[I, O, U](body: (fx: PipeEffect[I, O]) => Unit !! (U & fx.type)): Stream[I, U] => Stream[O, U] =
    unchunked(body)

  def unchunked[I, O, U](body: (fx: PipeEffect[I, O]) => Unit !! (U & fx.type)): Stream[I, U] => Stream[O, U] =
    case object Fx1 extends PipeEffect[I, O]
    case object Fx2 extends SourceEffect[O]
    input => Fx1.defaultHandler(Fx2, input).handle(body(Fx1)).asUnchunkedStream(Fx2)

  def chunked[I, O, U](body: (fx: PipeEffect[Chunk[I], Chunk[O]]) => Unit !! (U & fx.type)): Stream[I, U] => Stream[O, U] =
    case object Fx1 extends PipeEffect[Chunk[I], Chunk[O]]
    case object Fx2 extends SourceEffect[Chunk[O]]
    input => Fx1.defaultHandler(Fx2, input.chunks).handle(body(Fx1)).asChunkedStream(Fx2)

  def chunkedToUnchunked[I, O, U](body: (fx: PipeEffect[Chunk[I], O]) => Unit !! (U & fx.type)): Stream[I, U] => Stream[O, U] =
    case object Fx1 extends PipeEffect[Chunk[I], O]
    case object Fx2 extends SourceEffect[O]
    input => Fx1.defaultHandler(Fx2, input.chunks).handle(body(Fx1)).asUnchunkedStream(Fx2)

  def unchunkedToChunked[I, O, U](body: (fx: PipeEffect[I, Chunk[O]]) => Unit !! (U & fx.type)): Stream[I, U] => Stream[O, U] =
    case object Fx1 extends PipeEffect[I, Chunk[O]]
    case object Fx2 extends SourceEffect[Chunk[O]]
    input => Fx1.defaultHandler(Fx2, input).handle(body(Fx1)).asChunkedStream(Fx2)
