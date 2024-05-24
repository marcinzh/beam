package beam.internals
import turbolift.!!


sealed trait Step[+A, -U]


object Step:
  case class Emit[+A, -U](value: A, next: Step[A, U] !! U) extends Step[A, U]
  case object End extends Step[Nothing, Any]

  val endPure: Step[Nothing, Any] !! Any = !!.pure(End)


  object Syntax:
    extension [A](thiz: A)
      inline def ::![B >: A, U](that: Step[B, U] !! U): Step[B, U] !! U = !!.pure(Step.Emit(thiz, that))
      inline def ::!?[B >: A, U](that: => Step[B, U] !! U): Step[B, U] !! U = !!.pure(Step.Emit(thiz, !!.impureEff(that)))
