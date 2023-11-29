package beam
import turbolift.!!
import turbolift.Extensions._
import beam.effects.SinkEffect
import beam.internals.Step
import beam.internals.StepOps


final class Sink[-I, +R, U](val fun: Stream[I, U] => R !! U):
  def from(stream: Stream[I, U]): R !! U = fun(stream)
  def from[I2](pipe: Pipe[I2, I, U]): Sink[I2, R, U] = new Sink(stream => fun(pipe.unwrap(stream)))
  def <-<(stream: Stream[I, U]): R !! U = from(stream)
  def <-<[I2](pipe: Pipe[I2, I, U]): Sink[I2, R, U] = from(pipe)


object Sink:
  def coroutine[I, R, U](body: (fx: SinkEffect[I, R]) => R !! (U & fx.type)): Sink[I, R, U] =
    case object Fx extends SinkEffect[I, R]
    new Sink(stream => body(Fx).handleWith(Fx.handler[U](stream)))


  def fold[I, R](zero: R)(op: (R, I) => R): Sink[I, R, Any] =
    new Sink(_.fold(zero)(op))

  def fold_!![I, R, U](zero: R)(op: (R, I) => R !! U): Sink[I, R, U] =
    new Sink(_.fold_!!(zero)(op))
