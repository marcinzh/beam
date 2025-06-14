package beam
import turbolift.!!
import turbolift.Extensions._
import turbolift.data.Chunk
import Syntax._


object Source:
  def apply[O, U](body: (fx: SourceEffect[O]) => Unit !! (U & fx.type)): Stream[O, U] = unchunked(body)

  def unchunked[O, U](body: (fx: SourceEffect[O]) => Unit !! (U & fx.type)): Stream[O, U] =
    case object Fx extends SourceEffect[O]
    body(Fx).asUnchunkedStream(Fx)

  def chunked[O, U](body: (fx: SourceEffect[Chunk[O]]) => Unit !! (U & fx.type)): Stream[O, U] =
    case object Fx extends SourceEffect[Chunk[O]]
    body(Fx).asChunkedStream(Fx)
