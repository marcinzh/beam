package beam
import turbolift.{!!, Signature, Effect}
import turbolift.Extensions._
import Syntax._


sealed trait SourceSignature[O] extends Signature:
  def emit(value: O): Unit !! ThisEffect
  def emitChunk(chunk: Chunk[O]): Unit !! ThisEffect
  def exit: Nothing !! ThisEffect


trait SourceEffect[O] extends Effect[SourceSignature[O]] with SourceSignature[O]:
  final override def emit(value: O): Unit !! this.type = perform(_.emit(value))
  final override def emitChunk(chunk: Chunk[O]): Unit !! this.type = perform(_.emitChunk(chunk))
  final override def exit: Nothing !! this.type = perform(_.exit)


  final def defaultHandler[U]: ThisHandler[Const[Unit], Const[Stream[O, U]], Any] =
    new impl.Stateless[Const[Unit], Const[Stream[O, U]], Any] with impl.Sequential with SourceSignature[O]:
      override def onReturn(aa: Unit): Stream[O, U] !! Any = Stream.emptyEff

      override def emit(value: O): Unit !! ThisEffect = emitChunk(Chunk(value))

      override def emitChunk(chunk: Chunk[O]): Unit !! ThisEffect =
        Control.capture: k =>
          !!.pure:
            chunk !::: Control.strip(k(()))

      override def exit: Nothing !! ThisEffect =
        Control.abort(Stream.empty)

    .toHandler
