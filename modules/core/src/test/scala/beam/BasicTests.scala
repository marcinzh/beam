package beam
import org.specs2.mutable._
import turbolift.!!
import turbolift.Extensions._
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

    "scanLeft" >>{
      Stream.repeat("*").take(3).scanLeft("")(_ ++ _).toVector.run === Vector("", "*", "**", "***")
    }

    "zip" >> {
      val aa = 1 to 4
      val aa1 = 1 to 5
      val bb = 'a' to 'd'
      val bb1 = 'a' to 'e'
      "same length" >>{
        Stream.from(aa).zip(Stream.from(bb)).toVector.run === aa.zip(bb)
      }
      "left longer" >>{
        Stream.from(aa1).zip(Stream.from(bb)).toVector.run === aa.zip(bb)
      }
      "right longer" >>{
        Stream.from(aa).zip(Stream.from(bb1)).toVector.run === aa.zip(bb)
      }
    }
  }
