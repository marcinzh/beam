package beam.internals
import scala.reflect.ClassTag
import turbolift.{!!, Handler}
import turbolift.Extensions._
import beam.{Stream, SourceEffect}
import beam.Syntax._


final case class StreamImpl[A, U](Fx: SourceEffect[A])(val compute: Unit !! (U & Fx.type)) extends Stream.Unsealed[A, U]:
  type Fx = Fx.type

  // ------- map -------


  override def map[B](f: A => B): Stream[B, U] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = Fx2.emit(f(a))
    .toHandler.handle(compute).asStream(Fx2)


  override def mapEff[B, V <: U](f: A => B !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(a).flatMap(Fx2.emit)
    .toHandler.handle(compute).asStream(Fx2)


  override def flatMap[B, V <: U](f: A => Stream[B, V]): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(a).relayTo(Fx2)
    .toHandler.handle(compute).asStream(Fx2)


  override def flatMapEff[B, V <: U](f: A => Stream[B, V] !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(a).flatMap(_.relayTo(Fx2))
    .toHandler.handle(compute).asStream(Fx2)


  override def foreach(f: A => Unit): Stream[A, U] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = !!.impure(f(a)) &&! Fx2.emit(a)
    .toHandler.handle(compute).asStream(Fx2)


  override def foreachEff[V <: U](f: A => Unit !! V): Stream[A, V] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(a) &&! Fx2.emit(a)
    .toHandler.handle(compute).asStream(Fx2)


  override def forsome(f: PartialFunction[A, Unit]): Stream[A, U] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = !!.impure(f.applyOrElse(a, _ => ())) &&! Fx2.emit(a)
    .toHandler.handle(compute).asStream(Fx2)


  override def forsomeEff[V <: U](f: PartialFunction[A, Unit !! V]): Stream[A, V] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f.applyOrElse(a, _ => !!.unit) &&! Fx2.emit(a)
    .toHandler.handle(compute).asStream(Fx2)


  // ------- filter -------


  override def filter(f: A => Boolean): Stream[A, U] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = !!.when(f(a))(Fx2.emit(a))
    .toHandler.handle(compute).asStream(Fx2)


  override def filterEff[V <: U](f: A => Boolean !! V): Stream[A, V] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(a).flatMap(!!.when(_)(Fx2.emit(a)))
    .toHandler.handle(compute).asStream(Fx2)


  override def mapFilter[B](f: A => Option[B]): Stream[B, U] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = f(a).fold(!!.unit)(Fx2.emit)
    .toHandler.handle(compute).asStream(Fx2)


  override def mapFilterEff[B, V <: U](f: A => Option[B] !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(a).flatMap(_.fold(!!.unit)(Fx2.emit))
    .toHandler.handle(compute).asStream(Fx2)


  override def collect[B](f: PartialFunction[A, B]): Stream[B, U] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = f.lift(a).fold(!!.unit)(Fx2.emit)
    .toHandler.handle(compute).asStream(Fx2)


  override def collectEff[B, V <: U](f: PartialFunction[A, B !! V]): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f.lift(a).fold(!!.unit)(_.flatMap(Fx2.emit))
    .toHandler.handle(compute).asStream(Fx2)


  // ------- combine -------


  override def concat[B >: A, V <: U](that: Stream[B, V]): Stream[B, V] =
    val that2 = that.asImpl
    val compute1 = if Fx == that2.Fx then compute.cast[B, V] else relayTo(that2.Fx)
    (compute1 &&! that2.compute).asStream(that2.Fx)


  override def zipWith[B, C, V <: U](that: Stream[B, V])(f: (A, B) => C): Stream[C, V] =
    case object Fx2 extends SourceEffect[C]
    new Fx.Stateful[Stream[B, V], Fx2.type & V](that):
      override def onReturn(s: Local) = s.abort
      override def emit(a: A) =
        Local.get.flatMap: s =>
          s.decons.flatMap:
            case None => !!.unit
            case Some((b, s2)) => Local.put(s2) &&! Fx2.emit(f(a, b))
    .toHandler.handle(compute).asStream(Fx2)


  override def zipWithEff[B, C, V <: U](that: Stream[B, V])(f: (A, B) => C !! V): Stream[C, V] =
    case object Fx2 extends SourceEffect[C]
    new Fx.Stateful[Stream[B, V], Fx2.type & V](that):
      override def onReturn(s: Local) = s.abort
      override def emit(a: A) =
        Local.get.flatMap: s =>
          s.decons.flatMap:
            case None => !!.unit
            case Some((b, s2)) => Local.put(s2) &&! f(a, b).flatMap(Fx2.emit)
    .toHandler.handle(compute).asStream(Fx2)


  // ------- prefix -------


  override def decons: Option[(A, Stream[A, U])] !! U =
    new Fx.StatelessReturn[Option[(A, Stream[A, U])], U]:
      override def captureHint = true
      override def onReturn() = !!.none
      override def emit(value: A) =
        Control.capture0: k =>
          val tail = StreamImpl(Fx)(Control.strip(k()))
          Some((value, tail)).pure_!!
    .toHandler.handle(compute)


  override def splitAt[A2 >: A](using ClassTag[A2])(count: Int): (Array[A2], Stream[A, U]) !! U =
    if count <= 0L then
      (Array.empty[A2], this).pure_!!
    else
      !!.impure(new collection.mutable.ArrayBuffer[A2]).map: buf =>
        new Fx.StatefulReturn[Int, (Array[A2], Stream[A, U]), U](count):
          override def captureHint = true
          override def onReturn(s: Int) = (buf.toArray, emptyStream(Fx)).pure_!!
          override def emit(value: A) =
            !!.impure(buf += value) &&!
            Local.modifyGet(_ - 1).flatMap:
              case 0 => Control.capture0: k =>
                val tail = StreamImpl(Fx)(Control.strip(k()))
                (buf.toArray, tail).pure_!!
              case _ => !!.unit
        .toHandler
      .flattenHandler.handle(compute)


  override def take(count: Long): Stream[A, U] =
    if count <= 0L then
      Fx.emptyStream
    else
      case object Fx2 extends SourceEffect[A]
      new Fx.Stateful[Long, Fx2.type](count):
        override def emit(a: A) =
          for
            _ <- Fx2.emit(a)
            n <- Local.modifyGet(_ - 1)
            x <- !!.when(n == 0)(Control.abort(()))
          yield x
      .toHandler.handle(compute).asStream(Fx2)


  override def drop(count: Long): Stream[A, U] =
    if count <= 0L then
      this
    else
      new Fx.Stateful[Long, Fx](count):
        override def emit(a: A) =
          Local.modifyGet(_ - 1L).flatMap:
            case 0L => Control.capture0(k => Control.strip(k()))
            case _ => !!.unit
      .toHandler.handle(compute).asStream(Fx)


  override def takeWhile(f: A => Boolean): Stream[A, U] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = if f(a) then Fx2.emit(a) else Control.abort(())
    .toHandler.handle(compute).asStream(Fx2)


  override def takeWhileEff[V <: U](f: A => Boolean !! V): Stream[A, V] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(a).flatMap(if _ then Fx2.emit(a) else Control.abort(()))
    .toHandler.handle(compute).asStream(Fx2)


  override def dropWhile(f: A => Boolean): Stream[A, U] =
    new Fx.Stateless[Fx]:
      override def emit(a: A) =
        if f(a) then
          !!.unit
        else
          Control.capture0(k => Fx.emit(a) &&! Control.strip(k()))
    .toHandler.handle(compute).asStream(Fx)


  override def dropWhileEff[V <: U](f: A => Boolean !! V): Stream[A, V] =
    new Fx.Stateless[Fx & V]:
      override def emit(a: A) =
        f(a).flatMap:
          case true => !!.unit
          case false => Control.capture0(k => Fx.emit(a) &&! Control.strip(k()))
    .toHandler.handle(compute).asStream(Fx)


  // ------- misc -------


  override def scanLeft[B](initial: B)(f: (B, A) => B): Stream[B, U] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateful[B, Fx2.type](initial):
      override def emit(a: A) = Local.modifyGet(f(_, a)) >>= Fx2.emit
    .toHandler.handle(Fx2.emit(initial) &&! compute).asStream(Fx2)


  override def scanLeftEff[B, V <: U](initial: B)(f: (B, A) => B !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateful[B, V & Fx2.type](initial):
      override def emit(a: A) = Local.modifyGetEff(f(_, a)) >>= Fx2.emit
    .toHandler.handle(Fx2.emit(initial) &&! compute).asStream(Fx2)


  // ------- compile -------


  override def drain: Unit !! U =
    new Fx.Stateless[Any]:
      override def emit(a: A) = !!.unit
    .toHandler.handle(compute)


  override def abort: Unit !! U =
    new Fx.Stateless[Any]:
      override def emit(a: A) = !!.unit
    .toHandler.handle(Fx.exit &&! compute)


  override def foldLeft[B](initial: B)(f: (B, A) => B): B !! U =
    new Fx.StatefulReturn[B, B, Any](initial):
      override def onReturn(b: B) = b.pure_!!
      override def emit(a: A) = Local.modify(f(_, a))
    .toHandler.handle(compute)


  override def foldLeftEff[B, V <: U](initial: B)(f: (B, A) => B !! V): B !! V =
    new Fx.StatefulReturn[B, B, V](initial):
      override def onReturn(b: B) = b.pure_!!
      override def emit(a: A) = Local.modifyEff(f(_, a))
    .toHandler.handle(compute)


  // ------- internals -------


  override def relayTo[A2 >: A](Fx2: SourceEffect[A2]): Unit !! (U & Fx2.type) =
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = Fx2.emit(a)
    .toHandler.handle(compute)
