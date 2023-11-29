package beam
import turbolift.!!
import beam.internals.Step


opaque type Stream[+A, -U] = Stream.Underlying[A, U]


object Stream extends Stream_opaque:
  type Underlying[A, U] = Step[A, U] !! U

  inline def wrap[A, U](that: Underlying[A, U]): Stream[A, U] = that

  extension [A, U](thiz: Stream[A, U])
    inline def unwrap: Underlying[A, U] = thiz


  // doesnt work as extensions :(
  inline def unwrapFun[A, B, U](thiz: A => Stream[B, U]): A => Underlying[B, U] = thiz
  inline def unwrapFunEff[A, B, U, V](thiz: A => Stream[B, U] !! V): A => Underlying[B, U] !! V = thiz      
