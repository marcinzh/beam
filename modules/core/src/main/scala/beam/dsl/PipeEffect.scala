package beam.dsl
import turbolift.{!!, Signature, Effect}
import turbolift.Extensions._
import beam.protocol._


sealed trait PipeSignature[I, O] extends Signature:
  def read: (I | EndOfInput) !@! ThisEffect
  def write(value: O): Unit !@! ThisEffect
  def exit: Nothing !@! ThisEffect


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


  final def handler[U](initial: PullUp[I, U]): ThisHandler[[_] =>> Unit, [_] =>> PullDown[O, U], U] =
    type S = PullUp[I, U]
    def stopBoth(s: S): PullDown[O, U] !! U  = PullUp.expectStop(s, PullDown.Stop)

    new Const.Stateful[Unit, S, [_] =>> PullDown[O, U], U] with Sequential with PipeSignature[I, O]:
      override def onReturn[A](a: Unit, s: S) = stopBoth(s)

      override def read: (I | EndOfInput) !@! ThisEffect =
        (k, s) => s(true).flatMap:
          case PullDown.Stop => k(EndOfInput)
          case PullDown.Emit(i, s2) => k(i, s2)

      override def write(value: O): Unit !@! ThisEffect = 
        (k, s) =>
          val cont = PullUp.cond(k(()), stopBoth(s))
          PullDown.Emit(value, cont).pure_!!

      override def exit: Nothing !@! ThisEffect = (k, s) => stopBoth(s)

    .toHandler(initial)
