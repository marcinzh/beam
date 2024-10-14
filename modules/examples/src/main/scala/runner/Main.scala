package runner
import Macro.example


object Main:
  def main(args: Array[String]): Unit =
    args.headOption.flatMap(_.toIntOption).flatMap(list.unapply) match
      case Some((f, _)) => f()
      case None =>
        println("Pass an example index as a parameter to run it.")
        for ((f, n), i) <- list.zipWithIndex do
          println(s"  $i: $n")


  private val list =
    import examples._
    List(
      example(ex00_basic),
      example(ex01_sourceEffect),
      example(ex02_pipeEffect),
      example(ex03_resource),
    )
