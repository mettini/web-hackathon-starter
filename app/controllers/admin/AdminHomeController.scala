package controllers.admin

import common.auth.AuthValidator._
import common.actions.RequestType._
import common.FutureEither._
import common.ui.ViewBuilder
import services.auth._
import models.admin._

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.i18n.Langs
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi


class AdminHomeController(adminUserRepo: AdminUserRepo,
                          adminRoleRepo: AdminRoleRepo,
                          adminUserSessionRepo: AdminUserSessionRepo,
                          loginService: LoginService,
                          viewBuilder: ViewBuilder,
                          langs: Langs, 
                          messages: MessagesApi,
                          cc: ControllerComponents) extends AbstractAdminController(adminUserRepo, adminRoleRepo,
                          adminUserSessionRepo, viewBuilder, cc, messages) with I18nSupport {

  def index = OpsAdminAction { implicit request =>
    buildAdminView(views.html.admin.home())
  }

}
