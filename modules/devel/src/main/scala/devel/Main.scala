package devel


object Main:
  def main(args: Array[String]): Unit =
    args.headOption.getOrElse("") match
      case _ => println("hello world")
