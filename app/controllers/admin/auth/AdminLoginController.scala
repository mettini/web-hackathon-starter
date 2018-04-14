package controllers.admin.auth

import common.auth.AuthValidator._
import common.actions.RequestType._
import common.FutureEither._
import common.ui.ViewBuilder
import services.auth._
import models.admin._

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.data._
import play.api.i18n.Langs
import play.api.i18n.MessagesApi
import play.api.i18n.I18nSupport

import _root_.controllers.admin.AbstractAdminController


class AdminLoginController(adminUserRepo: AdminUserRepo,
                           adminRoleRepo: AdminRoleRepo,
                           adminUserSessionRepo: AdminUserSessionRepo,
                           loginService: LoginService,
                           viewBuilder: ViewBuilder,
                           langs: Langs, 
                           messages: MessagesApi,
                           cc: ControllerComponents) extends AbstractAdminController(adminUserRepo, adminRoleRepo,
                           adminUserSessionRepo, viewBuilder, cc, messages) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global
  import AdminLoginForm._

  def login = Action { implicit request =>
    Ok(views.html.admin.login(form, request))
  } 

  def doLogin = Action { implicit request =>
    val errorFunction = { formWithErrors: Form[Data] =>
      BadRequest(views.html.admin.login(formWithErrors, request))
    }

    val successFunction = { data: Data =>
      loginService.adminLogin(data.username, data.password).fold(err => {
        Logger.error(s"Cannot login admin user. username: ${data.username} -- error: $err")
        BadRequest(views.html.admin.login(form.fill(data).withGlobalError("error.admin.cannotLogin"), request))
      }, sessionKey => {
        Logger.debug(s"Successful login of admin user ${data.username}")
        persistSessionKey(sessionKey, Redirect(_root_.controllers.admin.routes.AdminHomeController.index))
      })
    }

    form.bindFromRequest.fold(errorFunction, successFunction)
  }

  private def persistSessionKey(sessionKey: String, result: Result)(implicit request: Request[AnyContent]): Result = {
    result.withCookies(Cookie(name = "sk", value = sessionKey, path = "/"))
  }

}
