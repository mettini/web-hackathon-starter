package services.auth

import models.auth._
import play.api.Configuration
import play.api.mvc.Request
import play.api.cache.AsyncCacheApi
import common.FutureEither
import common.FutureEither._
import scala.concurrent.Future
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

case class Session(authToken: String, userId: Long)

trait SessionService {

  def loginCredentialRepo: LoginCredentialRepo

  def validateSession(authToken: String)(implicit request: Request[_]): FutureEither[String, Session] = getSession(authToken).flatMap {
    case Some(session) => Future.successful(Right(session))
    case None => loginCredentialRepo.findValidByAuthToken(authToken).map { loginCredential =>
      val session = Session(authToken, loginCredential.userId)
      setSession(session).map {
        case true => Right(session)
        case false => Left("there was a problem while saving the session")
      }
    }.getOrElse(Future.successful(Left("invalid.authToken")))
  }.toFutureEither

  def getSession(authToken: String): Future[Option[Session]]
  def deleteSession(authToken: String): Future[Boolean]
  def setSession(session: Session): Future[Boolean]
}

class MemorySessionService(cacheApi: AsyncCacheApi,
                           config: Configuration,
                           lcr: LoginCredentialRepo) extends SessionService {

  lazy val sessionDuration = config.get[Int]("app.session.expiration.duration").seconds

  override def loginCredentialRepo: LoginCredentialRepo = lcr

  def getSession(authToken: String): Future[Option[Session]] = cacheApi.get[Session](authToken).map { sessionOpt =>
    sessionOpt.map(session => {
      cacheApi.set(session.authToken, session, sessionDuration)
      session
    })
  }

  def deleteSession(authToken: String): Future[Boolean] = cacheApi.get[Session](authToken).map { sessionOpt =>
    if (sessionOpt.isEmpty) false
    else {
      cacheApi.remove(authToken)
      true
    }
  }

  def setSession(session: Session): Future[Boolean] = cacheApi.get[Session](session.authToken).map { sessionOpt =>
    if (sessionOpt.isDefined) false
    else {
      cacheApi.set(session.authToken, session, sessionDuration)
      true
    }
  }

}
