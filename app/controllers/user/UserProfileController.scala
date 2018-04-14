package controllers.user

import services.auth.SessionService
import controllers.AbstractController

import anorm.NamedParameter
import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.data._
import play.api.i18n.Langs
import play.api.i18n.{Messages, MessagesApi}
import play.api.i18n.I18nSupport

import models.user.User.UserVerificationStatus
import models.user.UserProfileRepo._
import models.user.requests.UserProfileRequest
import models.user.{UserProfileRepo, UserRepo, User, UserProfile}
import common.actions.RequestType._
import common.ui.ViewBuilder
import services.user.UserEmailService

import scala.util.{Left, Right}

class UserProfileController(sessionService: SessionService,
                            userRepo: UserRepo,
                            viewBuilder: ViewBuilder,
                            userProfileRepo: UserProfileRepo,
                            userEmailService: UserEmailService,
                            langs: Langs, 
                            messages: MessagesApi,
                            cc: ControllerComponents) extends AbstractController(sessionService, userRepo, viewBuilder, cc, 
                            messages) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global
  import UserProfileForm._

  def profile(userId: Long) = LoggedAction(userId, Web) { implicit request =>
    userProfileRepo.findByUserId(userId).map { userProfile =>
      buildLoggedView(views.html.user.profile(userProfile, None))
    }.getOrElse {
      BadRequest(views.html.error(400, "error.user.notFound"))
    }
  }

  def update(userId: Long) = LoggedAction(userId, Web) { implicit request =>
    userProfileRepo.findByUserId(userId).map { userProfile =>
      buildLoggedView(views.html.user.editProfile(filledForm(userProfile, request.user)))
    }.getOrElse {
      BadRequest(views.html.error(400, "error.user.notFound"))
    }
  }

  def doUpdate(userId: Long) = LoggedAction(userId, Web) { implicit request =>
    val errorFunction = { formWithErrors: Form[Data] =>
      buildLoggedView(views.html.user.editProfile(formWithErrors))
    }

    val successFunction = { data: Data =>
      val userProfileRequest = UserProfileRequest(data.firstName, data.lastName)
      (for {
        profileUpdate <- userProfileRepo.updateProfile(userId, userProfileRequest).right
        result <- checkEmailUpdate(data.email, userId).right
        userProfile <- userProfileRepo.findByUserId(userId).toRight("error.user.notFound").right
      } yield (result -> userProfile)).fold(err => {
        Logger.error(s"Cannot update profile. UserProfileRequest: ${userProfileRequest} -- error: $err")
        buildLoggedView(400, views.html.user.editProfile(form.fill(data)
          .withGlobalError("error.user.cannotEditProfile")))
      }, ok => {
        val (result, userProfile) = ok
        Logger.debug(s"Successful update profile of user ${request.user.id}")
        buildLoggedView(views.html.user.profile(userProfile, Some(result)))
      })
    }

    form.bindFromRequest.fold(errorFunction, successFunction)
  }

  private def checkEmailUpdate[A](email: String, userId: Long)(implicit request: Request[A]): Either[String, String] = {
    userRepo.findById(userId) match {
      case Some(user) if (email != user.email) => {
        (for {
          update <- userRepo.update(userId, updateEmailParams(email)).right
          _ <- userEmailService.sendVerificationEmail(userId).right
        } yield update).fold(err => {
          Logger.error(s"Cannot update user $userId email for $email. Error $err")
          Left("error.user.cannotEditProfile")
        }, update => {
          if (update == 1) Right(Messages("editUserProfile.successButEmailValidation", email))
          else Left("error.user.cannotEditProfile")
        })
      }
      case Some(user) => Right(Messages("editUserProfile.success"))
      case None => Left("error.user.notFound")
    }
  }
  
  private def updateEmailParams(email: String): Seq[NamedParameter] = {
    Seq(NamedParameter("email", email), NamedParameter("verification_status", UserVerificationStatus.Pending.toString))
  }

  private def filledForm(userProfile: UserProfile, user: User): Form[UserProfileForm.Data] = {
    form.fill(UserProfileForm.Data(userProfile.firstname, userProfile.lastname.getOrElse(""), user.email))
  }

}
