package controllers.auth

import common.actions.LoggedRequest
import common.actions.RequestType._
import common.ui.ViewBuilder
import models.user.UserRepo
import models.auth._
import services.auth.SessionService
import controllers.AbstractController
import common.FutureEither._

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.i18n.Langs
import play.api.i18n.{MessagesApi, Messages}
import play.api.i18n.I18nSupport


class LogoutController(sessionService: SessionService,
                       loginCredentialRepo: LoginCredentialRepo,
                       userRepo: UserRepo,
                       viewBuilder: ViewBuilder,
                       langs: Langs, 
                       messages: MessagesApi,
                       cc: ControllerComponents) extends AbstractController(sessionService, userRepo, viewBuilder, cc, messages) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def logout(userId: Long) = LoggedAction(userId, Web).async { implicit request => 
    Logger.debug("Logout attempt of user #" + request.user.id)

    sessionService.deleteSession(authToken = request.authToken).map { deleted =>
      if (deleted) loginCredentialRepo.logout(authToken = request.authToken).fold({ err =>
        Logger.error(s"Cannot logout user $userId. Error: $err")
        error500(Messages("logout.error"))
      }, _ => {
        deleteLoginCredentials(Redirect(_root_.controllers.www.routes.HomeController.index))
      })
      else {
        Logger.error(s"Cannot delete session on logout of user $userId")
        error500(Messages("logout.error"))
      }
    }
  }
}
