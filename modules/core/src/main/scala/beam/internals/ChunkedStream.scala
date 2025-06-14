package beam.internals
import scala.collection.immutable.Queue
import collection.mutable.ArrayBuffer
import turbolift.{!!, Handler}
import turbolift.Extensions._
import turbolift.data.{Chunk, ChunkBuilder}
import beam.{Stream, SourceEffect}
import beam.Syntax._


final case class ChunkedStream[A, U](Fx: SourceEffect[Chunk[A]])(val compute: Unit !! (U & Fx.type)) extends Stream.Unsealed[A, U]:
  type Fx = Fx.type

  // ------- map -------


  override def map[B](f: A => B): Stream[B, U] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateless[Fx2.type]:
      override def emit(aa: Chunk[A]) = Fx2.emit(aa.map(f))
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def mapEff[B, V <: U](f: A => B !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(aa: Chunk[A]) = aa.mapEff(f).flatMap(Fx2.emit)
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def mapChunk[B](f: Chunk[A] => Chunk[B]): Stream[B, U] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateless[Fx2.type]:
      override def emit(aa: Chunk[A]) = Fx2.emit(f(aa))
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def mapChunkEff[B, V <: U](f: Chunk[A] => Chunk[B] !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(aa: Chunk[A]) = f(aa).flatMap(Fx2.emit)
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def mapToChunk[B](f: A => Chunk[B]): Stream[B, U] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateless[Fx2.type]:
      override def emit(aa: Chunk[A]) = aa.foreachEff(a => Fx2.emit(f(a)))
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def mapToChunkEff[B, V <: U](f: A => Chunk[B] !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateless[Fx2.type & V]:
      override def emit(aa: Chunk[A]) = aa.foreachEff(a => f(a).flatMap(Fx2.emit))
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def mapFromChunk[B](f: Chunk[A] => B): Stream[B, U] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[Fx2.type]:
      override def emit(aa: Chunk[A]) = Fx2.emit(f(aa))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def mapFromChunkEff[B, V <: U](f: Chunk[A] => B !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[Fx2.type & V]:
      override def emit(aa: Chunk[A]) = f(aa).flatMap(Fx2.emit)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def flatMap[B, V <: U](f: A => Stream[B, V]): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(aa: Chunk[A]) = aa.foreachEff(a => f(a).relayToUnchunked(Fx2))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def flatMapEff[B, V <: U](f: A => Stream[B, V] !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[B]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(aa: Chunk[A]) = aa.foreachEff(a => f(a).flatMap(_.relayToUnchunked(Fx2)))
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def tapEach(f: A => Unit): Stream[A, U] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateless[Fx2.type]:
      override def emit(aa: Chunk[A]) = !!.impure(aa.foreach(f)) &&! Fx2.emit(aa)
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def tapEachEff[V <: U](f: A => Unit !! V): Stream[A, V] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(aa: Chunk[A]) = aa.foreachEff(f) &&! Fx2.emit(aa)
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def tapEachChunk(f: Chunk[A] => Unit): Stream[A, U] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateless[Fx2.type]:
      override def emit(aa: Chunk[A]) = !!.impure(f(aa)) &&! Fx2.emit(aa)
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def tapEachChunkEff[V <: U](f: Chunk[A] => Unit !! V): Stream[A, V] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(aa: Chunk[A]) = f(aa) &&! Fx2.emit(aa)
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def mapK[V](f: [Fx] => (Unit !! (Fx & U)) => (Unit !! (Fx & V))): Stream[A, V] =
    f(compute).asChunkedStream(Fx)


  // ------- filter -------


  override def filter(f: A => Boolean): Stream[A, U] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateless[Fx2.type]:
      override def emit(aa: Chunk[A]) = Fx2.emitIfNonEmpty(aa.filter(f))
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def filterEff[V <: U](f: A => Boolean !! V): Stream[A, V] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(aa: Chunk[A]) = aa.filterEff(f).flatMap(Fx2.emitIfNonEmpty)
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def mapFilter[B](f: A => Option[B]): Stream[B, U] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateless[Fx2.type]:
      override def emit(aa: Chunk[A]) = Fx2.emitIfNonEmpty(aa.mapFilter(f))
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def mapFilterEff[B, V <: U](f: A => Option[B] !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(aa: Chunk[A]) = aa.mapFilterEff(f).flatMap(Fx2.emitIfNonEmpty)
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def collect[B](f: PartialFunction[A, B]): Stream[B, U] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateless[Fx2.type]:
      override def emit(aa: Chunk[A]) = Fx2.emitIfNonEmpty(aa.collect(f))
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def collectEff[B, V <: U](f: PartialFunction[A, B !! V]): Stream[B, V] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(aa: Chunk[A]) = aa.collectEff(f).flatMap(Fx2.emitIfNonEmpty)
    .toHandler.handle(compute).asChunkedStream(Fx2)


  // ------- combine -------


  override def concat[B >: A, V <: U](that: Stream[B, V]): Stream[B, V] =
    (that: @unchecked) match
      case that: UnchunkedStream[B, V] =>
        case object Fx2 extends SourceEffect[Chunk[B]]
        (relayToChunked(Fx2) &&! that.relayToChunked(Fx2)).asChunkedStream(Fx2)
      case that: ChunkedStream[B, V] =>
        val compute1 = if Fx == that.Fx then compute.cast[B, V] else relayToChunked(that.Fx)
        (compute1 &&! that.compute).asChunkedStream(that.Fx)


  override def zipWith[B, C, V <: U](that: Stream[B, V])(f: (A, B) => C): Stream[C, V] =
    //@#@TODO chunked
    unchunked.zipWith(that.unchunked)(f)


  override def zipWithEff[B, C, V <: U](that: Stream[B, V])(f: (A, B) => C !! V): Stream[C, V] =
    //@#@TODO chunked
    unchunked.zipWithEff(that.unchunked)(f)


  override def interleave[B >: A, V <: U](that: Stream[B, V]): Stream[B, V] =
    //@#@TODO chunked
    unchunked.interleave(that.unchunked)


  override def mergeSorted[B >: A, V <: U](that: Stream[B, V])(using ev: Ordering[B]): Stream[B, V] =
    //@#@TODO chunked
    unchunked.mergeSorted(that.unchunked)


  // ------- prefix -------


  override def prepend[B >: A](value: B): Stream[B, U] =
    prependChunk(Chunk.singleton(value))


  override def prependChunk[B >: A](values: Chunk[B]): Stream[B, U] =
    val Fx2 = Fx.upCast[Chunk[B]]
    (Fx2.emit(values) &&! compute.cast[Unit, U & Fx2.type]).asChunkedStream(Fx2)


  override def decons: Option[(A, Stream[A, U])] !! U =
    new Fx.StatelessReturn[Option[(A, Stream[A, U])], U]:
      override def captureHint = true
      override def onReturn() = !!.none
      override def emit(aa: Chunk[A]) =
        aa.decons match
          case None => !!.unit
          case Some((b, bb)) =>
            Control.capture0: k =>
              val tail = ChunkedStream(Fx)(Fx.emit(bb) &&! Control.strip(k()))
              Some((b, tail)).pure_!!
    .toHandler.handle(compute)


  override def deconsChunk: Option[(Chunk[A], Stream[A, U])] !! U =
    new Fx.StatelessReturn[Option[(Chunk[A], Stream[A, U])], U]:
      override def captureHint = true
      override def onReturn() = !!.none
      override def emit(aa: Chunk[A]) =
        Control.capture0: k =>
          val tail = ChunkedStream(Fx)(Control.strip(k()))
          Some((aa, tail)).pure_!!
    .toHandler.handle(compute)


  override def take(count: Long): Stream[A, U] =
    if count <= 0L then
      Fx.emptyChunked
    else
      case object Fx2 extends SourceEffect[Chunk[A]]
      new Fx.Stateful[Long, Fx2.type](count):
        override def emit(aa: Chunk[A]) =
          Local.getModifyGet(_ - aa.size).flatMap: (oldCount, newCount) =>
            Fx2.emit(aa.take(oldCount.toInt)) &&!
            !!.when(newCount <= 0)(Control.abort(()))
      .toHandler.handle(compute).asChunkedStream(Fx2)


  override def drop(count: Long): Stream[A, U] =
    if count <= 0L then
      this
    else
      case object Fx2 extends SourceEffect[Chunk[A]]
      new Fx.Stateful[Long, Fx2.type](count):
        override def emit(aa: Chunk[A]) =
          Local.getsEff: n =>
            if n > 0L then
              Local.put(n - aa.size) &&! Fx2.emit(aa.drop(n.toInt))
            else
              Fx2.emit(aa)
      .toHandler.handle(compute).asChunkedStream(Fx2)


  //@#@TODO replace after bugfix
  /*override*/ def drop__bugged(count: Long): Stream[A, U] =
    if count <= 0L then
      this
    else
      new Fx.Stateful[Long, Fx](count):
        override def captureHint = true
        override def emit(aa: Chunk[A]) =
          Local.getModifyGet(_ - aa.size).flatMap: (oldCount, newCount) =>
            !!.when(newCount <= 0):
              Fx.emitIfNonEmpty(aa.drop(oldCount.toInt)) &&!
              Control.capture0(k => Control.strip(k()))
      .toHandler.handle(compute).asChunkedStream(Fx)


  override def takeWhile(f: A => Boolean): Stream[A, U] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateless[Fx2.type]:
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          val bb = aa.takeWhile(f)
          if bb.nonEmpty then Fx2.emit(bb) else Control.abort(())
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def takeWhileEff[V <: U](f: A => Boolean !! V): Stream[A, V] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateless[V & Fx2.type]:
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          aa.takeWhileEff(f).flatMap: bb =>
            if bb.nonEmpty then Fx2.emit(bb) else Control.abort(())
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def dropWhile(f: A => Boolean): Stream[A, U] =
    new Fx.Stateless[Fx]:
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          val bb = aa.takeWhile(f)
          !!.when(bb.nonEmpty):
            Control.capture0(k => Fx.emit(bb) &&! Control.strip(k()))
    .toHandler.handle(compute).asChunkedStream(Fx)


  override def dropWhileEff[V <: U](f: A => Boolean !! V): Stream[A, V] =
    new Fx.Stateless[Fx & V]:
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          aa.dropWhileEff(f).flatMap: bb =>
            !!.when(bb.nonEmpty):
              Control.capture0(k => Fx.emit(bb) &&! Control.strip(k()))
    .toHandler.handle(compute).asChunkedStream(Fx)


  // ------- split -------


  override def splitAt(count: Int): (Chunk[A], Stream[A, U]) !! U =
    if count <= 0L then
      (Chunk.empty, this).pure_!!
    else
      new Fx.StatefulReturn[Chunk[A], (Chunk[A], Stream[A, U]), U](Chunk()):
        override def captureHint = true
        override def onReturn(s: Local) = (s, Fx.emptyChunked).pure_!!
        override def emit(aa: Chunk[A]) =
          Local.getsEff: accum =>
            val room = count - accum.size
            if aa.size < room then
              Local.put(accum ++ aa)
            else
              val (bb, cc) = aa.splitAt(room)
              Control.capture0: k =>
                val tail = ChunkedStream(Fx)(Fx.emitIfNonEmpty(cc) &&! Control.strip(k()))
                (aa ++ bb, tail).pure_!!
      .toHandler.handle(compute)


  override def split[A2 >: A](separator: A2): Stream[Chunk[A], U] = splitWhere(_ == separator)


  override def splitWhere(f: A => Boolean): Stream[Chunk[A], U] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateful[Chunk[A], Fx2.type](Chunk()):
      override def onReturn(s: Local) = Fx2.emit(s)
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          Local.getsEff: accum =>
            val (aaa, bbb) = aa.splitWhere(f).splitAt(1)
            val first = accum ++ aaa.head
            if bbb.isEmpty then
              Local.put(first)
            else
              val last = bbb.last
              val mids = bbb.init
              Local.put(last) &&! Fx2.emit(first) &&! mids.foreachEff(Fx2.emit)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  override def splitWhereEff[V <: U](f: A => Boolean !! V): Stream[Chunk[A], V] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateful[Chunk[A], Fx2.type & V](Chunk()):
      override def onReturn(s: Local) = Fx2.emit(s)
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          Local.getsEff: accum =>
            aa.splitWhereEff(f).flatMap: splits =>
              val (aaa, bbb) = splits.splitAt(1)
              val first = accum ++ aaa.head
              if bbb.isEmpty then
                Local.put(first)
              else
                val last = bbb.last
                val mids = bbb.init
                Local.put(last) &&! Fx2.emit(first) &&! mids.foreachEff(Fx2.emit)
    .toHandler.handle(compute).asUnchunkedStream(Fx2)


  // ------- history -------


  override def filterWithPrevious(f: (A, A) => Boolean): Stream[A, U] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateful[Option[A], Fx2.type](None):
      override def onReturn(s: Option[A]) = !!.unit
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          Local.modifyEff: s =>
            val (bb, s2) = aa.auxFilterWithPrevious(f, s)
            Fx2.emitIfNonEmpty(bb).as(s2)
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def filterWithPreviousEff[V <: U](f: (A, A) => Boolean !! V): Stream[A, V] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateful[Option[A], V & Fx2.type](None):
      override def onReturn(s: Option[A]) = !!.unit
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          Local.modifyEff: s =>
            aa.auxFilterWithPreviousEff(f, s).flatMap: (bb, s2) =>
              Fx2.emitIfNonEmpty(bb).as(s2)
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def changesBy[B](f: A => B): Stream[A, U] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateful[Option[B], Fx2.type](None):
      override def onReturn(s: Option[B]) = !!.unit
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          Local.modifyEff: s =>
            val (bb, s2) = aa.auxChangesBy(f, s)
            Fx2.emitIfNonEmpty(bb).as(s2)
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def changesByEff[B, V <: U](f: A => B !! V): Stream[A, V] =
    case object Fx2 extends SourceEffect[Chunk[A]]
    new Fx.Stateful[Option[B], V & Fx2.type](None):
      override def onReturn(s: Option[B]) = !!.unit
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          Local.modifyEff: s =>
            aa.auxChangesByEff(f, s).flatMap: (bb, s2) =>
              Fx2.emitIfNonEmpty(bb).as(s2)
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def window(size: Int, step: Int): Stream[Chunk[A], U] =
    //@#@THOV chunked
    unchunked.window(size, step)


  // ------- misc -------


  override def scanLeft[B](initial: B)(f: (B, A) => B): Stream[B, U] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateful[B, Fx2.type](initial):
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          Local.modifyEff: s =>
            val (bb, s2) = aa.auxScanLeft(s)(f)
            Fx2.emit(bb).as(s2)
    .toHandler.handle(Fx2.emit1(initial) &&! compute).asChunkedStream(Fx2)


  override def scanLeftEff[B, V <: U](initial: B)(f: (B, A) => B !! V): Stream[B, V] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateful[B, V & Fx2.type](initial):
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          Local.modifyEff: s =>
            aa.auxScanLeftEff(s)(f).flatMap: (bb, s2) =>
              Fx2.emit(bb).as(s2)
    .toHandler.handle(Fx2.emit1(initial) &&! compute).asChunkedStream(Fx2)


  override def zipWithIndex: Stream[(A, Int), U] =
    case object Fx2 extends SourceEffect[Chunk[(A, Int)]]
    new Fx.Stateful[Int, Fx2.type](0):
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          Local.getModify(_ + aa.size).flatMap: s =>
            Fx2.emit(aa.auxZipWithIndex(s))
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def zipWithLongIndex: Stream[(A, Long), U] =
    case object Fx2 extends SourceEffect[Chunk[(A, Long)]]
    new Fx.Stateful[Long, Fx2.type](0L):
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          Local.getModify(_ + aa.size).flatMap: s =>
            Fx2.emit(aa.auxZipWithLongIndex(s))
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def intersperse[B >: A](separator: B): Stream[B, U] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateful[Boolean, Fx2.type](false):
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          Local.getsEff: s =>
            Fx2.emit(aa.auxIntersperse(separator, s)) &&!
            !!.when(!s)(Local.put(true))
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def intersperse[B >: A](first: B, separator: B, last: B): Stream[B, U] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateful[Boolean, Fx2.type](false):
      override def emit(aa: Chunk[A]) =
        !!.when(aa.nonEmpty):
          Local.getsEff: s =>
            Fx2.emit(aa.auxIntersperse(separator, s)) &&!
            !!.when(!s)(Local.put(true))
    .toHandler.handle(Fx2.emit1(first) &&! compute &&! Fx2.emit1(last)).asChunkedStream(Fx2)


  // ------- chunk -------


  override def isChunked: Boolean = true


  override def chunked: Stream[A, U] = this


  override def unchunked: Stream[A, U] =
    case object Fx2 extends SourceEffect[A]
    relayToUnchunked(Fx2).asUnchunkedStream(Fx2)


  override def chunks: Stream[Chunk[A], U] =
    compute.asUnchunkedStream(Fx)


  override def unchunks[B](using ev: A <:< Chunk[B]): Stream[B, U] =
    case object Fx2 extends SourceEffect[Chunk[B]]
    new Fx.Stateless[Fx2.type]:
      override def emit(aa: Chunk[A]) = aa.foreachEff(a => Fx2.emit(ev(a)))
    .toHandler.handle(compute).asChunkedStream(Fx2)


  override def rechunk(n: Int = Stream.defaultChunkSize): Stream[A, U] =
    if n <= 1 then
      unchunked
    else
      case object Fx2 extends SourceEffect[Chunk[A]]
      new Fx.Stateful[ChunkBuilder[A], Fx2.type](ChunkBuilder(n)):
        override def onReturn(s: Local) = Fx2.emit(s.toChunk)
        override def emit(aa: Chunk[A]) = Local.getsEff: s =>
          val room = n - s.size
          if aa.size < room then
            s.addChunk(aa).pure_!!
          else
            Fx2.emit(s.addChunkSlice(aa, 0, room).toChunk) &&!
            Local.put(ChunkBuilder(n).addChunkSlice(aa, room, aa.size))
      .toHandler.handle(compute).asChunkedStream(Fx2)


  // ------- compile -------


  override def drain: Unit !! U =
    new Fx.Stateless[Any]:
      override def emit(aa: Chunk[A]) = !!.unit
    .toHandler.handle(compute)

  override def abort: Unit !! U =
    new Fx.Stateless[Any]:
      override def emit(aa: Chunk[A]) = !!.unit
    .toHandler.handle(Fx.exit &&! compute)


  override def foreach(f: A => Unit): Unit !! U =
    new Fx.Stateless[Any]:
      override def emit(aa: Chunk[A]) = !!.impure(aa.foreach(f))
    .toHandler.handle(compute)


  override def foreachEff[V <: U](f: A => Unit !! V): Unit !! V =
    new Fx.Stateless[V]:
      override def emit(aa: Chunk[A]) = aa.foreachEff(f)
    .toHandler.handle(compute)


  override def foldLeft[B](initial: B)(f: (B, A) => B): B !! U =
    new Fx.StatefulReturn[B, B, Any](initial):
      override def onReturn(b: B) = b.pure_!!
      override def emit(aa: Chunk[A]) = Local.modify(aa.foldLeft(_)(f))
    .toHandler.handle(compute)


  override def foldLeftEff[B, V <: U](initial: B)(f: (B, A) => B !! V): B !! V =
    new Fx.StatefulReturn[B, B, V](initial):
      override def onReturn(b: B) = b.pure_!!
      override def emit(aa: Chunk[A]) = Local.modifyEff(aa.foldLeftEff(_)(f))
    .toHandler.handle(compute)


  override def collectFirst[B](f: PartialFunction[A, B]): Option[B] !! U =
    new Fx.StatelessReturn[Option[B], Any]:
      override def onReturn() = !!.none
      override def emit(aa: Chunk[A]) =
        val mb = aa.collectFirst(f)
        !!.when(mb.isDefined)(Control.abort(mb))
    .toHandler.handle(compute)


  override def collectFirstEff[B, V <: U](f: PartialFunction[A, B !! V]): Option[B] !! V =
    new Fx.StatelessReturn[Option[B], V]:
      override def onReturn() = !!.none
      override def emit(aa: Chunk[A]) =
        aa.collectFirstEff(f).flatMap: mb =>
          !!.when(mb.isDefined)(Control.abort(mb))
    .toHandler.handle(compute)


  override def collectLast[B](f: PartialFunction[A, B]): Option[B] !! U =
    new Fx.StatefulReturn[Option[B], Option[B], Any](None):
      override def onReturn(s: Local) = s.pure_!!
      override def emit(aa: Chunk[A]) =
        val mb = aa.collectLast(f)
        !!.when(mb.isDefined)(Local.put(mb))
    .toHandler.handle(compute)


  override def collectLastEff[B, V <: U](f: PartialFunction[A, B !! V]): Option[B] !! V =
    new Fx.StatefulReturn[Option[B], Option[B], V](None):
      override def onReturn(s: Local) = s.pure_!!
      override def emit(aa: Chunk[A]) =
        aa.collectLastEff(f).flatMap: mb =>
          !!.when(mb.isDefined)(Local.put(mb))
    .toHandler.handle(compute)


  // ------- internals -------


  override def relayToUnchunked[A2 >: A](Fx2: SourceEffect[A2]): Unit !! (U & Fx2.type) =
    new Fx.Stateless[Fx2.type]:
      override def emit(aa: Chunk[A]) = aa.foreachEff(Fx2.emit)
    .toHandler.handle(compute)


  override def relayToChunked[B >: A](Fx2: SourceEffect[Chunk[B]]): Unit !! (U & Fx2.type) =
    new Fx.Stateless[Fx2.type]:
      override def emit(aa: Chunk[A]) = Fx2.emit(aa)
    .toHandler.handle(compute)


  override def relayToUnchunkedWhileLessThen[B >: A](Fx2: SourceEffect[B], b: B)(using ev: Ordering[B]): Option[(A, Stream[A, U])] !! (U & Fx2.type) =
    //@#@THOV chunked
    unchunked.relayToUnchunkedWhileLessThen(Fx2, b)
