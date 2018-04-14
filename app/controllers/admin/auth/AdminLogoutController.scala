package controllers.admin.auth

import common.actions.LoggedRequest
import common.actions.RequestType._
import common.ui.ViewBuilder
import models.auth._
import models.admin._
import services.auth.SessionService
import controllers.AbstractController
import common.FutureEither._

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.i18n.Langs
import play.api.i18n.{MessagesApi, Messages}
import play.api.i18n.I18nSupport

import _root_.controllers.admin.AbstractAdminController


class AdminLogoutController(adminUserRepo: AdminUserRepo,
                            adminRoleRepo: AdminRoleRepo,
                            adminUserSessionRepo: AdminUserSessionRepo,
                            viewBuilder: ViewBuilder,
                            langs: Langs, 
                            messages: MessagesApi,
                            cc: ControllerComponents) extends AbstractAdminController(adminUserRepo, adminRoleRepo,
                            adminUserSessionRepo, viewBuilder, cc, messages) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def logout = OpsAdminAction { implicit request => 
    Logger.debug("Logout attempt of admin user #" + request.adminUser.id)

    adminUserSessionRepo.finish(request.adminUserSession).fold({ err =>
      Logger.error(s"Cannot logout admin user ${request.adminUser.id}. Error: $err")
      error500(Messages("admin.logout.error"))
    }, _ => {
      deleteSessionKey(Redirect(_root_.controllers.admin.routes.AdminHomeController.index))
    })
  }

  private def deleteSessionKey(result: Result)(implicit request: Request[AnyContent]): Result = {
    result.withCookies(Cookie(name = "sk", value = "", path = "/"))
  }

}
