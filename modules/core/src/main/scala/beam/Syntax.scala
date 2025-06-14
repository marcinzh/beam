package beam
import turbolift.!!
import turbolift.Extensions._
import turbolift.data.Chunk
import beam.internals.{UnchunkedStream, ChunkedStream}


object Syntax:
  extension [U](comp: Unit !! U)
    def asUnchunkedStream[A, V](fx: SourceEffect[A])(using (fx.type & V) =:= U): Stream[A, V] =
      UnchunkedStream(fx)(comp.castEv[V & fx.type])

    def asChunkedStream[A, V](fx: SourceEffect[Chunk[A]])(using (fx.type & V) =:= U): Stream[A, V] =
      ChunkedStream(fx)(comp.castEv[V & fx.type])


  extension [A](fx: SourceEffect[Chunk[A]])
    def wrapChunked[U](comp: Unit !! (U & fx.type)): Stream[A, U] = ChunkedStream(fx)(comp)

    def emptyChunked: Stream[A, Any] = wrapChunked(!!.unit)


  extension [A](a: A)
    def singleton: Stream[A, Any] = Stream.singleton(a)

    def !::[B >: A, U](stream: Stream[B, U]): Stream[B, U] = stream.prepend(a)

  extension [A, U](comp: Stream[A, U] !! U)
    def flattenAsStream: Stream[A, U] = Stream.flattenStream(comp)
