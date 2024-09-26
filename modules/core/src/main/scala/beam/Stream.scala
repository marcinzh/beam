package beam
import turbolift.!!
import turbolift.Extensions._
import Stream.{Emit, Empty}
import Syntax._


sealed abstract class Stream[+A, -U]:

  final def decons: Option[(Chunk[A], Stream[A, U] !! U)] =
    this match
      case Emit(aa, k) => Some((aa, k))
      case Empty => None


  final def deconsSkip: Option[(Chunk[A], Stream[A, U] !! U)] !! U =
    this match
      case Emit(aa, k) =>
        if aa.isEmpty then
          k.flatMap(_.deconsSkip)
        else
          !!.pure(Some((aa, k)))
      case Empty => !!.none


  final def deconsChunk: (Chunk[A], Stream[A, U] !! U) =
    this match
      case Emit(aa, k) => (aa, k)
      case Empty => (Chunk(), Stream.emptyEff)


  //---------- add & remove ----------


  final def head: A !! U = headOption.map(_.get)


  final def headOption: Option[A] !! U = deconsSkip.map(_.map(_._1.head))


  final def tail: Stream[A, U] = drop(1)


  final def take(count: Long): Stream[A, U] =
    if count > 0 then
      this match
        case Emit(aa, k) =>
          val n = aa.size
          if count <= n then
            val count2 = count.toInt
            Emit(aa.take(count2), Stream.emptyEff)
          else
            Emit(aa, k.map(_.take(count - n)))
        case Empty => Empty
    else
      Empty


  final def drop(count: Long): Stream[A, U] =
    if count > 0 then
      this match
        case Emit(aa, k) =>
          val n = aa.size
          if count <= n then
            val count2 = count.toInt
            Emit(aa.drop(count2), k)
          else
            Emit(Chunk(), k.map(_.drop(count - n)))
        case Empty => Empty
    else
      this


  final def ++[B >: A, V <: U](that: => Stream[B, V]): Stream[B, V] = concat(that)


  final def concat[B >: A, V <: U](that: =>Stream[B, V]): Stream[B, V] =
    this match
      case Emit(aa, k) => Emit(aa, k.map(_.concat(that)))
      case Empty => that


  //---------- map & foreach ----------


  final def map[B](f: A => B): Stream[B, U] = mapChunk(_.map(f))

  final def mapEff[B, V <: U](f: A => B !! V): Stream[B, V] = mapChunkEff(_.mapEff(f))

  final def flatMap[B, V <: U](f: A => Stream[B, V]): Stream[B, V] =
    this match
      case Emit(aa, k) => Stream.fromStreams(aa.iterator.map(f)) ++ k.map(_.flatMap(f)).flattenAsStream
      case Empty => Empty


  final def flatMapEff[B, V <: U](f: A => Stream[B, V] !! V): Stream[B, V] =
    this match
      case Emit(aa, k) => Stream.fromStreamsEff(aa.iterator.map(f)) ++ k.map(_.flatMapEff(f)).flattenAsStream
      case Empty => Empty


  final def foreach(f: A => Unit): Stream[A, U] =
    this match
      case Emit(aa, k) => !!.impure(aa.foreach(f)).as(Emit(aa, k.map(_.foreach(f)))).flattenAsStream
      case Empty => Empty


  final def foreachEff[V <: U](f: A => Unit !! V): Stream[A, V] =
    this match
      case Emit(aa, k) => (aa.foreachEff(f).as(Emit(aa, k.map(_.foreachEff(f))))).flattenAsStream
      case Empty => Empty


  final def forsome(f: PartialFunction[A, Unit]): Stream[A, U] =
    this match
      case Emit(aa, k) => !!.impure(aa.forsome(f)).as(Emit(aa, k.map(_.foreach(f)))).flattenAsStream
      case Empty => Empty


  final def forsomeEff[V <: U](f: PartialFunction[A, Unit !! V]): Stream[A, V] =
    this match
      case Emit(aa, k) => (aa.forsomeEff(f).as(Emit(aa, k.map(_.foreachEff(f))))).flattenAsStream
      case Empty => Empty


  //---------- filter & collect ----------


  final def filter(f: A => Boolean): Stream[A, U] = mapChunk(_.filter(f))

  final def filterNot(f: A => Boolean): Stream[A, U] = mapChunk(_.filterNot(f))

  final def filterEff[V <: U](f: A => Boolean !! V): Stream[A, V] = mapChunkEff(_.filterEff(f))

  final def filterNotEff[V <: U](f: A => Boolean !! V): Stream[A, V] = mapChunkEff(_.filterNotEff(f))

  final def mapFilter[B](f: A => Option[B]): Stream[B, U] = mapChunk(_.mapFilter2(f))

  final def mapFilterEff[B, V <: U](f: A => Option[B] !! V): Stream[B, V] = mapChunkEff(_.mapFilterEff(f))

  final def collect[B](f: PartialFunction[A, B]): Stream[B, U] = mapChunk(_.collect(f))

  final def collectEff[B, V <: U](f: PartialFunction[A, B !! V]): Stream[B, V] = mapChunkEff(_.collectEff(f))


  //---------- fold ----------


  final def foldLeft[B](zero: B)(op: (B, A) => B): B !! U =
    this match
      case Emit(aa, k) => val bb = aa.foldLeft(zero)(op); k.flatMap(_.foldLeft(bb)(op))
      case Empty => zero.pure_!!


  final def foldLeftEff[B, V <: U](zero: B)(op: (B, A) => B !! V): B !! V =
    this match
      case Emit(aa, k) => aa.foldLeftEff(zero)(op).flatMap(bb => k.flatMap(_.foldLeftEff(bb)(op)))
      case Empty => zero.pure_!!


  final def reduceOption[B >: A](f: (B, B) => B): Option[B] !! U =
    foldLeft[Option[B]](None):
      case (None, a) => Some(a)
      case (Some(b), a) => Some(f(b, a))


  final def reduceOptionEff[B >: A, V <: U](f: (B, B) => B !! V): Option[B] !! V =
    foldLeftEff[Option[B], V](None):
      case (None, a) => Some(a).pure_!!
      case (Some(b), a) => f(b, a).map(Some(_))


  final def reduce[B >: A](f: (B, B) => B): B !! U = reduceOption(f).map(_.get)

  final def toVector: Vector[A] !! U = foldLeft(Vector[A]())(_ :+ _)


  final def toList: List[A] !! U =
    val lb = new collection.mutable.ListBuffer[A]
    def loop(s: Stream[A, U]): List[A] !! U =
      s match
        case Emit(aa, k) => !!.impure(lb ++= aa) &&! k.flatMap(loop)
        case Empty => !!.impure(lb.toList)
    loop(this)


  final def drain: Unit !! U =
    this match
      case Emit(_, k) => k.flatMap(_.drain)
      case Empty => !!.unit


  //---------- chunkwise ----------


  final def mapChunk[B](f: Chunk[A] => Chunk[B]): Stream[B, U] =
    this match
      case Emit(aa, k) => Emit(f(aa), k.map(_.mapChunk(f)))
      case Empty => Empty


  final def mapChunkEff[B, V <: U](f: Chunk[A] => Chunk[B] !! V): Stream[B, V] =
    this match
      case Emit(aa, k) => f(aa).map(Emit(_, k.map(_.mapChunkEff(f)))).flattenAsStream
      case Empty => Empty



object Stream:
  private case object Empty extends Stream[Nothing, Any]
  private final case class Emit[A, U](chunk: Chunk[A], next: Stream[A, U] !! U) extends Stream[A, U]


  //---------- Low level ----------


  def consLazy[A, U](a: A, s: => Stream[A, U]): Stream[A, U] = new Emit(Chunk(a), !!.impure(s))
  def consEff[A, U](a: A, s: Stream[A, U] !! U): Stream[A, U] = new Emit(Chunk(a), s)
  def consChunkLazy[A, U](aa: Chunk[A], s: => Stream[A, U]): Stream[A, U] = new Emit(aa, !!.impure(s))
  def consChunkEff[A, U](aa: Chunk[A], s: Stream[A, U] !! U): Stream[A, U] = new Emit(aa, s)
  def delay[A, U](s: Stream[A, U] !! U): Stream[A, U] = new Emit(Chunk(), s)


  //---------- Basic ctors ----------


  val empty: Stream[Nothing, Any] = Empty

  val emptyEff: Stream[Nothing, Any] !! Any = !!.pure(empty)

  val unit: Stream[Unit, Any] = singleton(())

  def apply(): Stream[Nothing, Any] = empty
  
  def apply[A](a: A): Stream[A, Any] = singleton(a)

  def apply[A](a: A, as: A*): Stream[A, Any] = a ?:: from(as)

  def eval[A, U](aa: A !! U): Stream[A, U] = singletonEff(aa)

  def singleton[A](a: A): Stream[A, Any] = a !:: emptyEff

  def singletonEff[A, U](aa: A !! U): Stream[A, U] = aa.map(singleton).flattenAsStream


  //---------- Loop ctors ----------


  def repeat[A](a: A): Stream[A, Any] = Chunk.repeat(a) ?::: repeat(a)

  def repeatEff[A, U](aa: A !! U): Stream[A, U] = aa.map(_ ?:: repeatEff(aa)).flattenAsStream
  
  def iterate[A](a: A)(f: A => A): Stream[A, Any] = a ?:: iterate(f(a))(f)

  def iterateEff[A, U](a: A)(f: A => A !! U): Stream[A, U] = a !:: f(a).map(iterateEff(_)(f))


  def unfold[A, S](s: S)(f: S => Option[(A, S)]): Stream[A, Any] =
    def loop(s: S): Stream[A, Any] =
      f(s) match
        case Some((a, s2)) => a ?:: loop(s2)
        case None => Empty
    loop(s)


  def unfoldEff[A, S, U](s: S)(f: S => Option[(A, S)] !! U): Stream[A, U] =
    def loop(s: S): Stream[A, U] !! U =
      f(s).map:
        case Some((a, s2)) => a !:: loop(s2)
        case None => Empty
    loop(s).flattenAsStream


  def range[A: Numeric](start: A, endExclusive: A, step: A): Stream[A, Any] =
    val N = summon[Numeric[A]]
    def loop(a: A): Stream[A, Any] =
      if N.lt(a, endExclusive) then
        a ?:: loop(N.plus(a, step))
      else
        Empty
    loop(start)


  def from[A](aa: IterableOnce[A]): Stream[A, Any] =
    val it = aa.iterator
    def loop(): Stream[A, Any] =
      if it.hasNext then
        it.next() ?:: loop()
      else
        Empty
    loop()
  

  def fromChunks[A](aa: IterableOnce[Chunk[A]]): Stream[A, Any] =
    val it = aa.iterator
    def loop(): Stream[A, Any] =
      if it.hasNext then
        Emit(it.next(), !!.impure(loop()))
      else
        Empty
    loop()


  def fromStreams[A, U](aa: IterableOnce[Stream[A, U]]): Stream[A, U] =
    val it = aa.iterator
    def loop(s: Stream[A, U]): Stream[A, U] =
      s match
        case Emit(aa, k) => Emit(aa, k.map(loop))
        case Empty =>
          if it.hasNext then
            loop(it.next())
          else
            Empty
    loop(Empty)


  def fromStreamsEff[A, U](aa: IterableOnce[Stream[A, U] !! U]): Stream[A, U] =
    val it = aa.iterator
    def loop(s: Stream[A, U]): Stream[A, U] =
      s match
        case Emit(aa, k) => Emit(aa, k.map(loop))
        case Empty =>
          if it.hasNext then
            it.next().map(loop).flattenAsStream
          else
            Empty
    loop(Empty)
