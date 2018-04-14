package common.actions

import models.auth.LoginCredentialRepo
import org.slf4j.MDC
import models.user.{UserRepo, User}
import services.auth.SessionService

import scala.util.Either
import scala.concurrent.{Future, ExecutionContext}
import play.api.mvc._
import play.api.Logger
import play.api.mvc.Results._
import play.api.libs.json.Json
import play.api.i18n.{MessagesApi, Messages}
import common.FutureEither
import common.FutureOption

import models.admin._

case class AdminRequest[A](adminUser: AdminUser, adminUserSession: AdminUserSession, request: Request[A]) extends WrappedRequest[A](request)

object AdminActions {

  import scala.concurrent.ExecutionContext.Implicits.global

  import common.actions.RequestType._
  import common.FutureEither._
  import common.FutureOption._

  def Admin(requiredRoles: List[String],
            adminUserSessionRepo: AdminUserSessionRepo,
            adminRoleRepo: AdminRoleRepo,
            adminUserRepo: AdminUserRepo,
            messages: MessagesApi): ActionRefiner[Request, AdminRequest] = new ActionRefiner[Request, AdminRequest] {

    implicit val implicitMessages: Messages = implicitly[Messages]

    override def refine[A](request: Request[A]): Future[Either[Result, AdminRequest[A]]] = {
      (for {
        sessionKey <- getSessionKey(request).toRight(toLogin("error.sessionKey.notFound")).toFutureEither
        adminUserSession <- adminUserSessionRepo.findBySessionKey(sessionKey).toRight(toLogin("error.session.notFound")).toFutureEither
        _ <- Either.cond(adminUserSession.sessionStatus != AdminUserSessionStatus.Finished, "ok", toLogin("sessionKey.finished")).toFutureEither
        adminUser <- adminUserRepo.findById(adminUserSession.adminUser.id).toRight(unauthorized("error.adminUser.notFound")).toFutureEither
        _ <-  Either.cond(adminUser.hasAnyRole(requiredRoles), "ok", forbidden("error.adminUser.invalidRoles")).toFutureEither
      } yield {
        AdminRequest(adminUser, adminUserSession, request)
      }).toFuture
    }

    private def getSessionKey[A](request: Request[A]): Option[String] = request.cookies.get("sk") match {
      case Some(Cookie("sk", sessionKey, _, _, _, _, _, _)) => Some(sessionKey)
      case _ => None
    }
    
    private def forbidden(msg: String): Result = {
      Logger.trace(s"Admin access forbidden: $msg")
      Forbidden(views.html.error(403, msg))
    }
    
    private def unauthorized(msg: String): Result = {
      Logger.trace(s"Admin access unauthorized: $msg")
      Unauthorized(views.html.error(401, msg))
    }

    private def toLogin(msg: String): Result = {
      Logger.trace(s"Redirecting user to login, reason: $msg")
      Redirect(_root_.controllers.admin.auth.routes.AdminLoginController.login)
    }

    override def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  }

}
