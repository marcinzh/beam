package devel
import turbolift.!!
import turbolift.effects.Console
import turbolift.Extensions._
import beam._


object Main:
  def main(args: Array[String]): Unit =
    args.headOption.getOrElse("") match
      case ""|"1" => example1()
      case "2" => example2()
      case "3" => example3()


  val lorem = """
    Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et 
    dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
    ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
    fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
    mollit anim id est laborum.
  """.split("\\s+").filter(_.nonEmpty).toVector


  def example1() =
    println:
      val s = Stream.from(1 to 10)
      val k = Sink.fold[Int, Int](0)(_ + _)
      (s >-> k).run


  def example2() =
    println:
      val p =
        Pipe.coroutine[String, String, Any]: Fx =>
          def loop: Unit !! Fx.type =
            for
              a <- Fx.readOrElseExit
              b <- Fx.readOrElse(Fx.write(a) &&! Fx.exit)
              _ <- Fx.write(s"$a, $b")
              _ <- loop
            yield ()
          loop

      val s = Stream.from(lorem)
      val k = Sink.fold("")((a, b) => s"$a\n$b")
      (s >-> p >-> k).run


  def example3() =
    val fibos: Stream[Long, Any] =
      Stream.coroutine: fx =>
        def loop(a: Long, b: Long): Unit !! fx.type =
          for
            _ <- fx.write(a)
            _ <- !!.when(a % 2 == 0)(fx.write(a))
            _ <- loop(b, a + b)
          yield ()
        loop(1, 1)

    fibos.take(10).toVector
    .tapEff(xs => Console.println(xs.mkString(" ")))
    .handleWith(Console.handler)
    .unsafeRun
