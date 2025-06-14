package beam.internals
import scala.collection.immutable.Queue
import collection.mutable.ArrayBuffer
import turbolift.{!!, Handler}
import turbolift.Extensions._
import turbolift.data.{Chunk, ChunkBuilder}
import beam.{Stream, SourceEffect}
import beam.Syntax._


final case class UnchunkedStream[A, U](Fx: SourceEffect[A])(val compute: Unit !! (U & Fx.type)) extends Stream.Unsealed[A, U]:
  type Fx = Fx.type

  // ------- map -------


  override def map[B](f: A => B): Stream[B, U] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = Fx2.emit(f(a))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def mapEff[B, V <: U](f: A => B !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(a).flatMap(Fx2.emit)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def mapChunk[B](f: Chunk[A] => Chunk[B]): Stream[B, U] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = Fx2.emit(f(Chunk.singleton(a)))
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def mapChunkEff[B, V <: U](f: Chunk[A] => Chunk[B] !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(Chunk.singleton(a)).flatMap(Fx2.emit)
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def mapToChunk[B](f: A => Chunk[B]): Stream[B, U] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = Fx2.emit(f(a))
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def mapToChunkEff[B, V <: U](f: A => Chunk[B] !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateless[Fx2.type & V]:
      override def emit(a: A) = f(a).flatMap(Fx2.emit)
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def mapFromChunk[B](f: Chunk[A] => B): Stream[B, U] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = Fx2.emit(f(Chunk.singleton(a)))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def mapFromChunkEff[B, V <: U](f: Chunk[A] => B !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[Fx2.type & V]:
      override def emit(a: A) = f(Chunk.singleton(a)).flatMap(Fx2.emit)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def flatMap[B, V <: U](f: A => Stream[B, V]): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(a).relayToUnchunked(Fx2)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def flatMapEff[B, V <: U](f: A => Stream[B, V] !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(a).flatMap(_.relayToUnchunked(Fx2))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def tapEach(f: A => Unit): Stream[A, U] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = !!.impure(f(a)) &&! Fx2.emit(a)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def tapEachEff[V <: U](f: A => Unit !! V): Stream[A, V] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(a) &&! Fx2.emit(a)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def tapEachChunk(f: Chunk[A] => Unit): Stream[A, U] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = !!.impure(f(Chunk.singleton(a))) &&! Fx2.emit(a)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def tapEachChunkEff[V <: U](f: Chunk[A] => Unit !! V): Stream[A, V] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(Chunk.singleton(a)) &&! Fx2.emit(a)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def mapK[V](f: [Fx] => (Unit !! (Fx & U)) => (Unit !! (Fx & V))): Stream[A, V] =
    f(compute).asUnchunkedStream(Fx)


  // ------- filter -------


  override def filter(f: A => Boolean): Stream[A, U] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = !!.when(f(a))(Fx2.emit(a))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def filterEff[V <: U](f: A => Boolean !! V): Stream[A, V] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(a).flatMap(!!.when(_)(Fx2.emit(a)))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def mapFilter[B](f: A => Option[B]): Stream[B, U] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = f(a).fold(!!.unit)(Fx2.emit)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def mapFilterEff[B, V <: U](f: A => Option[B] !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(a).flatMap(_.fold(!!.unit)(Fx2.emit))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def collect[B](f: PartialFunction[A, B]): Stream[B, U] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = f.lift(a).fold(!!.unit)(Fx2.emit)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def collectEff[B, V <: U](f: PartialFunction[A, B !! V]): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f.lift(a).fold(!!.unit)(_.flatMap(Fx2.emit))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  // ------- combine -------


  override def concat[B >: A, V <: U](that: Stream[B, V]): Stream[B, V] =
    (that: @unchecked) match
      case that: UnchunkedStream[B, V] =>
        val compute1 = if Fx == that.Fx then compute.cast[B, V] else relayToUnchunked(that.Fx)
        (compute1 &&! that.compute).asUnchunkedStream(that.Fx)
      case that: ChunkedStream[B, V] =>
        (relayToChunked(that.Fx) &&! that.compute).asChunkedStream(that.Fx)


  override def zipWith[B, C, V <: U](that: Stream[B, V])(f: (A, B) => C): Stream[C, V] =
    case object Fx2 extends SourceEffect[C]
    new Fx.Stateful[Stream[B, V], Fx2.type & V](that):
      override def onReturn(s: Local) = s.abort
      override def emit(a: A) =
        Local.getsEff: s =>
          s.decons.flatMap:
            case None => !!.unit
            case Some((b, s2)) => Local.put(s2) &&! Fx2.emit(f(a, b))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def zipWithEff[B, C, V <: U](that: Stream[B, V])(f: (A, B) => C !! V): Stream[C, V] =
    case object Fx2 extends SourceEffect[C]
    new Fx.Stateful[Stream[B, V], Fx2.type & V](that):
      override def onReturn(s: Local) = s.abort
      override def emit(a: A) =
        Local.getsEff: s =>
          s.decons.flatMap:
            case None => !!.unit
            case Some((b, s2)) => Local.put(s2) &&! f(a, b).flatMap(Fx2.emit)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def interleave[B >: A, V <: U](that: Stream[B, V]): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateful[Stream[B, V], Fx2.type & V](that):
      override def onReturn(s: Local) = s.abort
      override def emit(a: A) =
        Local.getsEff: s =>
          s.decons.flatMap:
            case None => !!.unit
            case Some((b, s2)) => Local.put(s2) &&! Fx2.emit(a) &&! Fx2.emit(b)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def mergeSorted[B >: A, V <: U](that: Stream[B, V])(using ev: Ordering[B]): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    that.decons.flatMapHandler:
      new Fx.Stateful[Option[(B, Stream[B, V])], Fx2.type & V](_):
        override def onReturn(s: Local) = s match
          case None => !!.unit
          case Some((b, bs)) => Fx2.emit(b) &&! bs.relayToUnchunked(Fx2)
        override def emit(a: A) =
          Local.getsEff:
            case None => Fx2.emit(a)
            case Some((b, bs)) =>
              if ev.compare(a, b) <= 0 then
                Fx2.emit(a)
              else
                Fx2.emit(b) &&!
                bs.relayToUnchunkedWhileLessThen(Fx2, a).flatMap(Local.put) &&!
                Fx2.emit(a)
      .toHandler
    .handle(compute).asUnchunkedStream(Fx2)


  // ------- prefix -------


  override def prepend[B >: A](value: B): Stream[B, U] =
    val Fx2 = Fx.upCast[B]
    (Fx2.emit(value) &&! compute.cast[Unit, U & Fx2.type]).asUnchunkedStream(Fx2)


  override def prependChunk[B >: A](values: Chunk[B]): Stream[B, U] =
    chunked.prependChunk(values)


  override def decons: Option[(A, Stream[A, U])] !! U =
    new Fx.StatelessReturn[Option[(A, Stream[A, U])], U]:
      override def captureHint = true
      override def onReturn() = !!.none
      override def emit(value: A) =
        Control.capture0: k =>
          val tail = UnchunkedStream(Fx)(Control.strip(k()))
          Some((value, tail)).pure_!!
    .toHandler.handle(compute)


  //@#@ unused
  private def deconsCompute: Option[(A, Unit !! (U & Fx))] !! U =
    new Fx.StatelessReturn[Option[(A, Unit !! (U & Fx))], U]:
      override def captureHint = true
      override def onReturn() = !!.none
      override def emit(value: A) =
        Control.capture0: k =>
          val tail = Control.strip(k())
          Some((value, tail)).pure_!!
    .toHandler.handle(compute)


  override def deconsChunk: Option[(Chunk[A], Stream[A, U])] !! U =
    decons.map(_.map { case (a, s) => (Chunk.singleton(a), s) })


  override def take(count: Long): Stream[A, U] =
    if count <= 0L then
      Fx.empty
    else
      case object Fx2 extends SourceEffect[A]
      new Fx.Stateful[Long, Fx2.type](count):
        override def emit(a: A) =
          Local.modifyGet(_ - 1).flatMap: n =>
            Fx2.emit(a) &&!
            !!.when(n == 0)(Control.abort(()))
      .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def drop(count: Long): Stream[A, U] =
    if count <= 0L then
      this
    else
      case object Fx2 extends SourceEffect[A]
      new Fx.Stateful[Long, Fx2.type](count):
        override def emit(a: A) =
          Local.getsEff: n =>
            if n > 0L then Local.put(n - 1L) else Fx2.emit(a)
      .toHandler.handle(compute).asUnchunkedStream(Fx2)


  //@#@TODO replace after bugfix
  /*override*/ def drop__bugged(count: Long): Stream[A, U] =
    if count <= 0L then
      this
    else
      new Fx.Stateful[Long, Fx](count):
        override def captureHint = true
        override def emit(a: A) =
          Local.modifyGet(_ - 1L).flatMap:
            case 0L => Control.capture0(k => Control.strip(k()))
            case _ => !!.unit
      .toHandler.handle(compute).asUnchunkedStream(Fx)


  override def takeWhile(f: A => Boolean): Stream[A, U] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = if f(a) then Fx2.emit(a) else Control.abort(())
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def takeWhileEff[V <: U](f: A => Boolean !! V): Stream[A, V] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(a: A) = f(a).flatMap(if _ then Fx2.emit(a) else Control.abort(()))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def dropWhile(f: A => Boolean): Stream[A, U] =
    new Fx.Stateless[Fx]:
      override def emit(a: A) =
        if f(a) then
          !!.unit
        else
          Control.capture0(k => Fx.emit(a) &&! Control.strip(k()))
    .toHandler.handle(compute).asUnchunkedStream(Fx)


  override def dropWhileEff[V <: U](f: A => Boolean !! V): Stream[A, V] =
    new Fx.Stateless[Fx & V]:
      override def emit(a: A) =
        f(a).flatMap:
          case true => !!.unit
          case false => Control.capture0(k => Fx.emit(a) &&! Control.strip(k()))
    .toHandler.handle(compute).asUnchunkedStream(Fx)


  // ------- split -------


  override def splitAt(count: Int): (Chunk[A], Stream[A, U]) !! U =
    if count <= 0L then
      (Chunk.empty, this).pure_!!
    else
      new Fx.StatefulReturn[Chunk[A], (Chunk[A], Stream[A, U]), U](Chunk()):
        override def captureHint = true
        override def onReturn(s: Local) = (s, Fx.empty).pure_!!
        override def emit(a: A) =
          Local.modifyGet(_ :+ a).flatMap: s =>
            if s.size < count then
              !!.unit
            else
              Control.capture0: k =>
                val tail = UnchunkedStream(Fx)(Control.strip(k()))
                (s, tail).pure_!!
      .toHandler.handle(compute)


  override def split[A2 >: A](separator: A2): Stream[Chunk[A], U] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateful[Chunk[A], Fx2.type](Chunk()):
      override def onReturn(s: Chunk[A]) = Fx2.emit(s)
      override def emit(a: A) =
        if a == separator then
          Local.swap(Chunk()).flatMap(Fx2.emit)
        else
          Local.modify(_ :+ a)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def splitWhere(f: A => Boolean): Stream[Chunk[A], U] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateful[Chunk[A], Fx2.type](Chunk()):
      override def onReturn(s: Chunk[A]) = Fx2.emit(s)
      override def emit(a: A) =
        if f(a) then
          Local.swap(Chunk(a)).flatMap(Fx2.emit)
        else
          Local.modify(_ :+ a)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def splitWhereEff[V <: U](f: A => Boolean !! V): Stream[Chunk[A], V] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateful[Chunk[A], Fx2.type & V](Chunk()):
      override def onReturn(s: Chunk[A]) = Fx2.emit(s)
      override def emit(a: A) =
        f(a).flatMap:
          case true => Local.swap(Chunk(a)).flatMap(Fx2.emit)
          case false => Local.modify(_ :+ a)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  // ------- history -------


  override def filterWithPrevious(f: (A, A) => Boolean): Stream[A, U] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateful[Option[A], Fx2.type](None):
      override def onReturn(s: Option[A]) = !!.unit
      override def emit(a: A) =
        Local.gets(_.fold(true)(f(_, a))).flatMap: ok =>
          !!.when(ok)(Local.put(Some(a)) &&! Fx2.emit(a))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def filterWithPreviousEff[V <: U](f: (A, A) => Boolean !! V): Stream[A, V] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateful[Option[A], V & Fx2.type](None):
      override def onReturn(s: Option[A]) = !!.unit
      override def emit(a: A) =
        Local.getsEff(_.fold(true.pure_!!)(f(_, a))).flatMap: ok =>
          !!.when(ok)(Local.put(Some(a)) &&! Fx2.emit(a))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def changesBy[B](f: A => B): Stream[A, U] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateful[Option[B], Fx2.type](None):
      override def onReturn(s: Option[B]) = !!.unit
      override def emit(a: A) =
        val b = f(a)
        Local.gets(_.fold(true)(_ != b)).flatMap: ok =>
          !!.when(ok)(Local.put(Some(b)) &&! Fx2.emit(a))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def changesByEff[B, V <: U](f: A => B !! V): Stream[A, V] =
    case object Fx2 extends SourceEffect[A]
    new Fx.Stateful[Option[B], V & Fx2.type](None):
      override def onReturn(s: Option[B]) = !!.unit
      override def emit(a: A) =
        f(a).flatMap: b =>
          Local.gets(_.fold(true)(_ != b)).flatMap: ok =>
            !!.when(ok)(Local.put(Some(b)) &&! Fx2.emit(a))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def window(size: Int, step: Int): Stream[Chunk[A], U] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    if size <= 0 || step <= 0 then
      Fx2.empty
    else
      case class S(que: Chunk[A], waits: Int, skips: Int)
      new Fx.Stateful[S, Fx2.type](S(Chunk.empty[A], waits = size, skips = step - 1)):
        override def onReturn(s: S) = !!.unit
        override def emit(a: A) = Local.modifyEff: s =>
          if s.waits > 0 then
            val q = s.que :+ a
            val s2 = S(q, waits = s.waits - 1, s.skips)
            if s.waits > 1 then
              s2.pure_!!
            else
              Fx2.emit(q).as(s2)
          else
            val q = s.que.tail :+ a
            if s.skips > 0 then
              S(q, waits = 0, skips = s.skips - 1).pure_!!
            else
              Fx2.emit(q).as(S(q, waits = 0, skips = step - 1))
      .toHandler.handle(compute).asUnchunkedStream(Fx2)


  // ------- misc -------


  override def scanLeft[B](initial: B)(f: (B, A) => B): Stream[B, U] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateful[B, Fx2.type](initial):
      override def emit(a: A) = Local.modifyGet(f(_, a)) >>= Fx2.emit
    .toHandler.handle(Fx2.emit(initial) &&! compute).asUnchunkedStream(Fx2)


  override def scanLeftEff[B, V <: U](initial: B)(f: (B, A) => B !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateful[B, V & Fx2.type](initial):
      override def emit(a: A) = Local.modifyGetEff(f(_, a)) >>= Fx2.emit
    .toHandler.handle(Fx2.emit(initial) &&! compute).asUnchunkedStream(Fx2)


  override def zipWithIndex: Stream[(A, Int), U] =
    case object Fx2 extends SourceEffect[(A, Int)]
    new Fx.Stateful[Int, Fx2.type](0):
      override def emit(a: A) = Local.modifyEff(n => Fx2.emit((a, n)).as(n + 1))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def zipWithLongIndex: Stream[(A, Long), U] =
    case object Fx2 extends SourceEffect[(A, Long)]
    new Fx.Stateful[Long, Fx2.type](0L):
      override def emit(a: A) = Local.modifyEff(n => Fx2.emit((a, n)).as(n + 1))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def intersperse[B >: A](separator: B): Stream[B, U] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateful[Boolean, Fx2.type](false):
      override def emit(a: A) = Local.getsEff:
        case false => Fx2.emit(a) &&! Local.put(true)
        case true => Fx2.emit(separator) &&! Fx2.emit(a)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def intersperse[B >: A](first: B, separator: B, last: B): Stream[B, U] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateful[Boolean, Fx2.type](false):
      override def emit(a: A) = Local.getsEff:
        case false => Fx2.emit(a) &&! Local.put(true)
        case true => Fx2.emit(separator) &&! Fx2.emit(a)
    .toHandler.handle(Fx2.emit(first) &&! compute &&! Fx2.emit(last)).asUnchunkedStream(Fx2)


  // ------- chunk -------


  override def isChunked: Boolean = false


  override def chunked: Stream[A, U] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    relayToChunked(Fx2).asChunkedStream(Fx2)


  override def unchunked: Stream[A, U] = this


  override def chunks: Stream[Chunk[A], U] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    relayToChunked(Fx2).asUnchunkedStream(Fx2)


  override def unchunks[B](using A <:< Chunk[B]): Stream[B, U] =
    val Fx2 = Fx.upCastEv[Chunk[B]]
    compute.cast[Unit, Fx2.type].asChunkedStream(Fx2)


  override def rechunk(n: Int = Stream.defaultChunkSize): Stream[A, U] =
    if n <= 1 then
      this
    else
      case object Fx2 extends SourceEffect[Chunk[A]]
      new Fx.Stateful[ChunkBuilder[A], Fx2.type](ChunkBuilder(n)):
        override def onReturn(s: Local) = Fx2.emit(s.toChunk)
        override def emit(a: A) = Local.getsEff: s =>
          s.addOne(a)
          !!.when(s.size == n):
            Fx2.emit(s.toChunk) &&!
            Local.put(ChunkBuilder(n))
      .toHandler.handle(compute).asChunkedStream(Fx2)


  // ------- compile -------


  override def drain: Unit !! U =
    new Fx.Stateless[Any]:
      override def emit(a: A) = !!.unit
    .toHandler.handle(compute)


  override def abort: Unit !! U =
    new Fx.Stateless[Any]:
      override def emit(a: A) = !!.unit
    .toHandler.handle(Fx.exit &&! compute)


  override def foreach(f: A => Unit): Unit !! U =
    new Fx.Stateless[Any]:
      override def emit(a: A) = !!.impure(f(a))
    .toHandler.handle(compute)


  override def foreachEff[V <: U](f: A => Unit !! V): Unit !! V =
    new Fx.Stateless[V]:
      override def emit(a: A) = f(a)
    .toHandler.handle(compute)


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


  override def collectFirst[B](f: PartialFunction[A, B]): Option[B] !! U =
    new Fx.StatelessReturn[Option[B], Any]:
      override def onReturn() = !!.none
      override def emit(a: A) =
        val mb = f.lift(a)
        !!.when(mb.isDefined)(Control.abort(mb))
    .toHandler.handle(compute)


  override def collectFirstEff[B, V <: U](f: PartialFunction[A, B !! V]): Option[B] !! V =
    new Fx.StatelessReturn[Option[B], V]:
      override def onReturn() = !!.none
      override def emit(a: A) =
        f.lift(a) match
          case Some(comp) => comp.flatMap(b => Control.abort(Some(b)))
          case None => !!.unit
    .toHandler.handle(compute)


  override def collectLast[B](f: PartialFunction[A, B]): Option[B] !! U =
    new Fx.StatefulReturn[Option[B], Option[B], Any](None):
      override def onReturn(s: Local) = s.pure_!!
      override def emit(a: A) =
        val mb = f.lift(a)
        !!.when(mb.isDefined)(Local.put(mb))
    .toHandler.handle(compute)


  override def collectLastEff[B, V <: U](f: PartialFunction[A, B !! V]): Option[B] !! V =
    new Fx.StatefulReturn[Option[B], Option[B], V](None):
      override def onReturn(s: Local) = s.pure_!!
      override def emit(a: A) =
        f.lift(a) match
          case Some(comp) => comp.flatMap(b => Local.put(Some(b)))
          case None => !!.unit
    .toHandler.handle(compute)


  // ------- internals -------


  override def relayToUnchunked[A2 >: A](Fx2: SourceEffect[A2]): Unit !! (U & Fx2.type) =
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = Fx2.emit(a)
    .toHandler.handle(compute)


  override def relayToChunked[B >: A](Fx2: SourceEffect[Chunk[B]]): Unit !! (U & Fx2.type) =
    new Fx.Stateless[Fx2.type]:
      override def emit(a: A) = Fx2.emit1(a)
    .toHandler.handle(compute)


  //@#@TODO rewrite with handler
  override def relayToUnchunkedWhileLessThen[B >: A](Fx2: SourceEffect[B], b: B)(using ev: Ordering[B]): Option[(A, Stream[A, U])] !! (U & Fx2.type) =
    decons.flatMap:
      case None => !!.none
      case Some((a, as)) =>
        if ev.compare(a, b) <= 0 then
          Fx2.emit(a) &&! as.relayToUnchunkedWhileLessThen(Fx2, b)
        else
          Some((a, as)).pure_!!
