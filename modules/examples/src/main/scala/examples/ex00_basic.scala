//> using scala "3.3.5"
//> using dep "io.github.marcinzh::beam-core:0.16.0"
package examples
import beam._


@main def ex00_basic =
  println:
    Stream
      .from(1 to 20)
      .take(10)
      .reduce(_ + _)
    .run

  println:
    Stream
      .repeat(1)
      .take(10)
      .scanLeft(0)(_ + _)
      .toVector
      .map(_.mkString("[", ", " ,"]"))
    .run

  println:
    Stream
      .repeat("x")
      .map(_.toUpperCase)
      .take(10)
      .scanLeft("")(_ ++ _)
      .filter(x => x.size % 2 == 1)
      .toVector
      .map(_.mkString("[", ", " ,"]"))
    .run
