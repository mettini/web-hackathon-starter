package controllers.user

import services.auth.SessionService
import play.api.libs.json._
import play.api.Logger
import play.api.mvc._

import com.github.nscala_time.time.Imports._

import models.user.{EmailVerificationRepo, UserRepo, EmailVerification, User}
import controllers.AbstractController
import services.user.UserEmailService

import common.ui.ViewBuilder
import common.actions.RequestType._
import controllers.AbstractController
import play.api.i18n.Langs
import play.api.i18n.MessagesApi
import play.api.i18n.I18nSupport


class EmailVerificationController(userEmailService: UserEmailService,
                                  sessionService: SessionService,
                                  userRepo: UserRepo,
                                  viewBuilder: ViewBuilder,
                                  emailVerificationRepo: EmailVerificationRepo,
                                  langs: Langs, 
                                  messages: MessagesApi,
                                  cc: ControllerComponents) extends AbstractController(sessionService, userRepo, viewBuilder, cc, messages) with I18nSupport {

  def verifyEmail(hash: String): Action[AnyContent] = MaybeLoggedAction(Web) { implicit request =>
    resolveEmailVerification(hash).fold(error => {
      buildMaybeLoggedView(400, views.html.user.emailVerification(error, hash, None))
    }, result => {
      val (message, user) = result
      buildMaybeLoggedView(views.html.user.emailVerification(message, hash, Some(user.email)))
    })
  }

  def resendEmailVerification(hash: String): Action[AnyContent] = MaybeLoggedAction(Web) { implicit request =>
    (for {
      emailVerification <- emailVerificationRepo.findByHash(hash).toRight("emailVerification.error.hashNotFound").right
      user <- userRepo.findById(emailVerification.userId).toRight("error.user.notFound").right
      result <- userEmailService.sendVerificationEmail(emailVerification.userId).right
    } yield (result -> user)).fold(err => {
      Logger.error(s"Cannot resend email verification. Error: $err")
      buildMaybeLoggedView(500, views.html.user.emailVerification("emailVerification.error.resend", hash, None))
    }, ok => {
      val (result, user) = ok
      Logger.debug(s"Email verification msg has been sent to user ${user.email}")
      buildMaybeLoggedView(views.html.user.emailVerification("emailVerification.sent", hash, Some(user.email)))
    })
  }

  def resendEmailVerificationForUser(userId: Long): Action[AnyContent] = LoggedAction(userId, Web) { implicit request =>
    Logger.debug(s"EmailVerificationController resendEmailVerification user #$userId")
    userEmailService.sendVerificationEmail(userId).fold(
      error => badRequestJson(error),
      reponse => okJson("emailVerification.sent", JsNull)
    )
  }

  private def resolveEmailVerification(hash: String): Either[String, (String, User)] = {
    for {
      emailVerification <- isValidHash(hash).right
      user <- userRepo.findById(emailVerification.userId).toRight("error.user.notFound").right
      emailVerificationUpdate <- emailVerificationRepo.updateVerificationStatusVerified(emailVerification.id).right
      userVerificationStatus <- userRepo.updateVerificationStatusActive(emailVerification.userId).right
    } yield "emailVerification.verified" -> user
  }

  private def isValidHash(hash: String): Either[String, EmailVerification] = {
    emailVerificationRepo.findByHash(hash) match {
      case Some(emailVerification: EmailVerification) =>
        if (emailVerification.expirationDate.isBefore(DateTime.now)) Left("emailVerification.error.hashExpired")
        else if (emailVerification.verificationStatus == "verified") Left("emailVerification.error.alreadyVerified")
        else Right(emailVerification)
      case _ => Left("emailVerification.error.hashNotFound")
    }
  }

}
