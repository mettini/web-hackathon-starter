package common

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

case class FutureOption[+A](future: Future[Option[A]]) extends AnyVal {

  def flatMap[B](f: A => FutureOption[B])(implicit ec: ExecutionContext): FutureOption[B] = {
    val newFuture = future.flatMap {
      case Some(a) => f(a).future
      case None => Future.successful(None)
    }
    FutureOption(newFuture)
  }

  def map[B](f: A => B)(implicit ec: ExecutionContext): FutureOption[B] = {
    FutureOption(future.map(option => option map f))
  }

}

object FutureOption {

  implicit class OptionImprovements[+A](option: Option[A]) {
    def toFutureOption: FutureOption[A] = FutureOption(Future.successful(option))
  }

  implicit class FutureOImprovements[+A](future: Future[Option[A]]) {
    def toFutureOption: FutureOption[A] = FutureOption(future)
  }

  implicit class FutureEitherOptionsImprovements[+A, +B](futureEither: FutureEither[A, B])(implicit ec: ExecutionContext) {
    def toFutureOption: FutureOption[B] = FutureOption(futureEither.fold(_ => None, v => Some(v)))
  }

}
