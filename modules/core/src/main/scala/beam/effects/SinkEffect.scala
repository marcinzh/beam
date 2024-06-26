package beam.effects
import turbolift.{!!, Signature, Effect}
import turbolift.Extensions._
import beam.internals.Step
import beam.Stream


sealed trait SinkSignature[I, R] extends Signature:
  def read: (I | EndOfInput) !! ThisEffect
  def exit(value: R): Nothing !! ThisEffect


trait SinkEffect[I, R] extends Effect[SinkSignature[I, R]] with SinkSignature[I, R]:
  final override def read: (I | EndOfInput) !! this.type = perform(_.read)
  final override def exit(value: R): Nothing !! this.type = perform(_.exit(value))

  final def tryRead: Option[I] !! this.type =
    read.map:
      case EndOfInput => None
      case i: I @unchecked => Some(i)

  final def readOrElse[I2 >: I, U <: this.type](comp: => I2 !! U): I2 !! U =
    read.flatMap:
      case EndOfInput => comp
      case i: I @unchecked => i.pure_!!

  final def readOrElseExit(r: => R): I !! this.type = readOrElse(exit(r))
  final def readOrElseExit(using Unit <:< R): I !! this.type = readOrElse(exit)
  final def exit(using ev: Unit <:< R): Nothing !! this.type = exit(ev(()))


  final def handler[U](initial: Stream[I, U]): ThisHandler[Const[R], Const[R], U] =
    new impl.Stateful[Const[R], Const[R], U] with impl.Sequential with SinkSignature[I, R]:
      override type Local = Step[I, U] !! U

      override def onInitial = initial.unwrap.pure_!!

      override def onReturn(r: R, s: Local) = r.pure_!!

      override def read: (I | EndOfInput) !! ThisEffect =
        Control.captureGet: (k, s) =>
          s.flatMap:
            case Step.End => k.resume(EndOfInput)
            case Step.Emit(i, s2) => k.resume(i, s2)

      override def exit(r: R): Nothing !! ThisEffect =
        Control.abort(r)

    .toHandler
