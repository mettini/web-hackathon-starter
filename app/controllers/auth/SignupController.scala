package controllers.auth

import models.auth.LoginCredentialRepo
import models.auth.requests.SignupRequest
import models.user.UserRepo
import services.auth._
import common.actions.RequestType._
import common.ui.ViewBuilder
import controllers.AbstractController

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.data._
import play.api.i18n.Langs
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi

class SignupController(signupService: SignupService,
                       sessionService: SessionService,
                       userRepo: UserRepo,
                       viewBuilder: ViewBuilder,
                       loginCredentialRepo: LoginCredentialRepo,
                       langs: Langs, 
                       messages: MessagesApi,
                       cc: ControllerComponents) extends AbstractController(sessionService, userRepo, viewBuilder, cc, messages) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global
  import SignupForm._

  def signup = MaybeLoggedAction(Web) { implicit request =>
    Ok(views.html.auth.signup(form, request))
  }  

  def doSignup = MaybeLoggedAction(Web) { implicit request =>
    val errorFunction = { formWithErrors: Form[Data] =>
      BadRequest(views.html.auth.signup(formWithErrors, request))
    }

    val successFunction = { data: Data =>
      val signupRequest = SignupRequest(data.email, data.password, data.firstName, data.lastName)
      signupService.signup(signupRequest).fold(err => {
        Logger.error(s"Cannot signup user. SignupRequest: ${signupRequest} -- error: $err")
        BadRequest(views.html.auth.signup(form.fill(data).withGlobalError("error.auth.cannotSingup"), request))
      }, loginCredentials => {
        Logger.debug(s"Successful signup of user ${data.email}")
        persistLoginCredentials(loginCredentials, Redirect(_root_.controllers.www.routes.HomeController.index))
      })
    }

    form.bindFromRequest.fold(errorFunction, successFunction)
  }

}
