//> using scala "3.3.5"
//> using dep "io.github.marcinzh::beam-core:0.16.0"
//> using dep "io.github.marcinzh::turbolift-bindless:0.118.0"
package examples
import turbolift.!!
import turbolift.effects.Console
import turbolift.bindless._
import beam._

/** Stuttering Fibonacci sequence: when the number is even, emit it twice. */

@main def ex01_sourceEffect =
  val stream: Stream[Long, Any] =
    Source: fx =>
      def loop(a: Long, b: Long): Unit !! fx.type =
        `do`:
          fx.emit(a).!
          if a % 2 == 0 then fx.emit(a).!
          loop(b, a + b).!
      loop(1, 1)

  stream
    .take(20)
    .foreachEff(i => Console.println(i.toString))
  .handleWith(Console.handler)
  .runIO
