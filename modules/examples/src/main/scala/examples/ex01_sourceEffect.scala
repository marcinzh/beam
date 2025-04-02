//> using scala "3.3.4"
//> using dep "io.github.marcinzh::beam-core:0.9.0-SNAPSHOT"
//> using dep "io.github.marcinzh::turbolift-bindless:0.104.0"
package examples
import turbolift.!!
import turbolift.bindless._
import beam._


/*
 * Stuttering Fibonacci sequence: when the number is even, emit it twice.
 */

@main def ex01_sourceEffect =
  val fibos: Stream[Long, Any] =
    Source: fx =>
      def loop(a: Long, b: Long): Unit !! fx.type =
        `do`:
          fx.emit(a).!
          if a % 2 == 0 then
            fx.emit(a).!
          loop(b, a + b).!
      loop(1, 1)

  println:
    fibos
      .take(20)
      .toList
    .run
