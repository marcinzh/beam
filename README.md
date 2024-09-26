[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.marcinzh/beam-core_3/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.marcinzh/beam-core_3)

# Beam

Purely functional streams, implemented with algebraic effects and handlers.


&nbsp;

- ⚗️ 🔬 🧪 &nbsp; 𝑷𝑹𝑶𝑻𝑶𝑻𝒀𝑷𝑬 &nbsp;   🚧 WIP 🚧
- Uses Scala 3.
- Uses [Turbolift](https://marcinzh.github.io/turbolift/) as effect system.

&nbsp;

# Examples

Runnable with [`scala-cli`](https://scala-cli.virtuslab.org/). Turbolift requires ⚠️**Java 11**⚠️ or newer.

Stuttering Fibonacci sequence: when the number is even, emit it twice.

```scala
//> using scala "3.3.3"
//> using dep "io.github.marcinzh::beam-core:0.8.0"
//> using dep "io.github.marcinzh::turbolift-bindless:0.98.0"
import turbolift.!!
import turbolift.effects.Console
import turbolift.bindless._
import beam._

@main def main =
  val fibos: Stream[Long, Any] =
    Source: fx =>
      def loop(a: Long, b: Long): Unit !! fx.type =
        `do`:
          fx.emit(a).!
          if a % 2 == 0 then fx.emit(a).!
          loop(b, a + b).!
      loop(1, 1)

  fibos.take(20).toVector
  .tapEff(xs => Console.println(xs.mkString(" ")))
  .handleWith(Console.handler)
  .runIO
```

See also [examples](modules/examples/src/main/scala/examples/) folder. Runnable with `sbt`:
```sh
sbt examples/run
```