package common.actions

import models.auth.LoginCredentialRepo
import org.slf4j.MDC
import models.user.{UserRepo, User}
import services.auth.SessionService

import scala.util.Either
import scala.concurrent.{Future, ExecutionContext}
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json.Json
import play.api.i18n.{MessagesApi, Messages}
import common.FutureEither
import common.FutureOption

case class LoggedRequest[A](user: User, authToken: String, request: Request[A]) extends WrappedRequest[A](request)
case class MaybeLoggedRequest[A](user: Option[User], authToken: Option[String], request: Request[A]) extends WrappedRequest[A](request)

sealed trait RequestType
object RequestType {
  case object Web extends RequestType
  case object Api extends RequestType
}

object AuthActions {

  import scala.concurrent.ExecutionContext.Implicits.global

  import common.actions.RequestType._
  import common.FutureEither._
  import common.FutureOption._

  def Logged(userId: Long,
             sessionService: SessionService,
             requestType: RequestType,
             userRepo: UserRepo,
             messages: MessagesApi): ActionRefiner[Request, LoggedRequest] = new ActionRefiner[Request, LoggedRequest] {

    implicit val implicitMessages: Messages = implicitly[Messages]

    override def refine[A](request: Request[A]): Future[Either[Result, LoggedRequest[A]]] = {
      (for {
        authToken <- getAuthToken(requestType, request).toRight(unauthorized("authToken.notfound")).toFutureEither
        session <- validateSession(authToken)(request)
        _ <- Either.cond(session.userId == userId, userId, forbidden("authToken.forbidden")).toFutureEither
        user <- userRepo.findById(session.userId).toRight(forbidden("user.notfound")).toFutureEither
      } yield {
        MDC.put("userId", user.id.toString)
        LoggedRequest(user, authToken, request)
      }).toFuture
    }

    def validateSession[A](authToken: String) (implicit request: Request[A]): FutureEither[Result, services.auth.Session] =
      sessionService.validateSession(authToken).lmap(forbidden)

    private def badRequest(msg: String): Result = requestType match {
      case Web => BadRequest(views.html.error(400, msg))
      case Api => BadRequest(Json.obj("error" -> true, "message" -> msg))
    }

    private def forbidden(msg: String): Result = requestType match {
      case Web => Forbidden(views.html.error(403, msg))
      case Api => Forbidden(Json.obj ("error" -> true, "message" -> msg))
    }

    private def unauthorized(msg: String): Result = requestType match {
      case Web => Unauthorized(views.html.error(401, msg))
      case Api => Unauthorized(Json.obj ("error" -> true, "message" -> msg))
    }

    override def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  }

  def MaybeLogged(sessionService: SessionService, 
                  requestType: RequestType,
                  userRepo: UserRepo,
                  messages: MessagesApi): ActionRefiner[Request, MaybeLoggedRequest] = new ActionRefiner[Request, MaybeLoggedRequest] {

    implicit val implicitMessages: Messages = implicitly[Messages]

    override def refine[A](request: Request[A]): Future[Either[Result, MaybeLoggedRequest[A]]] = {
      (for {
        authToken <- Future.successful(getAuthToken(requestType, request)).toFutureOption
        session <- sessionService.validateSession(authToken)(request).toFutureOption
        user <- userRepo.findById(session.userId).toFutureOption
      } yield {
        MDC.put("userId", session.userId.toString)
        Right(MaybeLoggedRequest(Some(user), Some(authToken), request))
      }).future.map {
        case Some(result) => result
        case None => Right(MaybeLoggedRequest(None, None, request))
      }
    }

    override def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  }

  private def getAuthToken[A](requestType: RequestType, request: Request[A]): Option[String] = requestType match {
    case Web => request.cookies.get("at") match {
      case Some(Cookie("at", authToken, _, _, _, _, _, _)) => Some(authToken)
      case _ => None
    }
    case Api => request.headers.get("X-AuthToken")
  }

}

