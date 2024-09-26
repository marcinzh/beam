package devel
import scala.util.chaining._
import turbolift.!!
import turbolift.bindless._
import beam._


case object Example2 extends Example:
  override def apply(): Unit =
    val fibos: Stream[Long, Any] =
      Source: fx =>
        def loop(a: Long, b: Long): Unit !! fx.type =
          `do`:
            fx.emit(a).!
            if a % 2 == 0 then
              fx.emit(a).!
            loop(b, a + b).!
        loop(1, 1)

    fibos
      .take(20)
      .toList
      .run
      .tap(println)
