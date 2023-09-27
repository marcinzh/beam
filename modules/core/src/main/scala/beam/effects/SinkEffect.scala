package beam.effects
import turbolift.{!!, Signature, Effect}
import turbolift.Extensions._
import beam.protocol._


sealed trait SinkSignature[I, R] extends Signature:
  def read: (I | EndOfInput) !@! ThisEffect
  def exit(value: R): Nothing !@! ThisEffect


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


  final def handler[U](initial: PullUp[I, U]): ThisHandler[[_] =>> R, [_] =>> R, U] =
    type S = PullUp[I, U]

    new Const.Stateful[R, S, [_] =>> R, U] with Sequential with SinkSignature[I, R]:
      override def onReturn[A](r: R, s: S) = PullUp.expectStop(s, r)

      override def read: (I | EndOfInput) !@! ThisEffect =
        (k, s) => s(true).flatMap:
          case PullDown.Stop => k(EndOfInput)
          case PullDown.Emit(i, s2) => k(i, s2)

      override def exit(r: R): Nothing !@! ThisEffect = (k, s) => PullUp.expectStop(s, r)

    .toHandler(initial)
