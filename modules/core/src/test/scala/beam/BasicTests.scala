package beam
import java.lang.Character
import scala.collection.immutable.Queue
import org.specs2.mutable._
import turbolift.!!
import turbolift.Extensions._
import turbolift.effects.IO
import turbolift.io.AtomicVar
import beam.Syntax._



class BasicTests extends Specification:
  "pure" >> {
    "cons" >>{
      (1 !:: 2 !:: Stream.from(3 to 5)).toVector.run === (1 to 5).toVector
    }

    "decons" >>{
      val s0 = Stream.from(1 to 6)
      s0.decons.flatMap:
        case None => List(-1).pure_!!
        case Some((a, s1)) => s1.decons.flatMap:
          case None => List(a, -2).pure_!!
          case Some((b, s2)) => s2.decons.flatMap:
            case None => List(a, b, -3).pure_!!
            case Some((c, s3)) => s3.toList.map(xs => List(a, b, c) ++ List(999) ++ xs)
      .run === List(1,2,3, 999, 4,5,6)
    }

    "repeat & take" >>{
      Stream.repeat(42).take(3).toVector.run === Vector.fill(3)(42)
    }

    "drop" >>{
      Stream.from(1 to 10).drop(7).toVector.run === Vector(8, 9, 10)
    }

    "dropWhile" >>{
      Stream.from(1 to 10).dropWhile(_ <= 5).toVector.run === (6 to 10).toVector
    }

    "takeWhile" >>{
      Stream.from(1 to 10).takeWhile(_ <= 5).toVector.run === (1 to 5).toVector
    }

    "concat" >>{
      (Stream.from(1 to 3) ++ Stream.from(8 to 10)).toVector.run === Vector(1, 2, 3, 8, 9, 10)
    }

    "filter" >>{
      Stream.from(1 to 10).filter(_ % 2 == 0).toVector.run === Vector(2, 4, 6, 8, 10)
    }

    "map" >>{
      Stream.from(1 to 3).map(_ * 10).toVector.run === Vector(10, 20, 30)
    }

    "flatMap" >>{
      Stream.from(1 to 3).flatMap(i => Stream.from((i*100) to (i*100 +2))).toVector.run === Vector(100, 101, 102, 200, 201, 202, 300, 301, 302)
    }

    "mapConcat" >>{
      Stream.from(1 to 3).mapConcat(i => List(i * 10, i * 100)).toVector.run === Vector(10, 100, 20, 200, 30, 300)
    }

    "scanLeft" >>{
      Stream.repeat("*").take(3).scanLeft("")(_ ++ _).toVector.run === Vector("", "*", "**", "***")
    }

    "zip & interleave" >> {
      val aa = 1 to 4
      val aa1 = 1 to 5
      val bb = 'a' to 'd'
      val bb1 = 'a' to 'e'
      val cc = aa.zip(bb)
      val dd = cc.flatMap((a, b) => Vector(a, b))
      "zip" >> {
        "same length"  >>{ Stream.from(aa).zip(Stream.from(bb)).toVector.run === cc }
        "left longer"  >>{ Stream.from(aa1).zip(Stream.from(bb)).toVector.run === cc }
        "right longer" >>{ Stream.from(aa).zip(Stream.from(bb1)).toVector.run === cc }
      }
      "interleave" >> {
        "same length"  >>{ Stream.from(aa).interleave(Stream.from(bb)).toVector.run === dd }
        "left longer"  >>{ Stream.from(aa1).interleave(Stream.from(bb)).toVector.run === dd }
        "right longer" >>{ Stream.from(aa).interleave(Stream.from(bb1)).toVector.run === dd }
      }
      "zipWithIndex" >> { Stream.from(aa).zipWithIndex.toVector.run === aa.zipWithIndex }
    }

    "window" >> {
      "window > size" >>{ Stream(1, 2, 3).window(5).toVector.run === Vector() }
      "window = size" >>{ Stream(1, 2, 3).window(3).toVector.run === Vector(Queue(1, 2, 3)) }
      "window < size" >>{
        Stream(1, 2, 3, 4, 5).window(3).toVector.run === Vector(
          Queue(1, 2, 3),
          Queue(2, 3, 4),
          Queue(3, 4, 5),
        )
      }
      "empties" >> {
        "window = 0" >>{ Stream(1, 2, 3).window(0).toVector.run === Vector() }
        "window = 1" >>{ Stream(1, 2, 3).window(1).toVector.run === (1 to 3).map(Queue(_)) }
        "size = 0"   >>{ Stream.empty.window(3).toVector.run === Vector[Queue[?]]() }
      }
    }

    "intersperse" >> {
      "intersperse 1" >>{ Stream(1, 2, 3).intersperse(0).toVector.run === Vector(1, 0, 2, 0, 3) }
      "intersperse 3" >>{ Stream(1, 2, 3).intersperse(10, 0, 20).toVector.run === Vector(10, 1, 0, 2, 0, 3, 20) }
      "intersperse 1 empty" >>{ Stream[Int]().intersperse(0).toVector.run === Vector() }
      "intersperse 3 empty" >>{ Stream[Int]().intersperse(10, 0, 20).toVector.run === Vector(10, 20) }
    }

    "take & map" >>{
      Stream.from(1 to 5).take(3).map(_ * 10).toVector.run === (1 to 3).map(_ * 10)
      Stream.from(1 to 5).map(_ * 10).take(3).map(_ + 1).toVector.run === (1 to 3).map(_ * 10).map(_ + 1)
    }

    "split" >> {
      "inner" >>{ Stream.from("abc,def,,ghi").split(',').toVector.run.map(_.mkString) === Vector("abc", "def", "", "ghi") }
      "outer" >>{ Stream.from(",abc,def,").split(',').toVector.run.map(_.mkString) === Vector("", "abc", "def", "") }
    }
    "splitWhere" >> {
      "inner" >>{ Stream.from("abcDefGHi").splitWhere(Character.isUpperCase).toVector.run.map(_.mkString) === Vector("abc", "Def", "G", "Hi") }
      "outer" >>{ Stream.from("AbcD").splitWhere(Character.isUpperCase).toVector.run.map(_.mkString) === Vector("", "Abc", "D") }
    }

    "mergeSorted" >> {
      val aa = Vector(1, 2, 10, 20)
      val bb = Vector(3, 4, 30, 40)
      val cc = (aa ++ bb).sorted
      "left first"  >>{ Stream.from(aa).mergeSorted(Stream.from(bb)).toVector.run === cc }
      "right first" >>{ Stream.from(bb).mergeSorted(Stream.from(aa)).toVector.run === cc }
    }
  }


  "IO" >> {
    "merge" >>{
      val aa = Stream.from(1 to 5 by 2).tapEff(_ => IO.sleep(100))
      val bb = Stream.from(2 to 6 by 2).tapEff(_ => IO.sleep(100))
      val cc = Stream.emptyEff(IO.sleep(50)) ++ bb
      aa.merge(cc).toVector.runIO.get === (1 to 6)
    }

    "tapEff" >>{
      AtomicVar(Vector.empty[Int]).flatMap: avar =>
        val ss = Stream.from(1 to 3).tapEff(i => avar.modify(_ :+ i))
        ss.toVector **! avar.get
      .runIO.get === (Vector(1, 2, 3), Vector(1, 2, 3))
    }
  }
