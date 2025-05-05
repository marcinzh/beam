[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.marcinzh/beam-core_3)](https://mvnrepository.com/artifact/io.github.marcinzh/beam-core) [![javadoc](https://javadoc.io/badge2/io.github.marcinzh/beam-core_3/javadoc.svg)](https://javadoc.io/doc/io.github.marcinzh/beam-core_3)

# Beam

Purely functional streams, implemented with algebraic effects and handlers.


&nbsp;

- ⚗️ 🔬 🧪 &nbsp; 𝑷𝑹𝑶𝑻𝑶𝑻𝒀𝑷𝑬 &nbsp;   🚧 WIP 🚧
- Uses Scala 3.
- Uses [Turbolift](https://marcinzh.github.io/turbolift/) as effect system.
- Inspired by [Stream](https://share.unison-lang.org/@unison/base/code/releases/3.34.0/latest/types/data/Stream) from [Unison](https://www.unison-lang.org/) language.

&nbsp;

# Examples

Runnable with [`scala-cli`](https://scala-cli.virtuslab.org/).

> [!IMPORTANT]
> Turbolift requires **Java 11** or newer.

```scala
//> using scala "3.3.5"
//> using dep "io.github.marcinzh::beam-core:0.12.0"
//> using dep "io.github.marcinzh::turbolift-bindless:0.112.0"
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
```

See also [examples](modules/examples/src/main/scala/examples/) folder.
