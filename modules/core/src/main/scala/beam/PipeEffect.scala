package beam
import turbolift.{!!, Signature, Effect}
import turbolift.Extensions._
import Syntax._


sealed trait PipeSignature[I, O] extends Signature:
  def accept: Option[I] !! ThisEffect
  def acceptChunk: Chunk[I] !! ThisEffect
  def emit(value: O): Unit !! ThisEffect
  def emitChunk(chunk: Chunk[O]): Unit !! ThisEffect
  def exit: Nothing !! ThisEffect


trait PipeEffect[I, O] extends Effect[PipeSignature[I, O]] with PipeSignature[I, O]:
  final override def accept: Option[I] !! ThisEffect = perform(_.accept)
  final override def acceptChunk: Chunk[I] !! ThisEffect = perform(_.acceptChunk)
  final override def emit(value: O): Unit !! this.type = perform(_.emit(value))
  final override def emitChunk(chunk: Chunk[O]): Unit !! this.type = perform(_.emitChunk(chunk))
  final override def exit: Nothing !! this.type = perform(_.exit)

  final def acceptOrElse[I2 >: I, U <: this.type](comp: => I2 !! U): I2 !! U =
    accept.flatMap:
      case None => comp
      case Some(i) => i.pure_!!

  final val acceptOrElseExit: I !! this.type = acceptOrElse(exit)


  final def defaultHandler[U](input: Stream[I, U]): ThisHandler[Const[Unit], Const[Stream[O, U]], U] =
    new impl.Stateful[Const[Unit], Const[Stream[O, U]], U] with impl.Sequential with PipeSignature[I, O]:
      override type Local = (Chunk[I], Stream[I, U] !! U)

      override def onInitial = input.deconsChunk.pure_!!

      override def onReturn(a: Unit, s: Local) = Stream.emptyEff

      override def emit(value: O): Unit !! ThisEffect = emitChunk(Chunk(value))

      override def emitChunk(chunk: Chunk[O]): Unit !! ThisEffect =
        Control.capture: k =>
          !!.pure:
            chunk !::: Control.strip(k(()))

      override def accept: Option[I] !! ThisEffect =
        Control.captureGet: (k, s) =>
          val (ch, kk) = s
          if ch.nonEmpty then
            k(Some(ch.head), (ch.tail, kk))
          else
            kk.flatMap(_.deconsSkip).flatMap:
              case None => k(None)
              case Some(ch, kk2) => k(Some(ch.head), (ch.tail, kk2))

      override def acceptChunk: Chunk[I] !! ThisEffect =
        Control.captureGet: (k, s) =>
          val (ch, kk) = s
          if ch.nonEmpty then
            k(ch, (Chunk(), kk))
          else
            kk.flatMap(_.deconsSkip).flatMap:
              case None => k(Chunk.empty)
              case Some(ch, kk2) => k(ch, (Chunk(), kk2))

      override def exit: Nothing !! ThisEffect =
        Control.abort(Stream.empty)

    .toHandler
