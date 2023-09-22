package beam.protocol
import turbolift.!!
import turbolift.Extensions._


sealed trait PullDown[+A, -U]

object PullDown:
  case object Stop extends PullDown[Nothing, Any]
  final case class Emit[A, U](element: A, next: PullUp[A, U]) extends PullDown[A, U]

  val stopPure = Stop.pure_!!


type PullUp[A, U] = Boolean => PullDown[A, U] !! U

object PullUp:
  inline def cond[A, U](inline ifTrue: PullDown[A, U] !! U, inline ifFalse: PullDown[A, U] !! U): PullUp[A, U] =
    (more: Boolean) => if more then ifTrue else ifFalse

  def invert[A, U](pull: PullDown[A, U] !! U): PullUp[A, U] = cond(pull, PullDown.stopPure)

  val alwaysStop: PullUp[Nothing, Any] = _ => PullDown.stopPure

  def expectStop[A, R, U](pull: PullUp[A, U], value: R): R !! U =
    pull(false).flatMap:
      case PullDown.Stop => value.pure_!!
      case _ => sys.error("Dangling upstream")
