package turbolift.data
import scala.reflect.ClassTag
import scala.util.control.Breaks
import turbolift.!!
import turbolift.Extensions._
import turbolift.data.Utils._
import turbolift.data.MissingExtensions._
import Chunk.{Empty, Singleton, Compact, Foreign, Slice, Concat}


sealed abstract class Chunk[@specialized(Byte) +A] extends IterableOnce[A]:
  def size: Int
  def depth: Int
  final def isEmpty: Boolean = size == 0
  final def nonEmpty: Boolean = !isEmpty

  def reverseIterator: Iterator[A] = iterator

  def covary[B](using A <:< B): Chunk[B] = asInstanceOf[Chunk[B]]


  // ------- at index -------


  def apply(i: Int): A

  final def slice(startIndex: Int, endIndex: Int): Chunk[A] = drop(startIndex).take(endIndex - startIndex)


  // ------- map -------


  final def map[B](f: A => B): Chunk[B] =
    this match
      case Singleton(a) => Singleton(f(a))
      case Empty => Empty
      case _ =>
        val cb = unsafeCB[B]
        foreachSlice(a => cb.unsafeAddOne(f(a)))
        cb.toChunk


  final def mapEff[B, U](f: A => B !! U): Chunk[B] !! U = iterator.mapEff(f).map(Chunk.fromIndexed)


  final def flatMap[B](f: A => Chunk[B]): Chunk[B] =
    this match
      case Singleton(a) => f(a)
      case Empty => Empty
      case _ =>
        val cb = ChunkBuilder[B](0)
        foreachSlice(a => cb.addChunk(f(a)))
        cb.toChunk


  final def flatMapEff[B, U](f: A => Chunk[B] !! U): Chunk[B] !! U = iterator.flatMapEff(f).map(Chunk.fromIndexed)

  final def foreach(f: A => Unit): Unit = foreachSlice(f)

  final def foreachEff[U](f: A => Unit !! U): Unit !! U = iterator.foreachEff(f)


  // ------- filter -------


  final def filter(f: A => Boolean): Chunk[A] =
    this match
      case Singleton(a) => if f(a) then this else Empty
      case Empty => Empty
      case _ =>
        val cb = unsafeCB[A]
        foreachSlice(a => if f(a) then cb.unsafeAddOne(a))
        cb.toChunk


  final def filterNot(f: A => Boolean): Chunk[A] = filter(a => !f(a))

  //@#@TODO turbolift.Extensions will use Chunk as result, instead of Vector
  final def filterEff[U](f: A => Boolean !! U): Chunk[A] !! U = iterator.filterEff(f).map(Chunk.fromIndexed)
  final def filterNotEff[U](f: A => Boolean !! U): Chunk[A] !! U = filterEff(a => f(a).map(!_))
  final def mapFilter[B](f: A => Option[B]): Chunk[B] = Chunk.fromIterator(iterator.mapFilter(f))
  final def mapFilterEff[B, U](f: A => Option[B] !! U): Chunk[B] !! U = iterator.mapFilterEff(f).map(Chunk.fromIndexed)
  final def collect[B](f: PartialFunction[A, B]): Chunk[B] = Chunk.fromIterator(iterator.collect(f))
  final def collectEff[B, U](f: PartialFunction[A, B !! U]): Chunk[B] !! U = iterator.collectEff(f).map(Chunk.fromIndexed)


  // ------- find -------


  final def find(f: A => Boolean): Option[A] = collectFirst { case a if f(a) => a }
  final def findLast(f: A => Boolean): Option[A] = collectLast { case a if f(a) => a }


  final def collectFirst[B](f: PartialFunction[A, B]): Option[B] =
    var result: Option[B] = None
    val k = Breaks()
    k.breakable:
      foreachSlice: a =>
        val mb = f.lift(a)
        if mb.isDefined then
          result = mb
          k.break
    result


  final def collectLast[B](f: PartialFunction[A, B]): Option[B] =
    var result: Option[B] = None
    val k = Breaks()
    k.breakable:
      foreachSliceReverse: a =>
        val mb = f.lift(a)
        if mb.isDefined then
          result = mb
          k.break
    result


  final def collectFirstEff[B, U](f: PartialFunction[A, B !! U]): Option[B] !! U =
    iterator.collectFirstEff(f)


  final def collectLastEff[B, U](f: PartialFunction[A, B !! U]): Option[B] !! U =
    reverseIterator.collectFirstEff(f)


  // ------- combine -------


  final def ++[A2 >: A](that: Chunk[A2]): Chunk[A2] =
    if isEmpty then
      that
    else if that.isEmpty then
      this
    else
      Concat(this, that)

  final def :+[A2 >: A](value: A2): Chunk[A2] = this ++ Chunk.singleton(value)
  final def +:[A2 >: A](value: A2): Chunk[A2] = Chunk.singleton(value) ++ this


  // ------- head -------


  final def decons: Option[(A, Chunk[A])] =
    val (a, b) = splitAt(1)
    if a.isEmpty then
      None
    else
      Some(a.head, b)


  final def head: A = apply(0)
  final def last: A = apply(size - 1)
  final def headOption: Option[A] = if nonEmpty then Some(head) else None
  final def lastOption: Option[A] = if nonEmpty then Some(last) else None
  final def tail: Chunk[A] = drop(1)
  final def init: Chunk[A] = dropRight(1)


  // ------- prefix -------


  final def take(count: Int): Chunk[A] =
    if count <= 0 then
      Empty
    else
      this match
        case Singleton(_) => this
        case Slice(that, n, m) =>
          if count < m - n then
            Slice(that, n, n + count)
          else
            this
        case _ => Slice(this, 0, count)


  final def drop(count: Int): Chunk[A] =
    if count <= 0 then
      this
    else
      this match
        case Singleton(_) => Empty
        case Slice(that, n, m) =>
          if count < m - n then
            Slice(that, n + count, m)
          else
            Empty
        case _ => Slice(this, count, Int.MaxValue)


  final def takeRight(count: Int): Chunk[A] = drop(size - count)
  final def dropRight(count: Int): Chunk[A] = take(size - count)
  final def takeWhile(f: A => Boolean): Chunk[A] = take(prefixLength(f))
  final def dropWhile(f: A => Boolean): Chunk[A] = drop(prefixLength(f))
  final def takeRightWhile(f: A => Boolean): Chunk[A] = takeRight(prefixLength(f))
  final def dropRightWhile(f: A => Boolean): Chunk[A] = dropRight(suffixLength(f))
  final def takeWhileEff[U](f: A => Boolean !! U): Chunk[A] !! U = prefixLengthEff(f).map(take)
  final def dropWhileEff[U](f: A => Boolean !! U): Chunk[A] !! U = prefixLengthEff(f).map(drop)
  final def takeRightWhileEff[U](f: A => Boolean !! U): Chunk[A] !! U = suffixLengthEff(f).map(take)
  final def dropRightWhileEff[U](f: A => Boolean !! U): Chunk[A] !! U = suffixLengthEff(f).map(drop)


  final def prefixLength(f: A => Boolean): Int =
    val k = Breaks()
    var count = 0
    k.breakable:
      foreachSlice(a => if f(a) then count += 1 else k.break)
    count


  final def suffixLength(f: A => Boolean): Int =
    val k = Breaks()
    var count = 0
    k.breakable:
      foreachSliceReverse(a => if f(a) then count += 1 else k.break)
    count


  final def prefixLengthEff[U](f: A => Boolean !! U): Int !! U = iterator.segmentLengthEff(f)
  final def suffixLengthEff[U](f: A => Boolean !! U): Int !! U = reverseIterator.segmentLengthEff(f)


  // ------- split -------


  final def splitAt(i: Int): (Chunk[A], Chunk[A]) =
    if i >= 0 then
      if i < size then
        this match
          case Singleton(_) => (Empty, this)
          case Slice(that, j, k) => (Slice(that, j, i), Slice(that, i, k))
          case _ => (Slice(this, 0, i), Slice(this, i, size))
      else
        (this, Empty)
    else
      (Empty, this)


  final def split[A2 >: A](separator: A2): Chunk[Chunk[A]] = splitWhere(_ == separator)


  final def splitWhere(f: A => Boolean): Chunk[Chunk[A]] =
    if isEmpty then
      Empty
    else
      val cb = ChunkBuilder[Chunk[A]](0)
      var sliceStart = 0
      var sliceEnd = 0
      foreachSlice: a =>
        if f(a) then
          cb.addOne(slice(sliceStart, sliceEnd))
          sliceStart = sliceEnd
        sliceEnd += 1
      cb.addOne(slice(sliceStart, sliceEnd))
      cb.toChunk


  def splitWhereEff[U](f: A => Boolean !! U): Chunk[Chunk[A]] !! U =
    if isEmpty then
      Empty.pure_!!
    else
      !!.impure((ChunkBuilder[Chunk[A]](0), iterator)).flatMap: (cb, it) =>
        def loop(sliceStart: Int, sliceEnd: Int): Chunk[Chunk[A]] !! U =
          if it.hasNext then
            val a = it.next
            f(a).flatMap: b =>
              if b then
                cb.addOne(slice(sliceStart, sliceEnd))
                loop(sliceStart = sliceEnd, sliceEnd = sliceEnd + 1)
              else
                loop(sliceStart = sliceStart, sliceEnd = sliceEnd + 1)
          else
            cb.addOne(slice(sliceStart, sliceEnd))
            cb.toChunk.pure_!!
        loop(0, 0)


  // ------- fold -------


  final def foldLeft[B](initial: B)(f: (B, A) => B): B =
    var b = initial
    foreachSlice(a => b = f(b, a))
    b


  final def foldLeftEff[B, U](initial: B)(f: (B, A) => B !! U): B !! U = iterator.foldLeftEff(initial)(f)
  final def scanLeft[B](initial: B)(f: (B, A) => B): Chunk[B] = auxScanLeft(initial)(f)._1
  final def scanLeftEff[B, U](initial: B)(f: (B, A) => B !! U): Chunk[B] !! U = auxScanLeftEff(initial)(f).map(_._1)


  // ------- misc -------


  final def filterWithPrevious[A2 >: A](f: (A2, A2) => Boolean): Chunk[A] = auxFilterWithPrevious(f, None)._1
  final def filterWithPreviousEff[A2 >: A, U](f: (A, A) => Boolean !! U): Chunk[A] !! U = auxFilterWithPreviousEff(f, None).map(_._1)
  final def changesBy[B](f: A => B): Chunk[A] = auxChangesBy(f, None)._1
  final def changesByEff[B, U](f: A => B !! U): Chunk[A] !! U = auxChangesByEff(f, None).map(_._1)
  final def zipWithIndex(initial: Int): Chunk[(A, Int)] = auxZipWithIndex(0)
  final def zipWithLongIndex(initial: Long): Chunk[(A, Long)] = auxZipWithLongIndex(0L)
  final def intersperse[B >: A](separator: B): Chunk[B] = auxIntersperse(separator, false)


  // ------- convert -------


  final def toArray[A2 >: A](using classTag: ClassTag[A2]): Array[A2] =
    this match
      case Singleton(a) => Array(a)
      case Compact(arr) =>
        if classTag.runtimeClass.isAssignableFrom(arr.getClass.getComponentType) then
          arr.asInstanceOf[Array[A2]]
        else
          arr.toArray
      case Foreign(as) => as.toArray
      case Empty => Array.empty
      case _ =>
        val cb = ChunkBuilder[A2](size, classTag)
        foreachSlice3(
          onSingleton = cb.addOne,
          onCompact = cb.addArraySlice,
          onForeign = cb.addIndexedSlice,
        )
        cb.toArray


  final def toIArray[A2 >: A](using ClassTag[A2]): IArray[A2] = IArray.unsafeFromArray(toArray)


  final def toVector: Vector[A] =
    val vb = new collection.immutable.VectorBuilder[A]
    vb.sizeHint(size)
    foreachSlice(vb.addOne)
    vb.result


  final def toList: List[A] =
    val lb = new collection.mutable.ListBuffer[A]
    lb.sizeHint(size)
    foreachSlice(lb.addOne)
    lb.result


  final def charsToString(ev: A <:< Char): String =
    val sb = new collection.mutable.StringBuilder
    sb.sizeHint(size)
    asInstanceOf[Chunk[Char]].foreachSlice3(
      onSingleton = sb.addOne,
      //@#@THOV boxing
      onCompact = (arr, i, j) => sb.append(arr, i, j - i),
      onForeign = forLoopSeq(sb.addOne),
    )
    sb.result



  // ------- build -------


  final def compact: Chunk[A] =
    this match
      case Empty | Singleton(_) | Compact(_) => this
      case _ =>
        val cb = unsafeCB[A]
        foreachSlice3(
          onSingleton = cb.unsafeAddOne,
          onCompact = cb.unsafeAddArraySlice,
          onForeign = cb.unsafeAddIndexedSlice,
        )
        Compact(cb.unsafeToArray)


  final def build[A2 >: A](cb: ChunkBuilder[A2]): Unit = buildSlice(cb, 0, size)


  final def buildSlice[A2 >: A](cb: ChunkBuilder[A2], sliceStart: Int, sliceEnd: Int): Unit =
    foreachSlice3(
      onSingleton = cb.addOne,
      onCompact = cb.addArraySlice,
      onForeign = cb.addIndexedSlice,
      sliceStart = sliceStart,
      sliceEnd = sliceEnd,
    )


  // ------- stream aux -------


  final def auxFilterWithPrevious[A2 >: A](f: (A2, A2) => Boolean, prev: Option[A2]): (Chunk[A], Option[A2]) =
    this match
      case Singleton(a) => prev match
        case None => (this, None)
        case Some(a0) => if f(a0, a) then (this, Some(a)) else (Empty, prev)
      case Empty => (this, prev)
      case _ =>
        val cb = ChunkBuilder[A](1)
        var a0: A2 = null.asInstanceOf[A2]
        val sliceStart = prev match
          case None => val a = head; a0 = a; cb.addOne(a); 1
          case Some(a) => a0 = a; 0
        foreachSlice(a => { if f(a0, a) then { a0 = a; cb.addOne(a) }}, sliceStart = sliceStart)
        (cb.toChunk, Some(a0))


  final def auxFilterWithPreviousEff[A2 >: A, U](f: (A, A) => Boolean !! U, prev: Option[A2]): (Chunk[A], Option[A2]) !! U = ???


  final def auxChangesBy[B](f: A => B, prev: Option[B]): (Chunk[A], Option[B]) =
    this match
      case Singleton(a) =>
        val b = f(a)
        prev match
          case None => (this, Some(b))
          case Some(b0) => if b0 != b then (this, Some(b)) else (Empty, prev)
      case Empty => (this, prev)
      case _ =>
        val cb = ChunkBuilder[A](1)
        var b0: B = null.asInstanceOf[B]
        val sliceStart = prev match
          case None => val a = head; b0 = f(a); cb.addOne(a); 1
          case Some(b2) => b0 = b2; 0
        foreachSlice(a => { val b2 = f(a); if b0 != b2 then { b0 = b2; cb.addOne(a) }}, sliceStart = sliceStart)
        (cb.toChunk, Some(b0))


  final def auxChangesByEff[B, U](f: A => B !! U, prev: Option[B]): (Chunk[A], Option[B]) !! U =
    !!.impure(ChunkBuilder[A](1)).flatMap: cb =>
      ???


  final def auxScanLeft[B](initial: B)(f: (B, A) => B): (Chunk[B], B) =
    this match
      case Singleton(a) => { val b = f(initial, a); (Singleton(b), b) }
      case Empty => (Empty, initial)
      case _ =>
        val cb = ChunkBuilder[B](size + 1, initial.getClass).addOne(initial)
        var b: B = initial
        foreachSlice(a => { b = f(b, a); cb.addOne(b) })
        (cb.toChunk, b)


  final def auxScanLeftEff[B, U](initial: B)(f: (B, A) => B !! U): (Chunk[B], B) !! U =
    !!.impure(ChunkBuilder[B](size + 1, initial.getClass).addOne(initial)).flatMap: cb =>
      !!.impure(iterator).flatMap: it =>
        def loop(b: B): (Chunk[B], B) !! U =
          if it.hasNext then
            f(b, it.next).flatMap: b2 =>
              cb.addOne(b2)
              loop(b2)
          else
            (cb.toChunk, b).pure_!!
        loop(initial)


  final def auxZipWithIndex(initial: Int): Chunk[(A, Int)] =
    this match
      case Singleton(a) => Singleton((a, initial))
      case Empty => Empty
      case _ =>
        val cb = unsafeCB[(A, Int)]
        var index = initial
        foreachSlice(a => { cb.unsafeAddOne((a, index)); index += 1 })
        cb.toChunk


  final def auxZipWithLongIndex(initial: Long): Chunk[(A, Long)] =
    this match
      case Singleton(a) => Singleton((a, initial))
      case Empty => Empty
      case _ =>
        val cb = unsafeCB[(A, Long)]
        var index = initial
        foreachSlice(a => { cb.unsafeAddOne((a, index)); index += 1 })
        cb.toChunk


  final def auxIntersperse[B >: A](separator: B, first: Boolean): Chunk[B] =
    if isEmpty then
      this
    else
      val cb = ChunkBuilder[B](size * 2 - (!first).toInt, headClass)
      val sliceStart = if first then 0 else { cb.unsafeAddOne(separator); 1 }
      foreachSlice(a => { cb.unsafeAddOne(separator); cb.unsafeAddOne(a) }, sliceStart = sliceStart)
      cb.toChunk


  // ------- internals -------


  protected def depthCheck: Chunk[A] = if depth < Chunk.maxDepth then this else compact

  protected def headClass(i: Int): Class[?]
  protected final def headClass: Class[?] = headClass(0)


  inline private final def foreachSlice[A2 >: A](
    inline f: A2 => Unit,
    sliceStart: Int = 0,
    sliceEnd: Int = size,
  ): Unit =
    foreachSlice3(
      onSingleton = f,
      onCompact = forLoopArr(f),
      onForeign = forLoopSeq(f),
      sliceStart = sliceStart,
      sliceEnd = sliceEnd,
    )


  inline private final def foreachSliceReverse[A2 >: A](
    inline f: A2 => Unit,
    sliceStart: Int = 0,
    sliceEnd: Int = size,
  ): Unit =
    foreachSliceReverse3(
      onSingleton = f,
      onCompact = forLoopArrReverse(f),
      onForeign = forLoopSeqReverse(f),
      sliceStart = sliceStart,
      sliceEnd = sliceEnd,
    )


  inline private final def foreachSlice3[A2 >: A](
    inline onSingleton: A2 => Unit,
    inline onCompact: (Array[A2], Int, Int) => Unit,
    inline onForeign: (IndexedSeq[A2], Int, Int) => Unit,
    sliceStart: Int = 0,
    sliceEnd: Int = size,
  ): Unit =
    def loop(that: Chunk[A], sliceStart: Int, sliceEnd: Int): Unit =
      (that: @unchecked) match
        case Singleton(a) => onSingleton(a)
        case Compact(as) => onCompact(as.asInstanceOf[Array[A2]], sliceStart, sliceEnd)
        case Foreign(as) => onForeign(as, sliceStart, sliceEnd)
        case Slice(that, i, _) => loop(that, sliceStart + i, sliceEnd + i)
        case Concat(lhs, rhs) =>
          val mid = lhs.size
          if sliceStart < mid then
            loop(lhs, sliceStart, sliceEnd.min(mid))
          if mid < sliceEnd then
            loop(rhs, (sliceStart - mid).max(0), sliceEnd - mid)
        case Empty => ()
    val sliceStart2 = sliceStart.max(0)
    val sliceEnd2 = sliceEnd.min(size)
    if sliceStart2 < sliceEnd2 then
      loop(this, sliceStart2, sliceEnd2)


  inline private final def foreachSliceReverse3[A2 >: A](
    inline onSingleton: A2 => Unit,
    inline onCompact: (Array[A2], Int, Int) => Unit,
    inline onForeign: (IndexedSeq[A2], Int, Int) => Unit,
    sliceStart: Int = 0,
    sliceEnd: Int = size,
  ): Unit =
    def loop(that: Chunk[A], sliceStart: Int, sliceEnd: Int): Unit =
      (that: @unchecked) match
        case Singleton(a) => onSingleton(a)
        case Compact(as) => onCompact(as.asInstanceOf[Array[A2]], sliceStart, sliceEnd)
        case Foreign(as) => onForeign(as, sliceStart, sliceEnd)
        case Slice(that, i, _) => loop(that, sliceStart + i, sliceEnd + i)
        case Concat(lhs, rhs) =>
          val mid = lhs.size
          if mid < sliceEnd then
            loop(rhs, (sliceStart - mid).max(0), sliceEnd - mid)
          if sliceStart < mid then
            loop(lhs, sliceStart, sliceEnd.min(mid))
        case Empty => ()
    val sliceStart2 = sliceStart.max(0)
    val sliceEnd2 = sliceEnd.min(size)
    if sliceStart2 < sliceEnd2 then
      loop(this, sliceStart2, sliceEnd2)


  private final def unsafeCB[B]: ChunkBuilder[B] = unsafeCB(0)
  private final def unsafeCB[B](i: Int): ChunkBuilder[B] = ChunkBuilder[B](size, headClass(i))


//----------------------------------------------------------------


object Chunk:
  final val maxDepth = 10


  // ------- ctor -------


  def empty[A]: Chunk[A] = Empty
  val unit: Chunk[Unit] = singleton(())

  def singleton[A](a: A): Chunk[A] = Singleton(a)
  def apply[A](values: A*): Chunk[A] = fromIterator(values.iterator)

  def from[A](values: Array[A]): Chunk[A] = fromArray(values)
  def from[A](values: IndexedSeq[A]): Chunk[A] = fromIndexed(values)
  def from[A](iter: Iterator[A]): Chunk[A] = fromIterator(iter)


  def fromArray[A](values: Array[A]): Chunk[A] =
    values.size match
      case 0 => Empty
      case 1 => Singleton(values(0))
      case _ => Compact(values)


  def fromIndexed[A](values: IndexedSeq[A]): Chunk[A] =
    values.size match
      case 0 => Empty
      case 1 => Singleton(values(0))
      case _ => Foreign(values)


  def fromIterator[A](iter: Iterator[A]): Chunk[A] =
    val cb = ChunkBuilder[A](1)
    cb.addIterator(iter)
    cb.toChunk


  // ------- repr -------


  private sealed abstract class Leaf[+A] extends Chunk[A]:
    final override def depth: Int = 0


  private case object Empty extends Leaf[Nothing]:
    override def size: Int = 0
    override def iterator: Iterator[Nothing] = Iterator.empty[Nothing]
    override def apply(i: Int): Nothing = throw new java.lang.IndexOutOfBoundsException()
    override def headClass(i: Int): Class[?] = throw new java.lang.IndexOutOfBoundsException()


  private final case class Singleton[A](value: A) extends Leaf[A]:
    override def size: Int = 1
    override def iterator: Iterator[A] = Iterator(value)
    override def apply(i: Int): A = if i == 0 then value else throw new java.lang.IndexOutOfBoundsException()
    override def headClass(i: Int): Class[?] = value.getClass


  private final case class Compact[A](values: Array[A]) extends Leaf[A]:
    override def size: Int = values.size
    override def iterator: Iterator[A] = values.iterator
    override def apply(i: Int): A = values(i)
    override def headClass(i: Int): Class[?] = values.getClass.getComponentType


  private final case class Foreign[A](values: IndexedSeq[A]) extends Leaf[A]:
    override def size: Int = values.size
    override def iterator: Iterator[A] = values.iterator
    override def apply(i: Int): A = values(i)
    override def headClass(i: Int): Class[?] = values(i).getClass


  private final case class Slice[A] private (underlying: Chunk[A], startIndex: Int, endIndex: Int) extends Chunk[A]:
    override def size: Int = endIndex - startIndex
    override val depth: Int = underlying.depth + 1
    override def headClass(i: Int): Class[?] = underlying.headClass(startIndex + i)
    override def iterator: Iterator[A] = underlying.iterator.slice(startIndex, endIndex)
    override def reverseIterator: Iterator[A] = underlying.reverseIterator.slice(startIndex, endIndex)
    override def apply(i: Int): A = underlying(i - startIndex)


  object Slice:
    def apply[A](underlying: Chunk[A], startIndex: Int, endIndex: Int): Chunk[A] =
      new Slice(underlying, startIndex, endIndex).depthCheck


  private final case class Concat[A] private (lhs: Chunk[A], rhs: Chunk[A]) extends Chunk[A]:
    override def size: Int = lhs.size + rhs.size
    override val depth: Int = lhs.depth.max(rhs.depth)
    override def iterator: Iterator[A] = lhs.iterator ++ rhs.iterator
    override def reverseIterator: Iterator[A] = rhs.reverseIterator ++ lhs.reverseIterator

    override def apply(i: Int): A =
      val n = lhs.size
      if i < n then
        lhs.apply(i)
      else
        rhs.apply(i - n)

    override def headClass(i: Int): Class[?] =
      val n = lhs.size
      if i < n then
        lhs.headClass(i)
      else
        rhs.headClass(i - n)


  object Concat:
    def apply[A](lhs: Chunk[A], rhs: Chunk[A]): Chunk[A] =
      new Concat(lhs, rhs).depthCheck
