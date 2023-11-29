package beam
import turbolift.!!
import turbolift.Extensions._
import beam.effects.PipeEffect
import beam.internals.Step


opaque type Pipe[-I, +O, U] = Pipe.Underlying[I, O, U]


object Pipe extends Pipe_opaque:
  type Underlying[I, O, U] = Stream[I, U] => Stream[O, U]

  inline def wrap[I, O, U](that: Underlying[I, O, U]): Pipe[I, O, U] = that

  extension [I, O, U](thiz: Pipe[I, O, U])
    inline def unwrap: Underlying[I, O, U] = thiz


