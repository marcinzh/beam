package beam
import turbolift.!!
import turbolift.Extensions._
import beam.internals.StreamImpl


object Syntax:
  extension [U](comp: Unit !! U)
    def asStream[A, V](fx: SourceEffect[A])(using (fx.type & V) =:= U): Stream[A, V] =
      StreamImpl(fx)(comp.castEv[V & fx.type])

  extension [A](fx: SourceEffect[A])
    def emptyStream: Stream[A, Any] = StreamImpl[A, Any](fx)(!!.unit)

  extension [A](a: A)
    def singleton: Stream[A, Any] = Stream.singleton(a)

    def !::[B >: A, U](stream: Stream[B, U]): Stream[B, U] =
      val stream2 = stream.asImpl
      (stream2.Fx.emit(a) &&! stream2.compute).asStream(stream2.Fx)
