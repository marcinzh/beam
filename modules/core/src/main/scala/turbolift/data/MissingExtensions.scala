package turbolift.data
import turbolift.!!
import turbolift.Extensions._
import turbolift.data.Utils._


//// will be moved to turbolift.internals.extensions
object MissingExtensions:
  extension [A](thiz: IterableOnce[A])
    def collectFirstEff[B, U](f: PartialFunction[A, B !! U]): Option[B] !! U =
      !!.impure(thiz.iterator).flatMap: it =>
        def loop: Option[B] !! U =
          if it.hasNext then
            f.lift(it.next()) match
              case Some(comp) => comp.map(Some(_))
              case None => !!.impureEff(loop)
          else
            !!.none
        loop


    def collectLastEff[B, U](f: PartialFunction[A, B !! U]): Option[B] !! U =
      !!.impure(thiz.iterator).flatMap: it =>
        def loop(acc: Option[B]): Option[B] !! U =
          if it.hasNext then
            f.lift(it.next()) match
              case Some(comp) => comp.flatMap(a => loop(Some(a)))
              case None => !!.impureEff(loop(acc))
          else
            !!.pure(acc)
        loop(None)


    def segmentLengthEff[U](f: A => Boolean !! U): Int !! U =
      !!.impure(thiz.iterator).flatMap: it =>
        def loop(acc: Int): Int !! U =
          if it.hasNext then
            f(it.next()).flatMap(x => loop(acc + x.toInt))
          else
            !!.pure(acc)
        loop(0)
