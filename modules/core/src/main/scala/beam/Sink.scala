package beam
import turbolift.!!
import turbolift.Extensions._
import beam.dsl.SinkEffect
import beam.protocol._


final class Sink[I, R, U](val fun: PullUp[I, U] => R !! U):
  def flowFrom(stream: Stream[I, U]): R !! U = fun(stream.pull)
  def flowFrom[I2](pipe: Pipe[I2, I, U]): Sink[I2, R, U] = new Sink(pull => fun(pipe.fun(pull)))
  def <-<(stream: Stream[I, U]): R !! U = flowFrom(stream)
  def <-<[I2](pipe: Pipe[I2, I, U]): Sink[I2, R, U] = flowFrom(pipe)


object Sink:
  def compile[I, R, U](body: (fx: SinkEffect[I, R]) => R !! (U & fx.type)): Sink[I, R, U] =
    case object Fx extends SinkEffect[I, R]
    new Sink(up => body(Fx).handleWith(Fx.handler[U](up)))

  def fold[I, R](zero: R)(op: (R, I) => R): Sink[I, R, Any] =
    def loop(todo: PullUp[I, Any], accum: R !! Any): R !! Any =
      todo(true).flatMap:
        case PullDown.Stop => accum
        case PullDown.Emit(a, cc) => loop(cc, accum.map(op(_, a)))
    new Sink(loop(_, zero.pure_!!))

  def fold_!![I, R, U](zero: R)(op: (R, I) => R !! U): Sink[I, R, U] =
    def loop(todo: PullUp[I, U], accum: R !! U): R !! U =
      todo(true).flatMap:
        case PullDown.Stop => accum
        case PullDown.Emit(a, cc) => loop(cc, accum.flatMap(op(_, a)))
    new Sink(loop(_, zero.pure_!!))
