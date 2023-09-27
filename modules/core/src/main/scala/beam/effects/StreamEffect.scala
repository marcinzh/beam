package beam.effects
import turbolift.{!!, Signature, Effect}
import turbolift.Extensions._
import beam.protocol._


sealed trait StreamSignature[O] extends Signature:
  def write(value: O): Unit !@! ThisEffect
  def exit: Nothing !@! ThisEffect


trait StreamEffect[O] extends Effect[StreamSignature[O]] with StreamSignature[O]:
  final override def write(value: O): Unit !! this.type = perform(_.write(value))
  final override def exit: Nothing !! this.type = perform(_.exit)


  final def handler[U]: ThisHandler[[_] =>> Unit, [_] =>> PullDown[O, U], Any] =
    new Free.Const.Stateless[Unit, [_] =>> PullDown[O, U]] with Sequential with StreamSignature[O]:
      override def onReturn[A](a: Unit) = PullDown.stopPure

      override def write(value: O): Unit !@! ThisEffect = k => PullDown.Emit(value, PullUp.invert(k(()))).pure_!!

      override def exit: Nothing !@! ThisEffect = k => PullDown.stopPure

    .toHandler
