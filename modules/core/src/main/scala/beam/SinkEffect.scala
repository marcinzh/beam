package beam
import turbolift.{!!, Signature, Effect}
import turbolift.Extensions._


sealed trait SinkSignature[I, R] extends Signature:
  def accept: Option[I] !! ThisEffect
  def exit(value: R): Nothing !! ThisEffect


trait SinkEffect[I, R] extends Effect[SinkSignature[I, R]] with SinkSignature[I, R]:
  final override def accept: Option[I] !! this.type = perform(_.accept)
  final override def exit(value: R): Nothing !! this.type = perform(_.exit(value))
  final def exit(using ev: Unit =:= R): Nothing !! ThisEffect = exit(ev(()))

  final def acceptOrElse[A >: I, U <: this.type](value: => A): A !! U =
    accept.map:
      case None => value
      case Some(i) => i

  final def acceptOrElseEff[A >: I, U <: this.type](comp: => A !! U): A !! U =
    accept.flatMap:
      case None => comp
      case Some(i) => i.pure_!!

  final def acceptOrElseExit(value: => R): I !! this.type = acceptOrElseEff(exit(value))


  final def defaultHandler[U](input: Stream[I, U]) =
    new impl.Stateful[Const[R], Const[R], U] with impl.Sequential with SinkSignature[I, R]:
      override type Local = Stream[I, U]
      override def onInitial = input.pure_!!
      override def onReturn(r: R, s: Local) = s.abort &&! r.pure_!!

      override def accept: Option[I] !! ThisEffect =
        Local.getsEff(_.decons).flatMap:
          case None => !!.none
          case Some((i, s2)) => Local.put(s2).as(Some(i))

      override def exit(r: R): Nothing !! ThisEffect = Local.getsEff(_.abort) &&! Control.abort(r)
    .toHandler
