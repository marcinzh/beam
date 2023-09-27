package beam
import turbolift.!!
import turbolift.Extensions._
import turbolift.effects._
import beam.effects.PipeEffect
import beam.protocol._


final class Pipe[I, O, U](val fun: PullUp[I, U] => PullUp[O, U]):
  def flowFrom(stream: Stream[I, U]): Stream[O, U] = new Stream(fun(stream.pull))
  def flowFrom[I2](pipe: Pipe[I2, I, U]): Pipe[I2, O, U] = new Pipe(pull => fun(pipe.fun(pull)))
  def flowTo[O2](pipe: Pipe[O, O2, U]): Pipe[I, O2, U] = pipe.flowFrom(this)
  def flowTo[R](sink: Sink[O, R, U]): Sink[I, R, U] = sink.flowFrom(this)
  def <-<(stream: Stream[I, U]): Stream[O, U] = flowFrom(stream)
  def <-<[I2](pipe: Pipe[I2, I, U]): Pipe[I2, O, U] = flowFrom(pipe)
  def >->[O2](pipe: Pipe[O, O2, U]): Pipe[I, O2, U] = flowTo(pipe)
  def >->[R](sink: Sink[O, R, U]): Sink[I, R, U] = flowTo(sink)


object Pipe:
  def coroutine[I, O, U](body: (fx: PipeEffect[I, O]) => Unit !! (U & fx.type)): Pipe[I, O, U] =
    case object Fx extends PipeEffect[I, O]
    new Pipe(up =>
      val down = body(Fx).handleWith[U](Fx.handler[U](up))
      PullUp.invert(down)
    )

  def transform[I, O, U](f: Stream[I, U] => Stream[O, U]): Pipe[I, O, U] =
    new Pipe(pull => f(new Stream(pull)).pull)

