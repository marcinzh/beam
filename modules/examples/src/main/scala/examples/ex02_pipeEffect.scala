//> using scala "3.3.5"
//> using dep "io.github.marcinzh::beam-core:0.16.0"
//> using dep "io.github.marcinzh::turbolift-bindless:0.114.0"
package examples
import scala.util.chaining._
import turbolift.!!
import turbolift.bindless._
import turbolift.effects.Console
import beam._

/** Transforms a stream of words, by grouping words into pairs */

@main def ex02_pipeEffect =
  def intoPairs[A, U](missing: A): Stream[A, U] => Stream[(A, A), U] =
    Pipe: fx =>
      def loop: Unit !! fx.type =
        `do`:
          fx.accept.! match
            case None => ()
            case Some(a) =>
              fx.accept.! match
                case None => fx.emit(a, missing).!
                case Some(b) =>
                  fx.emit((a, b)).!
                  loop.!
      loop

  val lorem = """
    Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et 
    dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
    ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
    fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
    mollit anim id est laborum.
  """.split("\\s+").filter(_.nonEmpty)

  extension (thiz: String) def pad = thiz.padTo(20, ' ')

  Stream
    .from(lorem)
    .pipe(intoPairs(missing ="<END>"))
    .foreachEff { case (a, b) => Console.println(s"| ${a.pad} | ${b.pad} |") }
  .handleWith(Console.handler)
  .runIO
