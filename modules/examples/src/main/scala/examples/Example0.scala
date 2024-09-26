package devel
import scala.util.chaining._
import beam._


case object Example0 extends Example:
  override def apply(): Unit =
    Stream
      .from(1 to 20)
      .take(10)
      .reduce(_ + _)
      .run
      .tap(println)
