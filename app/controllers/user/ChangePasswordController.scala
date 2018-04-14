package controllers.user

import services.auth.{SessionService, LoginService}
import controllers.AbstractController
import models.user._
import security.PasswordManager
import services.user.UserEmailService
import common.auth.AuthValidator._
import common.actions.RequestType._
import common.actions.LoggedRequest
import common.FutureEither
import common.FutureEither._
import common.ui.ViewBuilder

import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import play.api.data._
import play.api.Configuration
import com.github.nscala_time.time.Imports._
import play.api.i18n.Langs
import play.api.i18n.MessagesApi
import play.api.i18n.I18nSupport


class ChangePasswordController(userEmailService: UserEmailService,
                               config: Configuration,
                               sessionService: SessionService,
                               loginService: LoginService,
                               userRepo: UserRepo,
                               userProfileRepo: UserProfileRepo,
                               viewBuilder: ViewBuilder,
                               emailResetPasswordRepo: EmailResetPasswordRepo,
                               langs: Langs, 
                               messages: MessagesApi,
                               cc: ControllerComponents) extends AbstractController(sessionService, userRepo, viewBuilder, cc, 
                               messages) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global
  import ChangePasswordForm._

  def modify(userId: Long) = LoggedAction(userId, Web) { implicit request =>
    buildLoggedView(views.html.user.changePassword(form))
  }

  def doModify(userId: Long) = LoggedAction(userId, Web) { implicit request =>
    val errorFunction = { formWithErrors: Form[Data] =>
      buildLoggedView(400, views.html.user.changePassword(formWithErrors))
    }

    val successFunction = { data: Data =>
      PasswordManager.checkPassword(data.currentPassword, request.user.password).fold(err => {
        Logger.error(s"Invalid current password (at update password). Error $err")
        buildLoggedView(400, views.html.user.changePassword(form.fill(data).withGlobalError("changePassword.invalidCurrentPassword")))
      }, okCurrentPassword => {
        execPasswordChange(data)
      })
    }

    form.bindFromRequest.fold(errorFunction, successFunction)
  }

  private def execPasswordChange(data: ChangePasswordForm.Data)(implicit request: LoggedRequest[AnyContent]) = {
    loginService.updatePassword(request.user.id, data.newPassword).map { err =>
      buildLoggedView(500, views.html.user.changePassword(form.fill(data).withGlobalError("error.user.cannotModifyPassword")))
    }.getOrElse {
      userProfileRepo.findByUserId(request.user.id).map { userProfile =>
        buildLoggedView(views.html.user.profile(userProfile, Some("changePassword.success")))
      }.getOrElse {
        BadRequest(views.html.error(400, "error.user.notFound"))
      }
    }
  }

}
