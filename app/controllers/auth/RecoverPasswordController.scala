package controllers.auth

import common.actions.LoggedRequest
import common.actions.RequestType._
import common.ui.ViewBuilder
import models.user.{UserRepo, EmailResetPasswordRepo, EmailResetPassword}
import models.auth._
import services.auth.{SessionService, LoginService}
import controllers.AbstractController
import common.FutureEither._

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.data._
import play.api.i18n.Langs
import play.api.i18n.{MessagesApi, Messages}
import play.api.i18n.I18nSupport
import services.user.UserEmailService

import com.github.nscala_time.time.Imports._


class RecoverPasswordController(sessionService: SessionService,
                                loginService: LoginService,
                                userEmailService: UserEmailService,
                                emailResetPasswordRepo: EmailResetPasswordRepo,
                                userRepo: UserRepo,
                                viewBuilder: ViewBuilder,
                                langs: Langs, 
                                messages: MessagesApi,
                                cc: ControllerComponents) extends AbstractController(sessionService, 
                                userRepo, viewBuilder, cc, messages) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global
  import RecoverPasswordForm._
  
  def recoverRequest = MaybeLoggedAction(Web) { implicit request =>
    Ok(views.html.auth.recover(form, request, None))
  }

  def doRecoverRequest = MaybeLoggedAction(Web) { implicit request =>
    val errorFunction = { formWithErrors: Form[Data] =>
      BadRequest(views.html.auth.recover(formWithErrors, request, None))
    }

    val successFunction = { data: Data =>
      sendResetPasswordMail(data.email).fold(err => {
        Logger.error(s"Cannot recover password for email: ${data.email} -- error: $err")
        BadRequest(views.html.auth.recover(form.fill(data).withGlobalError("error.auth.cannotRecover"), 
          request, None))
      }, result => {
        Logger.debug(s"Successful password recovery for user ${data.email}")
        Ok(views.html.auth.recover(form, request, Some("recover.success")))
      })
    }

    form.bindFromRequest.fold(errorFunction, successFunction)
  }

  def recover(hash: String) = MaybeLoggedAction(Web) { implicit request =>
    resolveEmailResetPassword(hash).fold(error => {
      BadRequest(views.html.auth.recover(form.withGlobalError(error), request, None))
    }, emailResetPassword => {
      Logger.debug(s"User ${emailResetPassword.userId} trying to reset password")
      Ok(views.html.auth.resetPassword(hash, resetForm, request))
    })
  }

  def doRecover(hash: String) = MaybeLoggedAction(Web) { implicit request =>
    resolveEmailResetPassword(hash).fold(error => {
      BadRequest(views.html.auth.recover(form.withGlobalError(error), request, None))
    }, emailResetPassword => {
      val errorFunction = { formWithErrors: Form[ResetData] =>
        BadRequest(views.html.auth.resetPassword(hash, formWithErrors, request))
      }

      val successFunction = { data: ResetData =>
        loginService.updatePassword(emailResetPassword.userId, data.newPassword).map { err =>
          InternalServerError(views.html.auth.resetPassword(hash, resetForm.fill(data)
            .withGlobalError("error.user.cannotModifyPassword"), request))
        }.getOrElse {
          Ok(views.html.auth.recover(form, request, Some("resetPassword.success")))
        }
      }

      resetForm.bindFromRequest.fold(errorFunction, successFunction)
    })
  }

  private def sendResetPasswordMail(email: String): Either[String, String] = {
    userRepo.findByEmail(email) match {
      case Some(user) => userEmailService.sendResetPasswordMail(user.id)
      case _ => Left("error.email.notFound")
    }
  }

  private def resolveEmailResetPassword(hash: String): Either[String, EmailResetPassword] = {
    emailResetPasswordRepo.findByVerificationCode(hash) match {
      case Some(emailResetPassword: EmailResetPassword) =>
        if (emailResetPassword.expirationDate.isBefore(DateTime.now)) Left("emailResetPassword.error.hashExpired")
        else if (emailResetPassword.used) Left("emailResetPassword.error.alreadyVerified")
        else Right(emailResetPassword)
      case _ => Left("emailResetPassword.error.hashNotFound")
    }
  }

}
