package common

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}


case class FutureEither[+L, +R](future: Future[Either[L, R]]) extends AnyVal {

  def flatMap[A >: L, B](f: R => FutureEither[A,B])(implicit ec: ExecutionContext): FutureEither[A,B] = {
    FutureEither(future.flatMap {
      case Right(r) => f(r).future
      case Left(l) => Future.successful(Left(l))
    })
  }

  def lflatMap[B, A >: R](f: L => FutureEither[B,A])(implicit ec: ExecutionContext): FutureEither[B,A] = {
    FutureEither(future.flatMap {
      case Right(r) => Future.successful(Right(r))
      case Left(l) => f(l).future
    })
  }

  def map[B](f: R => B)(implicit ec: ExecutionContext): FutureEither[L,B] = {
    FutureEither(future.map {
      case Right(r) => Right(f(r))
      case Left(l) => Left(l)
    })
  }

  def lmap[B](f: L => B)(implicit ec: ExecutionContext): FutureEither[B,R] = {
    FutureEither(future.map {
      case Right(r) => Right(r)
      case Left(l) => Left(f(l))
    })
  }

  def isRight(implicit ec: ExecutionContext): Future[Boolean] = future.map(_.isRight)

  def fold[X](fa: (L) ⇒ X, fb: (R) ⇒ X)(implicit ec: ExecutionContext): Future[X] = {
    future.map(_.fold(fa, fb))
  }

  def orElse[A >: L, B >: R](f: => FutureEither[A,B])(implicit ec: ExecutionContext): FutureEither[A,B] = {
    FutureEither(future.flatMap {
      case Right(r) => Future.successful(Right(r))
      case Left(l) => f.future
    })
  }
}

object FutureEither {

  implicit class EitherImprovements[+L, +R](either: Either[L, R]) {
    def toFutureEither: FutureEither[L,R] = FutureEither(Future.successful(either))
  }

  implicit class FutureImprovements[+L, +R](future: Future[Either[L, R]]) {
    def toFutureEither: FutureEither[L,R] = FutureEither(future)
  }

  implicit class FutureEitherImprovements[+L, +R](futureEither: FutureEither[L, R])(implicit ec: ExecutionContext) {
    def toFuture: Future[Either[L,R]] = futureEither.fold[Either[L, R]](l => Left(l), r => Right(r))
  }

}
