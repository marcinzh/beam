package beam
import turbolift.!!
import turbolift.Extensions._


//@#@TODO
type Chunk[A] = Vector[A]


object Chunk:
  def empty: Chunk[Nothing] = Vector()
  def apply(): Chunk[Nothing] = Vector()
  def apply[A](value: A): Chunk[A] = Vector(value)
  def repeat[A](value: A): Chunk[A] = apply(value)


//@#@TODO patch `turbolift.Extensions`
extension [A](thiz: Chunk[A])
  def mapFilter2[B](f: A => Option[B]): Chunk[B] = thiz.flatMap(f)
  def filterNotEff[U](f: A => Boolean !! U): Chunk[A] !! U = thiz.filterEff(a => f(a).map(!_))
