package turbolift.data
import org.specs2.mutable._
import turbolift.!!
import turbolift.Extensions._



class ChunkTests extends Specification:
  sequential

  class Foo(n: Int) { override def toString = s"Foo($n)" }
  class Bar(n: Int) extends Foo(n) { override def toString = s"Bar($n)" }
  val foo = Foo(100)
  val bar = Bar(200)


  "box" >> {
    val range = 1 to 5
    val viu: Vector[Int] = range.toVector
    val aiu: Array[Int] = range.toArray
    val vbu: Vector[Byte] = range.iterator.map(_.toByte).toVector
    val abu: Array[Byte] = range.iterator.map(_.toByte).toArray

    val cviu: Chunk[Int] = Chunk.from(viu)
    val caiu: Chunk[Int] = Chunk.from(aiu)
    val cvbu: Chunk[Byte] = Chunk.from(vbu)
    val cabu: Chunk[Byte] = Chunk.from(abu)


    "add" >>{
      val r = (range ++ range).toVector
      (caiu ++ caiu).toArray.toVector === r
    }


    "map" >>{
      def f(n: Int) = n * 10
      val r = range.map(f).toVector
      viu.map(f).toArray.toVector === r
      aiu.map(f).toArray.toVector === r
    }

    "filter" >>{
      def f(n: Int) = n % 2 == 0
      val r = range.map(f).toVector
      viu.map(f).toArray.toVector === r
      aiu.map(f).toArray.toVector === r
    }

    "sub" >>{
      val bar = Bar(200)
      val foos = Chunk.from(Array(foo))
      val bars = Chunk.from(Array(bar))
      (foos ++ bars).toArray.toVector === Vector(foo, bar)
      (bars ++ foos).toArray.toVector === Vector(bar, foo)
    }

    "mix" >>{
      Chunk.from((1 to 3).toArray).map {
        case 1 => 10
        case 2 => "x"
        case 3 => true
      }.toArray.toVector === Vector(10, "x", true)
    }

    "widen Bar <: Foo" >> {
      "[foo, bar]" >>{
        Chunk.from((1 to 2).toArray).map {
          case 1 => foo
          case 2 => bar
        }.toArray.toVector === Vector(foo, bar)
      }

      "[bar, foo]" >>{
        Chunk.from((1 to 2).toArray).map {
          case 1 => bar
          case 2 => foo
        }.toArray.toVector === Vector(bar, foo)
      }
    }
  }


