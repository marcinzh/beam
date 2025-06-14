package beam
import turbolift.{!!, Signature, Effect}
import turbolift.Extensions._
import turbolift.data.Chunk


sealed trait PipeSignature[I, O, R] extends Signature:
  def accept: Option[I] !! ThisEffect
  def emit(value: O): Unit !! ThisEffect
  def exit(value: R): Nothing !! ThisEffect


trait PipeEffectExt[I, O, R] extends Effect[PipeSignature[I, O, R]] with PipeSignature[I, O, R]:
  final override def accept: Option[I] !! this.type = perform(_.accept)
  final override def emit(value: O): Unit !! this.type = perform(_.emit(value))
  final override def exit(value: R): Nothing !! this.type = perform(_.exit(value))

  final def emit1[O2](value: O2)(using ev: Chunk[O2] <:< O): Unit !! this.type = emit(ev(Chunk.singleton(value)))
  final def emitOpt(value: Option[O]): Unit !! this.type = value.fold(!!.unit)(emit)
  final def emitIfNonEmpty[O2](values: Chunk[O2])(using ev: Chunk[O2] <:< O): Unit !! this.type = !!.when(values.nonEmpty)(emit(ev(values)))
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


  final def defaultHandler[U](Fx: SourceEffect[O], input: Stream[I, U]) =
    new impl.Stateful[Const[R], Const[R], U & Fx.type] with impl.Sequential with PipeSignature[I, O, R]:
      override type Local = Stream[I, U]
      override def onInitial = input.pure_!!
      override def onReturn(r: R, s: Local) = s.abort &&! r.pure_!!

      override def accept: Option[I] !! ThisEffect =
        Local.getsEff(_.decons).flatMap:
          case None => !!.none
          case Some((i, s2)) => Local.put(s2).as(Some(i))

      override def emit(o: O): Unit !! ThisEffect = Fx.emit(o)

      override def exit(r: R): Nothing !! ThisEffect = Local.getsEff(_.abort) &&! Control.abort(r)
    .toHandler


trait PipeEffect[I, O] extends PipeEffectExt[I, O, Unit]
