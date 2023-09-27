package beam
import turbolift.!!
import turbolift.Extensions._
import turbolift.effects.NonDet
import beam.effects.StreamEffect
import beam.protocol._


final class Stream[A, U](val pull: PullUp[A, U]) extends AnyVal:
  def flowTo[B](pipe: Pipe[A, B, U]): Stream[B, U] = pipe.flowFrom(this)
  def flowTo[B](sink: Sink[A, B, U]): B !! U = sink.flowFrom(this)
  def >->[B](pipe: Pipe[A, B, U]): Stream[B, U] = flowTo(pipe)
  def >->[B](sink: Sink[A, B, U]): B !! U = flowTo(sink)


object Stream:
  def apply[A](as: A*): Stream[A, Any] = from(as)
  def empty[A]: Stream[A, Any] = new Stream(PullUp.alwaysStop)

  def coroutine[O, U](body: (fx: StreamEffect[O]) => Unit !! (U & fx.type)): Stream[O, U] =
    case object Fx extends StreamEffect[O]
    val down = body(Fx).handleWith[U](Fx.handler[U])
    new Stream(PullUp.invert(down))
  
  def from[A](as: Iterable[A]): Stream[A, Any] = new Stream(pullFromIterator(as.iterator))
  def from[A](as: Iterator[A]): Stream[A, NonDet] = new Stream(pullFromIterator(as))

  private def pullFromIterator[A](it: Iterator[A]): PullUp[A, Any] =
    def loop(more: Boolean): PullDown[A, Any] !! Any =
      if more && it.hasNext
      then PullDown.Emit(it.next(), loop).pure_!!
      else PullDown.stopPure
    loop
