package devel
import beam._


object Main:
  def main(args: Array[String]): Unit =
    args.headOption.getOrElse("") match
      case _ => example1()


  def example1() =
    println:
      val s = Stream.from(1 to 10)
      val k = Sink.fold[Int, Int](0)(_ + _)
      (s >-> k).run
