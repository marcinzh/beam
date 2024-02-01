[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.marcinzh/beam-core_3/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.marcinzh/beam-core_3)

# Beam

Purely functional streams, implemented with algebraic effects and handlers.

Data processing is done with entities such as `Stream`, `Pipe` and `Sink`, which can employ arbitrary sets of effects.

Optionally, they can be constructed as **coroutines**.


&nbsp;

- ⚗️ 🔬 🧪 &nbsp; 𝑷𝑹𝑶𝑻𝑶𝑻𝒀𝑷𝑬 &nbsp;   🚧 WIP 🚧
- Uses Scala 3.
- Uses [Turbolift](https://marcinzh.github.io/turbolift/) as effect system.

&nbsp;

# Example

Stuttering Fibonacci sequence: when the number is even, emit it twice.

```scala
//> using scala "3.3.1"
//> using dep "io.github.marcinzh::beam-core:0.4.0"
import turbolift.!!
import turbolift.effects.Console
import beam._

@main def main =
  val fibos: Stream[Long, Any] =
    // Encapsulates a local instance of `StreamEffect`, which is then handled on exit.
    Stream.coroutine: fx =>
      def loop(a: Long, b: Long): Unit !! fx.type =
        for
          _ <- fx.write(a)
          _ <- !!.when(a % 2 == 0)(fx.write(a))
          _ <- loop(b, a + b)
        yield ()
      loop(1, 1)
    
  fibos.take(10).toVector
  .flatTap(xs => Console.println(xs.mkString(" ")))
  .handleWith(Console.handler)
  .unsafeRun
```
