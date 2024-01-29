package beam.internals
import scala.math.Numeric
import turbolift.!!
import turbolift.Extensions._
import Step.Syntax._


private[beam] object StepOps:
  type NextStep[A, U] = Step[A, U] !! U


  //========== construct ==========


  def singleton[A](a: A): NextStep[A, Any] = a ::! Step.endPure
  def singletonEff[A, U](aa: A !! U): NextStep[A, U] = aa.flatMap(_ ::! Step.endPure)


  def unfold[A, S](s: S)(f: S => Option[(A, S)]): NextStep[A, Any] =
    def loop(s: S): NextStep[A, Any] =
      f(s) match
        case Some((a, s2)) => a ::!? loop(s2)
        case None => Step.endPure
    loop(s)


  def unfoldEff[A, S, U](s: S)(f: S => Option[(A, S)] !! U): NextStep[A, U] =
    def loop(s: S): NextStep[A, U] =
      f(s).flatMap:
        case Some((a, s2)) => a ::! loop(s2)
        case None => Step.endPure
    loop(s)


  def repeat[A](a: A): NextStep[A, Any] = a ::!? repeat(a)
  def repeatEff[A, U](aa: A !! U): NextStep[A, U] = aa.flatMap(_ ::! repeatEff(aa))
  def iterate[A](a: A)(f: A => A): NextStep[A, Any] = a ::!? iterate(f(a))(f)
  def iterateEff[A, U](a: A)(f: A => A !! U): NextStep[A, U] = a ::! f(a).flatMap(iterateEff(_)(f))


  def fromIterator[A](it: Iterator[A]): NextStep[A, Any] =
    def loop(): NextStep[A, Any] =
      !!.impure:
        if it.hasNext then
          Step.Emit(it.next(), loop())
        else
          Step.End
    loop()


  def range[A: Numeric](start: A, endExclusive: A, step: A): NextStep[A, Any] =
    val N = summon[Numeric[A]]
    def loop(a: A): NextStep[A, Any] =
      if N.lt(a, endExclusive) then
        a ::! loop(N.plus(a, step))
      else
        Step.endPure
    loop(start)


  //========== add & remove ==========


  def append[A, B >: A, U, V <: U](thiz: NextStep[A, U], that: NextStep[B, V]): NextStep[B, V] =
    thiz.flatMap:
      case Step.Emit(a, nx) => a ::! append(nx, that)
      case Step.End => that


  def head[A, U](thiz: NextStep[A, U]): Option[A] !! U =
    thiz.flatMap:
      case Step.Emit(a, _) => Some(a).pure_!!
      case Step.End => !!.none


  def tail[A, U](thiz: NextStep[A, U]): NextStep[A, U] =
    thiz.decons: (_, nx) =>
      nx


  def take[A, U](thiz: NextStep[A, U], count: Long): NextStep[A, U] =
    if count > 0 then
      thiz.decons: (a, nx) =>
        a ::! take(nx, count - 1)
    else
      Step.endPure


  def drop[A, U](thiz: NextStep[A, U], count: Long): NextStep[A, U] =
    if count > 0 then
      thiz.decons: (a, nx) =>
        drop(nx, count - 1)
    else
      thiz
        

  //========== map & foreach ==========


  def map[A, U, B](thiz: NextStep[A, U], f: A => B): NextStep[B, U] =
    thiz.decons: (a, nx) =>
      f(a) ::! map(nx, f)


  def mapEff[A, B, U, V <: U](thiz: NextStep[A, U], f: A => B !! V): NextStep[B, V] =
    thiz.decons: (a, nx) =>
      f(a).flatMap(_ ::! mapEff(nx, f))


  def flatMap[A, B, U, V <: U](thiz: NextStep[A, U], f: A => NextStep[B, V]): NextStep[B, V] =
    thiz.decons: (a, nx) =>
      append(f(a), flatMap(nx, f))


  def flatMapEff[A, B, U, V <: U](thiz: NextStep[A, U], f: A => NextStep[B, V] !! V): NextStep[B, V] =
    thiz.decons: (a, nx) =>
      append(f(a).flatten, flatMapEff(nx, f))


  def foreach[A, U](thiz: NextStep[A, U], f: A => Unit): NextStep[A, U] =
    thiz.decons: (a, nx) =>
      !!.impure(f(a)) &&! (a ::! foreach(nx, f))


  def foreachEff[A, U, V <: U](thiz: NextStep[A, U], f: A => Unit !! V): NextStep[A, V] =
    thiz.decons: (a, nx) =>
      f(a) &&! (a ::! foreachEff(nx, f))


  def forsome[A, U](thiz: NextStep[A, U], f: PartialFunction[A, Unit]): NextStep[A, U] =
    forsomeEff(thiz, a => !!.impure(f(a)))


  def forsomeEff[A, U, V <: U](thiz: NextStep[A, U], f: PartialFunction[A, Unit !! V]): NextStep[A, V] =
    thiz.decons: (a, nx) =>
      val mb = f.applyOrElse(a, fallbackFun[A, Unit, V])
      !!.when(fallbackVal ne mb)(mb) &&!
      (a ::! forsomeEff(nx, f))


  //========== filter & collect ==========


  def filter[A, U](thiz: NextStep[A, U], f: A => Boolean): NextStep[A, U] =
    thiz.decons: (a, nx) =>
      f(a) match
        case true => a ::! filter(nx, f)
        case false => filter(nx, f)


  def filterNot[A, U](thiz: NextStep[A, U], f: A => Boolean): NextStep[A, U] =
    thiz.decons: (a, nx) =>
      f(a) match
        case false => a ::! filter(nx, f)
        case true => filter(nx, f)


  def filterEff[A, U, V <: U](thiz: NextStep[A, U], f: A => Boolean !! V): NextStep[A, V] =
    thiz.decons: (a, nx) =>
      f(a).flatMap:
        case true => a ::! filterEff(nx, f)
        case false => filterEff(nx, f)


  def filterNotEff[A, U, V <: U](thiz: NextStep[A, U], f: A => Boolean !! V): NextStep[A, V] =
    thiz.decons: (a, nx) =>
      f(a).flatMap:
        case false => a ::! filterEff(nx, f)
        case true => filterEff(nx, f)


  def mapFilter[A, U, B](thiz: NextStep[A, U], f: A => Option[B]): NextStep[B, U] =
    thiz.decons: (a, nx) =>
      f(a) match
        case Some(b) => b ::! mapFilter(nx, f)
        case None => mapFilter(nx, f)


  def mapFilterEff[A, B, U, V <: U](thiz: NextStep[A, U], f: A => Option[B] !! V): NextStep[B, V] =
    thiz.decons: (a, nx) =>
      f(a).flatMap:
        case Some(b) => b ::! mapFilterEff(nx, f)
        case None => mapFilterEff(nx, f)


  def collect[A, U, B](thiz: NextStep[A, U], f: PartialFunction[A, B]): NextStep[B, U] =
    mapFilter(thiz, f.lift(_))


  def collectEff[A, B, U, V <: U](thiz: NextStep[A, U], f: PartialFunction[A, B !! V]): NextStep[B, V] =
    thiz.decons: (a, nx) =>
      val mb = f.applyOrElse(a, fallbackFun[A, B, V])
      if fallbackVal ne mb then
        mb.flatMap(_ ::! collectEff(nx, f))
      else
        collectEff(nx, f)


  def filterWithPrevious[A, U](thiz: NextStep[A, U], f: (A, A) => Boolean): NextStep[A, U] =
    def loop(last: A, todo: NextStep[A, U]): NextStep[A, U] =
      todo.flatMap:
        case Step.Emit(a, nx) =>
          f(last, a) match
            case true => last ::! loop(a, nx)
            case false => loop(a, nx)
        case Step.End => singleton(last)
    thiz.decons: (a, nx) =>
      loop(a, nx)


  def filterWithPreviousEff[A, U, V <: U](thiz: NextStep[A, U], f: (A, A) => Boolean !! V): NextStep[A, V] =
    def loop(last: A, todo: NextStep[A, U]): NextStep[A, V] =
      todo.flatMap:
        case Step.Emit(a, nx) =>
          f(last, a).flatMap:
            case true => last ::! loop(a, nx)
            case false => loop(a, nx)
        case Step.End => singleton(last)
    thiz.decons: (a, nx) =>
      loop(a, nx)


  //========== fold ==========


  def fold[A, B, U](thiz: NextStep[A, U], zero: B, op: (B, A) => B): B !! U =
    def loop(todo: NextStep[A, U], accum: B !! Any): B !! U =
      todo.flatMap:
        case Step.Emit(a, nx) => loop(nx, accum.map(op(_, a)))
        case Step.End => accum
    loop(thiz, zero.pure_!!)


  def foldEff[A, B, U, V <: U](thiz: NextStep[A, U], zero: B, op: (B, A) => B !! V): B !! V =
    def loop(todo: NextStep[A, U], accum: B !! V): B !! V =
      todo.flatMap:
        case Step.Emit(a, nx) => loop(nx, accum.flatMap(op(_, a)))
        case Step.End => accum
    loop(thiz, zero.pure_!!)


  def drain[A, U](thiz: NextStep[A, U]): Unit !! U =
    thiz.flatMap:
      case Step.Emit(_, nx) => drain(nx)
      case Step.End => !!.unit


  //========== aux ==========


  extension [A, U](thiz: NextStep[A, U])
    inline private def decons[B, V <: U](inline f: (A, NextStep[A, U]) => NextStep[B, V]): NextStep[B, V] =
      thiz.flatMap:
        case Step.Emit(a, nx) => f(a, nx)
        case Step.End => Step.endPure


  extension [A, B](thiz: A => B)
    def andThenPure: A => B !! Any = a => !!.pure(thiz(a))


  private val fallbackResult: Any !! Any = !!.pure(null)
  private val fallbackVal: Any => Any !! Any = _ => fallbackResult
  private def fallbackFun[A, B, U]: A => B !! U = fallbackVal.asInstanceOf[A => B !! U]
