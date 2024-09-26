package beam
import turbolift.{!!, Signature, Effect}
import turbolift.Extensions._


sealed trait SinkSignature[I, R] extends Signature:
  def accept: Option[I] !! ThisEffect
  def acceptChunk: Chunk[I] !! ThisEffect
  def exit(value: R): Nothing !! ThisEffect


trait SinkEffect[I, R] extends Effect[SinkSignature[I, R]] with SinkSignature[I, R]:
  final override def accept: Option[I] !! ThisEffect = perform(_.accept)
  final override def acceptChunk: Chunk[I] !! ThisEffect = perform(_.acceptChunk)
  final override def exit(value: R): Nothing !! this.type = perform(_.exit(value))

  final def acceptOrElse[I2 >: I, U <: this.type](comp: => I2 !! U): I2 !! U =
    accept.flatMap:
      case None => comp
      case Some(i) => i.pure_!!

  final def acceptOrElseExit(r: => R): I !! this.type = acceptOrElse(exit(r))
  final def acceptOrElseExit(using Unit <:< R): I !! this.type = acceptOrElse(exit)
  final def exit(using ev: Unit <:< R): Nothing !! this.type = exit(ev(()))


  object handlers:
    def withRemainder[U](input: Stream[I, U]): ThisHandler[Const[R], Const[(R, Stream[I, U])], U] = defaultHandler(input)
    
    def dropRemainder[U](input: Stream[I, U]): ThisHandler[Const[R], Const[R], U] =
      defaultHandler(input).mapK([_] => (pair: (R, Stream[I, U])) => pair._1)
    
    def justRemainder[U](input: Stream[I, U]): ThisHandler[Const[R], Const[Stream[I, U]], U] =
      defaultHandler(input).mapK([_] => (pair: (R, Stream[I, U])) => pair._2)


  final def defaultHandler[U](input: Stream[I, U]): ThisHandler[Const[R], Const[(R, Stream[I, U])], U] =
    new impl.Stateful[Const[R], Const[(R, Stream[I, U])], U] with impl.Sequential with SinkSignature[I, R]:
      override type Local = (Chunk[I], Stream[I, U] !! U)

      override def onInitial = input.deconsChunk.pure_!!

      override def onReturn(r: R, s: Local) = makeRemainder(r, s).pure_!!

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

      override def exit(r: R): Nothing !! ThisEffect =
        Local.get.flatMap: s =>
          Control.abort(makeRemainder(r, s))

      def makeRemainder(r: R, s: Local): (R, Stream[I, U]) =
        val (ch, kk) = s
        (r, Stream.consChunkEff(ch, kk))

    .toHandler
