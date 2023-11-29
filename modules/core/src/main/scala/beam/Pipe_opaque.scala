package beam
import turbolift.!!
import turbolift.Extensions._
import beam.effects.PipeEffect
import beam.internals.Step
import beam.Syntax._


private[beam] trait Pipe_opaque:
  def coroutine[I, O, U](body: (fx: PipeEffect[I, O]) => Unit !! (U & fx.type)): Pipe[I, O, U] =
    case object Fx extends PipeEffect[I, O]
    Pipe.wrap(stream => body(Fx).handleWith[U](Fx.handler[U](stream)).flattenAsStream)

  extension [I, O, U](thiz: Pipe[I, O, U])
    def from(stream: Stream[I, U]): Stream[O, U] = thiz.unwrap(stream)
    def from[I2](pipe: Pipe[I2, I, U]): Pipe[I2, O, U] = Pipe.wrap(stream => thiz.unwrap(pipe.unwrap(stream)))
    def through[O2](pipe: Pipe[O, O2, U]): Pipe[I, O2, U] = pipe.from(thiz)
    def through[R](sink: Sink[O, R, U]): Sink[I, R, U] = sink.from(thiz)
    def <-<(stream: Stream[I, U]): Stream[O, U] = from(stream)
    def <-<[I2](pipe: Pipe[I2, I, U]): Pipe[I2, O, U] = from(pipe)
    def >->[O2](pipe: Pipe[O, O2, U]): Pipe[I, O2, U] = through(pipe)
    def >->[R](sink: Sink[O, R, U]): Sink[I, R, U] = through(sink)
