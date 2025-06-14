package turbolift.data
import java.lang.{ClassCastException, ArrayStoreException}
import java.lang.reflect.{Array => ReflectArray}
import scala.reflect.ClassTag


final class ChunkBuilder[A] private (
  private var arrayVar: Array[A],
  private val initialCapacity: Int,
):
  private var sizeVar: Int = 0
  def size: Int = sizeVar


  def toArray(using classTag: ClassTag[A]): Array[A] =
    val elemClass = arrayVar.getClass.getComponentType
    if classTag.runtimeClass.isAssignableFrom(elemClass) then
      unsafeToArray
    else
      val resultArray = classTag.newArray(sizeVar)
      if !elemClass.isPrimitive then
        System.arraycopy(arrayVar, 0, resultArray, 0, size)
        arrayVar
      else
        var i = 0
        val n = size
        val arr = arrayVar
        while i < n do
          resultArray(i) = arr(i)
          i += 1
        resultArray


  def unsafeToArray: Array[A] =
    if size == arrayVar.size then
      arrayVar
    else
      arrayVar.slice(0, size)


  def toChunk: Chunk[A] =
    sizeVar match
      case 0 => Chunk.empty
      case 1 => Chunk.singleton(arrayVar(0))
      case _ => Chunk.fromArray(unsafeToArray)


  def addOne(value: A): ChunkBuilder[A] =
    val o = grow(1)(value.getClass)
    try
      arrayVar(o) = value
    catch ClassCastException =>
      widen(o)(value.getClass)
      arrayVar(o) = value
    this


  def unsafeAddOne(value: A): Unit =
    val o = unsafeGrow(1)
    try
      arrayVar(o) = value
    catch ClassCastException =>
      widen(o)(value.getClass)
      arrayVar(o) = value


  def addArraySlice(values: Array[A], startIndex: Int, endIndex: Int): ChunkBuilder[A] =
    val startIndex2 = startIndex.max(0)
    val endIndex2 = endIndex.min(values.size)
    val d = endIndex2 - startIndex2
    if d > 0 then
      var o = grow(d)(values.getClass.getComponentType)
      try
        System.arraycopy(values, startIndex2, arrayVar, o, d)
      catch ArrayStoreException =>
        widen(o)(values.getClass.getComponentType)
        copyFromArray(values, startIndex2, o, d)
    this


  def unsafeAddArraySlice(values: Array[A], startIndex: Int, endIndex: Int): Unit =
    val d = endIndex - startIndex
    var o = unsafeGrow(d)
    try
      System.arraycopy(values, startIndex, arrayVar, o, d)
    catch ArrayStoreException =>
      widen(o)(values.getClass.getComponentType)
      copyFromArray(values, startIndex, o, d)


  def addIndexedSlice(values: IndexedSeq[A], startIndex: Int, endIndex: Int): ChunkBuilder[A] =
    val startIndex2 = startIndex.max(0)
    val endIndex2 = endIndex.min(values.size)
    val d = endIndex2 - startIndex2
    if d > 0 then
      var o = grow(d)(values.head.getClass)
      try
        copyFromIndexed(values, startIndex2, endIndex2, o)
      catch ClassCastException =>
        //@#@TODO still can fail
        widen(o)(values.head.getClass)
        copyFromIndexed(values, startIndex2, endIndex2, o)
    this


  def unsafeAddIndexedSlice(values: IndexedSeq[A], startIndex: Int, endIndex: Int): Unit =
    val d = endIndex - startIndex
    var o = unsafeGrow(d)
    try
      copyFromIndexed(values, startIndex, endIndex, o)
    catch ClassCastException =>
      //@#@TODO still can fail
      widen(o)(values.head.getClass)
      copyFromIndexed(values, startIndex, endIndex, o)


  def addIterator(it: Iterator[A]): ChunkBuilder[A] =
    while it.hasNext do
      addOne(it.next())
    this


  def addChunk(chunk: Chunk[A]): ChunkBuilder[A] =
    chunk.build(this)
    this


  def addChunkSlice(chunk: Chunk[A], startIndex: Int, endIndex: Int): ChunkBuilder[A] =
    chunk.buildSlice(this, startIndex, endIndex)
    this


  private def copyFromArray(values: Array[A], fromIndex: Int, toIndex: Int, count: Int): Unit =
    val arr = arrayVar
    var i = 0
    while i < count do
      arr(toIndex + i) = values(fromIndex + i)
      i += 1


  private def copyFromIndexed(values: IndexedSeq[A], fromIndex: Int, toIndex: Int, count: Int): Unit =
    val arr = arrayVar
    var i = 0
    while i < count do
      arr(toIndex + i) = values(fromIndex + i)
      i += 1


  private inline def unsafeGrow(d: Int): Int =
    val i = sizeVar
    sizeVar += d
    i


  private inline def grow(d: Int)(inline getNewClass: Class[?]): Int =
    if arrayVar == null then
      init(d, getNewClass)
    else
      doGrow(d)


  private def init(d: Int, newClass: Class[?]): Int =
    val initialCapacity2 = initialCapacity.max(d)
    arrayVar = ReflectArray.newInstance(newClass, initialCapacity2).asInstanceOf[Array[A]]
    sizeVar = d
    0


  private def doGrow(d: Int): Int =
    val oldArray = arrayVar
    val oldSize = sizeVar
    val newSize = oldSize + d
    this.sizeVar = newSize
    if newSize > oldArray.size then
      val newCapacity = 1 << (32 - Integer.numberOfLeadingZeros(newSize - 1))
      val newArray = ReflectArray.newInstance(oldArray.getClass.getComponentType, newCapacity).asInstanceOf[Array[A]]
      System.arraycopy(oldArray, 0, newArray, 0, oldSize)
      this.arrayVar = newArray
    oldSize


  private def widen(oldSize: Int)(newClass: Class[?]): Unit =
    val oldArray = arrayVar
    val oldClass = oldArray.getClass.getComponentType
    val lubClass =
      if oldClass.isAssignableFrom(newClass) then
        oldClass
      else if newClass.isAssignableFrom(oldClass) then
        newClass
      else
        classOf[Any]
    val newArray = ReflectArray.newInstance(lubClass, oldArray.size).asInstanceOf[Array[A]]
    this.arrayVar = newArray
    copyFromArray(oldArray, 0, 0, oldSize)


  // def reset(newCapacity: Int = 0): ChunkBuilder[A] =
  //   //@#@TODO must get rid of `initialCapacity` before it works for general case
  //   assert(initialCapacity == 0)
  //   arrayVar = null
  //   sizeVar = 0
  //   this


object ChunkBuilder:
  def apply[A](initialCapacity: Int): ChunkBuilder[A] =
    require(initialCapacity >= 0)
    new ChunkBuilder[A](null, initialCapacity)


  def apply[A](initialCapacity: Int, elementClass: Class[?]): ChunkBuilder[A] =
    require(initialCapacity >= 0)
    val array = ReflectArray.newInstance(elementClass, initialCapacity).asInstanceOf[Array[A]]
    new ChunkBuilder[A](array, initialCapacity)


  def apply[A](initialCapacity: Int, tag: ClassTag[A]): ChunkBuilder[A] =
    require(initialCapacity >= 0)
    val array = tag.newArray(initialCapacity)
    new ChunkBuilder[A](array, initialCapacity)
