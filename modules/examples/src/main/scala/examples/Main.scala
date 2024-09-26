package devel


object Main:
  def main(args: Array[String]): Unit =
    args.headOption.flatMap(_.toIntOption).filter(cases.isDefinedAt(_)) match
      case None => describe()
      case Some(x) => cases(x.toInt)()

  def describe() =
    println("Usage: `examples/run <INDEX_OF_EXAMPLE>`")
    println("Available examples:")
    for (o, i) <- cases.zipWithIndex do
      println(s"  $i - ${o.name}")


  val cases = List(
    Example0,
    Example1,
    Example2,
    Example3,
  )



trait Example extends Product with Function0[Unit]:
  def description: String = ""
  final def name = productPrefix
