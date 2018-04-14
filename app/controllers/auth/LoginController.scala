package controllers.auth

import common.auth.AuthValidator._
import common.actions.RequestType._
import common.FutureEither._
import common.ui.ViewBuilder
import models.auth.{LoginCredential, LoginCredentialRepo}
import models.user.UserRepo
import services.auth._
import controllers.AbstractController

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.data._
import play.api.i18n.Langs
import play.api.i18n.MessagesApi
import play.api.i18n.I18nSupport


class LoginController(userRepo: UserRepo,
                      sessionService: SessionService,
                      loginService: LoginService,
                      viewBuilder: ViewBuilder,
                      langs: Langs, 
                      messages: MessagesApi,
                      cc: ControllerComponents) extends AbstractController(sessionService, userRepo, viewBuilder, cc, messages) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global
  import LoginForm._

  def login = MaybeLoggedAction(Web) { implicit request =>
    Ok(views.html.auth.login(form, request))
  } 

  def doLogin = MaybeLoggedAction(Web) { implicit request =>
    val errorFunction = { formWithErrors: Form[Data] =>
      BadRequest(views.html.auth.login(formWithErrors, request))
    }

    val successFunction = { data: Data =>
      loginService.login(data.email, data.password).fold(err => {
        Logger.error(s"Cannot login user. email: ${data.email} -- error: $err")
        BadRequest(views.html.auth.login(form.fill(data).withGlobalError("error.auth.cannotLogin"), request))
      }, loginCredentials => {
        Logger.debug(s"Successful login of user ${data.email}")
        persistLoginCredentials(loginCredentials, Redirect(_root_.controllers.www.routes.HomeController.index))
      })
    }

    form.bindFromRequest.fold(errorFunction, successFunction)
  }
}
