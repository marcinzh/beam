package beam
import turbolift.!!
import beam.internals.Step


object Syntax:
  extension [A](thiz: A)
    inline def ::![B >: A, U](that: Stream[B, U]): Stream[B, U] =
      Stream.wrap(!!.pure(Step.Emit(thiz, that.unwrap)))

    inline def ::!?[B >: A, U](that: => Stream[B, U]): Stream[B, U] =
      Stream.wrap(!!.pure(Step.Emit(thiz, !!.impureEff(that.unwrap))))

  extension [A, U, V <: U](thiz: Stream[A, U] !! V)
    def flattenAsStream: Stream[A, V] = Stream.wrap(thiz.flatMap(_.unwrap))

  extension [I, R, U](thiz: Stream[I, U] => R !! U)
    def asSink: Sink[I, R, U] = new Sink(thiz)

  extension [I, O, U](thiz: Stream[I, U] => Stream[O, U])
    def asPipe: Pipe[I, O, U] = Pipe.wrap(thiz)
