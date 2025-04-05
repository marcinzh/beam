package beam
import turbolift.{!!, Signature, Effect}
import turbolift.Extensions._


trait SourceSignature[O, R] extends Signature:
  def emit(value: O): Unit !! ThisEffect
  def exit(value: R): Nothing !! ThisEffect


trait SourceEffectExt[O, R] extends Effect[SourceSignature[O, R]] with SourceSignature[O, R]:
  final override def emit(value: O): Unit !! this.type = perform(_.emit(value))
  final override def exit(value: R): Nothing !! this.type = perform(_.exit(value))
  final def exit(using ev: Unit =:= R): Nothing !! ThisEffect = exit(ev(()))

  abstract class Stateless[U] extends StatelessReturn[Unit, U]:
    override def onReturn() = !!.unit

  abstract class Stateful[S, U](initial: S) extends StatefulReturn[S, Unit, U](initial):
    override def onReturn(s: Local) = !!.unit

  abstract class StatelessReturn[R, U] extends impl.Stateless[Const[Unit], Const[R], U] with impl.Sequential with SourceSignature[O, R]:
    override def onReturn(x: Unit) = onReturn()
    override def exit(r: R): Nothing !! ThisEffect = Control.abort(r)
    def onReturn(): R !! ThisEffect

  abstract class StatefulReturn[S, R, U](initial: S) extends impl.Stateful[Const[Unit], Const[R], U] with impl.Sequential with SourceSignature[O, R]:
    final override type Local = S
    override def onInitial = initial.pure_!!
    override def onReturn(x: Unit, s: Local) = onReturn(s)
    override def exit(r: R): Nothing !! ThisEffect = Control.abort(r)
    def onReturn(s: Local): R !! ThisEffect



trait SourceEffect[O] extends SourceEffectExt[O, Unit]:
  final def upCast[O2 >: O]: SourceEffect[O2] = asInstanceOf[SourceEffect[O2]]


object SourceEffect:
  case object FxNothing extends SourceEffect[Nothing]
  case object FxUnit extends SourceEffect[Unit]
