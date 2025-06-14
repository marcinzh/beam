package turbolift.data
import turbolift.!!


private object Utils:
  inline def forLoopArr[A](inline f: A => Unit)(values: Array[A], i0: Int, i1: Int): Unit =
    var i = i0
    while i < i1 do
      f(values(i))
      i += 1


  inline def forLoopSeq[A](inline f: A => Unit)(values: IndexedSeq[A], i0: Int, i1: Int): Unit =
    var i = i0
    while i < i1 do
      f(values(i))
      i += 1


  //@#@ unused
  def forLoopArrEff[A, U](f: A => Unit !! U)(values: Array[A], i0: Int, i1: Int): Unit !! U =
    def loop(i: Int): Unit !! U =
      !!.when(i < i1)(f(values(i)) &&! loop(i + 1))
    loop(i0)



  //@#@ unused
  def forLoopSeqEff[A, U](f: A => Unit !! U)(values: IndexedSeq[A], i0: Int, i1: Int): Unit !! U =
    def loop(i: Int): Unit !! U =
      !!.when(i < i1)(f(values(i)) &&! loop(i + 1))
    loop(i0)


  inline def forLoopArrReverse[A](inline f: A => Unit)(values: Array[A], i0: Int, i1: Int): Unit =
    var i = i1 - 1
    while i >= i0 do
      f(values(i))
      i -= 1


  inline def forLoopSeqReverse[A](inline f: A => Unit)(values: IndexedSeq[A], i0: Int, i1: Int): Unit =
    var i = i1 - 1
    while i >= i0 do
      f(values(i))
      i -= 1


  //@#@ unused
  def forLoopArrReverseEff[A, U](f: A => Unit !! U)(values: Array[A], i0: Int, i1: Int): Unit !! U =
    def loop(i: Int): Unit !! U =
      !!.when(i >= i0)(f(values(i)) &&! loop(i - 1))
    loop(i1 - 1)


  //@#@ unused
  def forLoopSeqReverseEff[A, U](f: A => Unit !! U)(values: IndexedSeq[A], i0: Int, i1: Int): Unit !! U =
    def loop(i: Int): Unit !! U =
      !!.when(i >= i0)(f(values(i)) &&! loop(i - 1))
    loop(i1 - 1)


  extension (thiz: Boolean)
    def toInt: Int = if thiz then 1 else 0
