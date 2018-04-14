package controllers.admin

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.twirl.api.Html
import play.api.i18n.{MessagesApi, Messages}

import common.actions.AdminActions._
import common.actions.AdminRequest
import common.ui.ViewBuilder
import models.admin._


abstract class AbstractAdminController(adminUserRepo: AdminUserRepo,
                                       adminRoleRepo: AdminRoleRepo,
                                       adminUserSessionRepo: AdminUserSessionRepo,
                                       viewBuilder: ViewBuilder,
                                       cc: ControllerComponents,
                                       messages: MessagesApi) extends play.api.mvc.AbstractController(cc) {

  private implicit val implicitMessages: Messages = implicitly[Messages]

  protected[controllers] def error500(message: String): Result = {
    InternalServerError(views.html.error(500, message))
  }

  protected[controllers] def error400(message: String): Result = {
    InternalServerError(views.html.error(400, message))
  }

  protected[controllers] def forbidden(message: String): Result = {
    Forbidden(views.html.error(403, message))
  }

  protected[controllers] def buildAdminView(view: (AdminUser, Request[AnyContent], Messages) => Html)
    (implicit request: AdminRequest[AnyContent]): Result = {
    viewBuilder.buildAdminView(view)(request, messages)
  }

  protected[controllers] def buildAdminView(statusCode: Int, view: (AdminUser, Request[AnyContent], Messages) => Html)
    (implicit request: AdminRequest[AnyContent]): Result = {
    viewBuilder.buildAdminView(statusCode, view)(request, messages)
  }

  val AdminAction = Action andThen Admin(List(AdminRole.Admin), adminUserSessionRepo, adminRoleRepo, adminUserRepo, messages)
  val OpsAdminAction = Action andThen Admin(List(AdminRole.Admin, AdminRole.Ops), adminUserSessionRepo, adminRoleRepo, adminUserRepo, messages)

}
