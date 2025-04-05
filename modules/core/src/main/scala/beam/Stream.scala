package beam
import scala.reflect.ClassTag
import scala.collection.immutable.Queue
import turbolift.!!
import turbolift.Extensions._
import turbolift.effects.IO
import turbolift.io.{Channel, Warp}
import beam.internals.StreamImpl
import Syntax._


sealed abstract class Stream[+A, -U]:
  // ------- map -------

  def map[B](f: A => B): Stream[B, U]
  def mapEff[B, V <: U](f: A => B !! V): Stream[B, V]
  def flatMap[B, V <: U](f: A => Stream[B, V]): Stream[B, V]
  def flatMapEff[B, V <: U](f: A => Stream[B, V] !! V): Stream[B, V]
  def tap(f: A => Unit): Stream[A, U]
  def tapEff[V <: U](f: A => Unit !! V): Stream[A, V]
  final def mapConcat[B](f: A => Iterable[B]): Stream[B, U] = flatMap(a => Stream.from(f(a)))
  final def mapConcatEff[B, V <: U](f: A => Iterable[B] !! V): Stream[B, V] = flatMapEff(a => f(a).map(Stream.from))

  // ------- filter -------

  def filter(f: A => Boolean): Stream[A, U]
  def filterEff[V <: U](f: A => Boolean !! V): Stream[A, V]
  def mapFilter[B](f: A => Option[B]): Stream[B, U]
  def mapFilterEff[B, V <: U](f: A => Option[B] !! V): Stream[B, V]
  def collect[B](f: PartialFunction[A, B]): Stream[B, U]
  def collectEff[B, V <: U](f: PartialFunction[A, B !! V]): Stream[B, V]

  // ------- combine -------

  def concat[B >: A, V <: U](that: Stream[B, V]): Stream[B, V]
  final def ++[B >: A, V <: U](that: Stream[B, V]): Stream[B, V] = concat(that)
  final def flatten[B, V <: U](using ev: A <:< Stream[B, V]): Stream[B, V] = flatMap(ev)

  final def zip[B, V <: U](that: Stream[B, V]): Stream[(A, B), V] = zipWith(that)((_, _))
  def zipWith[B, C, V <: U](that: Stream[B, V])(f: (A, B) => C): Stream[C, V]
  def zipWithEff[B, C, V <: U](that: Stream[B, V])(f: (A, B) => C !! V): Stream[C, V]

  def zipWithIndex: Stream[(A, Long), U]
  def intersperse[B >: A](separator: B): Stream[B, U]
  def intersperse[B >: A](first: B, separator: B, last: B): Stream[B, U]

  def interleave[B >: A, V <: U](that: Stream[B, V]): Stream[B, V]
  def mergeSorted[B >: A, V <: U](that: Stream[B, V])(using ev: Ordering[B]): Stream[B, V]
  final def merge[B >: A, V <: U & IO](that: Stream[B, V]): Stream[B, V] = mergeWith(that)(_.merge)
  final def mergeEither[B, V <: U & IO](that: Stream[B, V]): Stream[Either[A, B], V] = mergeWith(that)(x => x)
  final def mergeWith[B, C, V <: U & IO](that: Stream[B, V])(f: Either[A, B] => C): Stream[C, V] = mergeWithEff(that)(f.andThen(!!.pure))
  final def mergeWithEff[B, C, V <: U & IO](that: Stream[B, V])(f: Either[A, B] => C !! V): Stream[C, V] = Stream.mergeWithEff(this, that)(f)

  // ------- prefix -------

  def prepend[B >: A](value: B): Stream[B, U]
  def decons: Option[(A, Stream[A, U])] !! U
  final def head: A !! U = headOption.map(_.get)
  final def headOption: Option[A] !! U = decons.map(_.map(_._1))
  final def tail: Stream[A, U] = drop(1)

  def take(count: Long): Stream[A, U]
  def drop(count: Long): Stream[A, U]
  def takeWhile(f: A => Boolean): Stream[A, U]
  def takeWhileEff[V <: U](f: A => Boolean !! V): Stream[A, V]
  def dropWhile(f: A => Boolean): Stream[A, U]
  def dropWhileEff[V <: U](f: A => Boolean !! V): Stream[A, V]

  // ------- split -------

  def split[A2 >: A](separator: A2): Stream[Vector[A], U]
  def splitAt(count: Int): (Vector[A], Stream[A, U]) !! U
  def splitWhere(f: A => Boolean): Stream[Vector[A], U]
  def splitWhereEff[V <: U](f: A => Boolean !! V): Stream[Vector[A], V]

  // ------- history -------

  def filterWithPrevious(f: (A, A) => Boolean): Stream[A, U]
  def filterWithPreviousEff[V <: U](f: (A, A) => Boolean !! V): Stream[A, V]
  final def changes: Stream[A, U] = filterWithPrevious(_ != _)
  def changesBy[B](f: A => B): Stream[A, U]
  def changesByEff[B, V <: U](f: A => B !! V): Stream[A, V]

  def window(size: Int, step: Int): Stream[Queue[A], U]
  def window(size: Int): Stream[Queue[A], U] = window(size, 1)

  // ------- misc -------

  def scanLeft[B](initial: B)(f: (B, A) => B): Stream[B, U]
  def scanLeftEff[B, V <: U](initial: B)(f: (B, A) => B !! V): Stream[B, V]

  // ------- compile -------

  def drain: Unit !! U
  def abort: Unit !! U

  def foreach(f: A => Unit): Unit !! U
  def foreachEff[V <: U](f: A => Unit !! V): Unit !! V
  final def forsome(f: PartialFunction[A, Unit]): Unit !! U = foreach(a => f.applyOrElse(a, _ => ()))
  final def forsomeEff[V <: U](f: PartialFunction[A, Unit !! V]): Unit !! V = foreachEff(a => f.applyOrElse(a, _ => !!.unit))

  def foldLeft[B](initial: B)(f: (B, A) => B): B !! U
  def foldLeftEff[B, V <: U](initial: B)(f: (B, A) => B !! V): B !! V

  final def reduce[B >: A](f: (B, B) => B): B !! U = reduceOption(f).map(_.get)
  final def reduceEff[B >: A, V <: U](f: (B, B) => B !! V): B !! V = reduceOptionEff(f).map(_.get)

  final def reduceOption[B >: A](f: (B, B) => B): Option[B] !! U =
    foldLeft[Option[B]](None):
      case (None, a) => Some(a)
      case (Some(b), a) => Some(f(b, a))

  final def reduceOptionEff[B >: A, V <: U](f: (B, B) => B !! V): Option[B] !! V =
    foldLeftEff[Option[B], V](None):
      case (None, a) => Some(a).pure_!!
      case (Some(b), a) => f(b, a).map(Some(_))

  final def toVector: Vector[A] !! U = foldLeft(Vector[A]())(_ :+ _)

  final def toList: List[A] !! U =
    for
      buf <- !!.impure(new collection.mutable.ListBuffer[A])
      _ <- foreach(buf += _)
    yield buf.toList

  final def toArray[B >: A: ClassTag]: Array[B] !! U =
    for
      buf <- !!.impure(new collection.mutable.ArrayBuffer[B])
      _ <- foreach(buf += _)
    yield buf.toArray


  // ------- internals -------

  def relayTo[B >: A](Fx2: SourceEffect[B]): Unit !! (U & Fx2.type)
  def relayToWhileLessThen[B >: A](Fx2: SourceEffect[B], b: B)(using ev: Ordering[B]): Option[(A, Stream[A, U])] !! (U & Fx2.type)
  final def asImpl[B >: A, V <: U]: StreamImpl[B, V] = asInstanceOf[StreamImpl[B, V]]


object Stream:
  import SourceEffect.{FxNothing, FxUnit}
  abstract class Unsealed[A, U] extends Stream[A, U]
  type Decons[A, U] = Option[(A, Stream[A, U])]

  def empty[A]: Stream[Nothing, Any] = nothing
  def emptyEff[U](comp: Unit !! U): Stream[Nothing, U] = StreamImpl(FxNothing)(comp)
  val nothing: Stream[Nothing, Any] = FxNothing.emptyStream
  val unit: Stream[Unit, Any] = FxUnit.emit(()).asStream(FxUnit)
  def apply[A](as: A*): Stream[A, Any] = from(as)


  def singleton[A](a: A): Stream[A, Any] =
    case object Fx extends SourceEffect[A]
    Fx.emit(a).asStream(Fx)


  def singletonEff[A, U](comp: A !! U): Stream[A, U] =
    case object Fx extends SourceEffect[A]
    comp.flatMap(Fx.emit).asStream(Fx)


  def from[A](aa: IterableOnce[A]): Stream[A, Any] =
    case object Fx extends SourceEffect[A]
    val it = aa.iterator
    def loop(): Unit !! Fx.type =
      if it.hasNext then
        Fx.emit(it.next()) &&! loop()
      else
        !!.unit
    loop().asStream(Fx)


  def range[A: Numeric](start: A, endExclusive: A, step: A): Stream[A, Any] =
    case object Fx extends SourceEffect[A]
    val N = summon[Numeric[A]]
    def loop(a: A): Unit !! Fx.type =
      if N.lt(a, endExclusive) then
        Fx.emit(a) &&! loop(N.plus(a, step))
      else
        !!.unit
    loop(start).asStream(Fx)


  def repeat[A](value: A): Stream[A, Any] =
    case object Fx extends SourceEffect[A]
    def loop: Unit !! Fx.type = Fx.emit(value) &&! loop
    loop.asStream(Fx)


  def repeatEff[A, U](comp: A !! U): Stream[A, U] =
    case object Fx extends SourceEffect[A]
    def loop: Unit !! (Fx.type & U) = comp.flatMap(Fx.emit) &&! loop
    loop.asStream(Fx)

  
  def iterate[A](initial: A)(f: A => A): Stream[A, Any] =
    case object Fx extends SourceEffect[A]
    def loop(a: A): Unit !! Fx.type = Fx.emit(a) &&! loop(f(a))
    loop(initial).asStream(Fx)


  def iterateEff[A, U](initial: A)(f: A => A !! U): Stream[A, U] =
    case object Fx extends SourceEffect[A]
    def loop(a: A): Unit !! (Fx.type & U) = Fx.emit(a) &&! f(a).flatMap(loop)
    loop(initial).asStream(Fx)


  def unfold[A, S](s: S)(f: S => Option[(A, S)]): Stream[A, Any] =
    case object Fx extends SourceEffect[A]
    def loop(s: S): Unit !! Fx.type =
      f(s) match
        case Some((a, s2)) => Fx.emit(a) &&! loop(s2)
        case None => !!.unit
    loop(s).asStream(Fx)


  def unfoldEff[A, S, U](s: S)(f: S => Option[(A, S)] !! U): Stream[A, U] =
    case object Fx extends SourceEffect[A]
    def loop(s: S): Unit !! (Fx.type & U) =
      f(s).map:
        case Some((a, s2)) => Fx.emit(a) &&! loop(s2)
        case None => !!.unit
    loop(s).asStream(Fx)


  def flattenStream[A, U](comp: Stream[A, U] !! U): Stream[A, U] =
    case object Fx extends SourceEffect[A]
    comp.flatMap(_.relayTo(Fx)).asStream(Fx)


  private enum MergeEvent[+A, +B]:
    case LeftEmit(a: A)
    case RightEmit(b: B)
    case LeftDone
    case RightDone


  def mergeWithEff[A, B, C, U <: IO](lhs: Stream[A, U], rhs: Stream[B, U])(f: Either[A, B] => C !! U): Stream[C, U] =
    case object Fx extends SourceEffect[C]
    Channel.synchronous[MergeEvent[A, B]].flatMap: channel =>
      Warp:
        (
          lhs.foreachEff(a => channel.put(MergeEvent.LeftEmit(a))).&&!(channel.put(MergeEvent.LeftDone)).fork **!
          rhs.foreachEff(b => channel.put(MergeEvent.RightEmit(b))).&&!(channel.put(MergeEvent.RightDone)).fork
        ).flatMap: (fibA, fibB) =>
          def loop(haltOnLeft: Boolean, haltOnRight: Boolean): Unit !! (Fx.type & U) =
            channel.get.flatMap:
              case MergeEvent.LeftEmit(a) => f(Left(a)).flatMap(Fx.emit) &&! loop(haltOnLeft, haltOnRight)
              case MergeEvent.RightEmit(b) => f(Right(b)).flatMap(Fx.emit) &&! loop(haltOnLeft, haltOnRight)
              case MergeEvent.LeftDone => fibA.join &&! !!.when(!haltOnLeft)(loop(false, true))
              case MergeEvent.RightDone => fibB.join &&! !!.when(!haltOnRight)(loop(true, false))
          loop(false, false)
    .asStream(Fx)
