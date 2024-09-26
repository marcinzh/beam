package devel
import scala.util.chaining._
import turbolift.!!
import turbolift.effects.{IO, FinalizerEffectIO, Console}
import turbolift.io.ResourceFactory
import turbolift.bindless._
import beam._


//@#@TODO not good
case object Example3 extends Example:
  override def apply(): Unit =
    `do`:
      val foo = makeStream("foo", 0).!.take(10)
      val bar = makeStream("bar", 100).!.take(5)
      val qux = foo ++ bar
      qux.drain.!
    .handleWith(Fin.handler)
    .handleWith(Console.handler)
    .runIO


  def makeResource(name: String) = ResourceFactory(
    Console.println(s"OPEN `$name`"),
    _ => Console.println(s"CLOSE `$name`"),
  )

  case object Fin extends FinalizerEffectIO[Console & IO]
  type Fin = Fin.type

  def makeStream(name: String, initial: Int): Stream[Int, Console] !! Fin =
    Fin.use(makeResource(name)).as:
      Source: fx =>
        def loop(n: Int): Unit !! (Console & fx.type) =
          `do`:
            Console.println(s"Emit from `$name`: $n").!
            fx.emit(n).!
            loop(n + 1).!
        loop(initial)
