package beam
import org.specs2.mutable._
import turbolift.!!
import turbolift.Extensions._
import beam.Syntax._



class BasicTests extends Specification:
  "pure" >> {
    "cons" >> {
      (1 ?:: 2 ?:: Stream.from(3 to 5)).toVector.run === Vector(1, 2, 3, 4, 5)
    }

    "repeat & take" >> {
      Stream.repeat(42).take(3).toVector.run === Vector(42, 42, 42)
    }

    "drop" >> {
      Stream.from(1 to 10).drop(7).toVector.run === Vector(8, 9, 10)
    }

    "concat" >> {
      (Stream.from(1 to 3) ++ Stream.from(8 to 10)).toVector.run === Vector(1, 2, 3, 8, 9, 10)
    }

    "filter" >> {
      Stream.from(1 to 10).filter(_ % 2 == 0).toVector.run === Vector(2, 4, 6, 8, 10)
    }

    "map" >> {
      Stream.from(1 to 3).map(_ * 10).toVector.run === Vector(10, 20, 30)
    }

    "flatMap" >> {
      Stream.from(1 to 3).flatMap(i => Stream.from((i*100) to (i*100 +2))).toVector.run === Vector(100, 101, 102, 200, 201, 202, 300, 301, 302)
    }
  }
