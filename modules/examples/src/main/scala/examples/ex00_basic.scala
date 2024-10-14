//> using scala "3.3.4"
//> using dep "io.github.marcinzh::beam-core:0.9.0-SNAPSHOT"
package examples
import beam._


@main def ex00_basic =
  println:
    Stream
      .from(1 to 20)
      .take(10)
      .reduce(_ + _)
    .run
