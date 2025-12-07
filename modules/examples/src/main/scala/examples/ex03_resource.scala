//> using scala "3.3.7"
//> using dep "io.github.marcinzh::beam-core:0.20.0"
//> using dep "io.github.marcinzh::turbolift-bindless:0.124.0"
package examples
import turbolift.!!
import turbolift.bindless._
import turbolift.effects.{Finalizer, Console}
import turbolift.data.Resource
import beam._


@main def ex03_resource =
  def makeResource(name: String) = Resource(
    Console.println(s"OPEN `$name`"),
    Console.println(s"CLOSE `$name`"),
  )

  def makeStream(name: String, initial: Int): Stream[Int, Console] =
    Source: fx =>
      `do`:
        val dummyResource = Finalizer.use(makeResource(name)).!
        def loop(n: Int): Unit !! (fx.type & Console) =
          `do`:
            Console.println(s"Emit from `$name`: $n").!
            fx.emit(n).!
            loop(n + 1).!
        loop(initial).!
      .finalized


  `do`:
    val foo = makeStream("foo", 0).take(7)
    val bar = makeStream("bar", 100).take(5)
    val qux = foo ++ bar
    qux.toVector.!
  .tapEff(x => Console.println(x.toString))
  .handleWith(Console.handler)
  .runIO
