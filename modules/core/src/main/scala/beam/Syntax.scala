package beam
import turbolift.!!


object Syntax:
  extension [A](thiz: A)
    inline def ?::[B >: A, U](that: => Stream[B, U]): Stream[B, U] = Stream.consLazy(thiz, that)
    inline def !::[B >: A, U](that: Stream[B, U] !! U): Stream[B, U] = Stream.consEff(thiz, that)

  extension [A](thiz: Chunk[A])
    inline def ?:::[B >: A, U](that: => Stream[B, U]): Stream[B, U] = Stream.consChunkLazy(thiz, that)
    inline def !:::[B >: A, U](that: Stream[B, U] !! U): Stream[B, U] = Stream.consChunkEff(thiz, that)


  extension [A, U](thiz: Stream[A, U] !! U)
    def flattenAsStream: Stream[A, U] = Stream.delay(thiz)
