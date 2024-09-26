package devel
import scala.util.chaining._
import turbolift.!!
import turbolift.bindless._
import beam._


case object Example1 extends Example:
  extension (thiz: String) def pad = thiz.padTo(Data.loremMax, ' ')
  def missing = "<<END>>".pad

  override def apply(): Unit =
    Stream
      .from(Data.lorem)
      .pipe(intoPairs(default ="<END>"))
      .foreach { case (a, b) => println(s"| ${a.pad} | ${b.pad} |") }
      .drain
      .run


  def intoPairs[A, U](default: A): Stream[A, U] => Stream[(A, A), U] =
    Pipe: fx =>
      def loop: Unit !! fx.type =
        `do`:
          fx.accept.! match
            case None => ()
            case Some(a) =>
              fx.accept.! match
                case None => fx.emit(a, default).!
                case Some(b) =>
                  fx.emit((a, b)).!
                  loop.!
      loop
