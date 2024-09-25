package beam.effects
import turbolift.{!!, Signature, Effect}
import turbolift.Extensions._
import beam.internals.Step
import beam.Stream


sealed trait PipeSignature[I, O] extends Signature:
  def read: (I | EndOfInput) !! ThisEffect
  def write(value: O): Unit !! ThisEffect
  def exit: Nothing !! ThisEffect


trait PipeEffect[I, O] extends Effect[PipeSignature[I, O]] with PipeSignature[I, O]:
  final override def read: (I | EndOfInput) !! this.type = perform(_.read)
  final override def write(value: O): Unit !! this.type = perform(_.write(value))
  final override def exit: Nothing !! this.type = perform(_.exit)

  final def tryRead: Option[I] !! this.type =
    read.map:
      case EndOfInput => None
      case i: I @unchecked => Some(i)

  final def readOrElse[I2 >: I, U <: this.type](comp: => I2 !! U): I2 !! U =
    read.flatMap:
      case EndOfInput => comp
      case i: I @unchecked => i.pure_!!

  final val readOrElseExit: I !! this.type = readOrElse(exit)


  final def handler[U](initial: Stream[I, U]): ThisHandler[Const[Unit], Const[Stream[O, U]], U] =
    new impl.Stateful[Const[Unit], Const[Step[O, U]], U] with impl.Sequential with PipeSignature[I, O]:
      override type Local = Step[I, U] !! U

      override def onInitial = initial.unwrap.pure_!!

      override def onReturn(a: Unit, s: Local) = Step.endPure

      override def read: (I | EndOfInput) !! ThisEffect =
        Control.captureGet: (k, s) =>
          s.flatMap:
            case Step.End => k(EndOfInput)
            case Step.Emit(i, s2) => k(i, s2)

      override def write(value: O): Unit !! ThisEffect =
        Control.captureGet: (k, s) =>
          Step.Emit(value, Control.strip(k((), s))).pure_!!

      override def exit: Nothing !! ThisEffect =
        Control.abort(Step.End)

    .toHandler
    .mapK([_] => (step: Step[O, U]) => Stream.wrap(step.pure_!!))
