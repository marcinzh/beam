package beam.effects
import turbolift.{!!, Signature, Effect}
import turbolift.Extensions._
import beam.internals.Step
import beam.Stream


sealed trait StreamSignature[O] extends Signature:
  def write(value: O): Unit !@! ThisEffect
  def exit: Nothing !@! ThisEffect


trait StreamEffect[O] extends Effect[StreamSignature[O]] with StreamSignature[O]:
  final override def write(value: O): Unit !! this.type = perform(_.write(value))
  final override def exit: Nothing !! this.type = perform(_.exit)


  final def handler[U]: ThisHandler[[_] =>> Unit, [_] =>> Stream[O, U], Any] =
    new impl.Const.Stateless[Unit, [_] =>> Step[O, U], Any] with impl.Sequential with StreamSignature[O]:
      override def onReturn(aa: Unit): Step[O, U] !! Any = Step.endPure

      override def write(value: O): Unit !@! ThisEffect =
        k => Step.Emit(value, k(())).pure_!!

      override def exit: Nothing !@! ThisEffect =
        k => Step.endPure

    .toHandler
    .mapK([_] => (step: Step[O, U]) => Stream.wrap(step.pure_!!))
