package beam
import scala.math.Numeric
import turbolift.!!
import turbolift.Extensions._
import beam.internals.Step
import beam.internals.StepOps
import beam.effects.StreamEffect
import beam.Syntax._


private[beam] trait Stream_opaque:
  def empty: Stream[Nothing, Any] = Stream.wrap(Step.endPure)
  val unit: Stream[Unit, Any] = singleton(()) 
  def singleton[A](a: A): Stream[A, Any] = Stream.wrap(StepOps.singleton(a))
  def singletonEff[A, U](aa: A !! U): Stream[A, U] = Stream.wrap(StepOps.singletonEff(aa))

  def apply[A](as: A*): Stream[A, Any] = from(as)
  def from[A](as: IterableOnce[A]): Stream[A, Any] = Stream.wrap(StepOps.fromIterator(as.iterator))

  def range[A: Numeric](start: A, endExclusive: A, step: A): Stream[A, Any] = Stream.wrap(StepOps.range(start, endExclusive, step))
  def range[A: Numeric](start: A, endExclusive: A): Stream[A, Any] = range(start, endExclusive, summon[Numeric[A]].fromInt(1)) 

  def unfold[A, S](s: S)(f: S => Option[(A, S)]): Stream[A, Any] = Stream.wrap(StepOps.unfold(s)(f))
  def unfoldEff[A, S, U](s: S)(f: S => Option[(A, S)] !! U): Stream[A, U] = Stream.wrap(StepOps.unfoldEff(s)(f))

  def repeat[A](a: A): Stream[A, Any] = Stream.wrap(StepOps.repeat(a))
  def repeatEff[A, U](aa: A !! U): Stream[A, U] = Stream.wrap(StepOps.repeatEff(aa))

  def iterate[A](a: A)(f: A => A): Stream[A, Any] = Stream.wrap(StepOps.iterate(a)(f))
  def iterateEff[A, U](a: A)(f: A => A !! U): Stream[A, U] = Stream.wrap(StepOps.iterateEff(a)(f))

  def coroutine[O, U](body: (fx: StreamEffect[O]) => Unit !! (U & fx.type)): Stream[O, U] =
    case object Fx extends StreamEffect[O]
    body(Fx).handleWith[U](Fx.handler[U]).flattenAsStream


  extension [A, U](thiz: Stream[A, U])
    private inline def rewrap[B, V](inline f: Stream.Underlying[A, U] => Stream.Underlying[B, V]): Stream[B, V] =
      Stream.wrap(f(thiz.unwrap))

    //========== combine ==========

    def through[B, V <: U](pipe: Pipe[A, B, V]): Stream[B, V] = pipe.from(thiz)
    def into[B, V <: U](sink: Sink[A, B, V]): B !! V = sink.from(thiz)
    def >->[B](pipe: Pipe[A, B, U]): Stream[B, U] = through(pipe)
    def >->[B](sink: Sink[A, B, U]): B !! U = into(sink)


    //========== add & remove ==========

    def append[B >: A, V <: U](that: Stream[B, V]): Stream[B, V] = rewrap(StepOps.append(_, that.unwrap))

    def head: Option[A] !! U = StepOps.head(thiz.unwrap)

    def tail: Stream[A, U] = rewrap(StepOps.tail)

    def take(count: Long): Stream[A, U] = rewrap(StepOps.take(_, count))

    def drop(count: Long): Stream[A, U] = rewrap(StepOps.drop(_, count))

    //========== map & foreach ==========

    def map[B](f: A => B): Stream[B, U] = rewrap(StepOps.map(_, f))

    def mapEff[B, V <: U](f: A => B !! V): Stream[B, V] = rewrap(StepOps.mapEff(_, f))

    def flatMap[B, V <: U](f: A => Stream[B, V]): Stream[B, V] = rewrap(StepOps.flatMap(_, Stream.unwrapFun(f)))

    def flatMapEff[B, V <: U](f: A => Stream[B, V] !! V): Stream[B, V] = rewrap(StepOps.flatMapEff(_, Stream.unwrapFunEff(f)))

    def foreach(f: A => Unit): Stream[A, U] = rewrap(StepOps.foreach(_, f))

    def foreachEff[V <: U](f: A => Unit !! V): Stream[A, V] = rewrap(StepOps.foreachEff(_, f))

    def forsome(f: PartialFunction[A, Unit]): Stream[A, U] = rewrap(StepOps.forsome(_, f))

    def forsomeEff[V <: U](f: PartialFunction[A, Unit !! V]): Stream[A, V] = rewrap(StepOps.forsomeEff(_, f))

    //========== filter & collect ==========

    def filter(f: A => Boolean): Stream[A, U] = rewrap(StepOps.filter(_, f))

    def filterNot(f: A => Boolean): Stream[A, U] = rewrap(StepOps.filterNot(_, f))

    def filterEff[V <: U](f: A => Boolean !! V): Stream[A, V] = rewrap(StepOps.filterEff(_, f))

    def filterNotEff[V <: U](f: A => Boolean !! V): Stream[A, V] = rewrap(StepOps.filterNotEff(_, f))

    def mapFilter[B](f: A => Option[B]): Stream[B, U] = rewrap(StepOps.mapFilter(_, f))

    def mapFilterEff[B, V <: U](f: A => Option[B] !! V): Stream[B, V] = rewrap(StepOps.mapFilterEff(_, f))

    def collect[B](f: PartialFunction[A, B]): Stream[B, U] = rewrap(StepOps.collect(_, f))

    def collectEff[B, V <: U](f: PartialFunction[A, B !! V]): Stream[B, V] = rewrap(StepOps.collectEff(_, f))

    def filterWithPrevious(f: (A, A) => Boolean): Stream[A, U] = rewrap(StepOps.filterWithPrevious(_, f))

    def filterWithPreviousEff[V <: U](f: (A, A) => Boolean !! V): Stream[A, V] = rewrap(StepOps.filterWithPreviousEff(_, f))

    //========== fold ==========

    def fold[B](zero: B)(op: (B, A) => B): B !! U = StepOps.fold(thiz.unwrap, zero, op)

    def foldEff[B, V <: U](zero: B)(op: (B, A) => B !! V): B !! V = StepOps.foldEff(thiz.unwrap, zero, op)

    def toVector: Vector[A] !! U = fold(Vector[A]())(_ :+ _)

    def drain: Unit !! U = StepOps.drain(thiz.unwrap)
