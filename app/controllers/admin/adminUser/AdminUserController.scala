package controllers.admin.adminUser

import common.auth.AuthValidator._
import common.actions.RequestType._
import common.actions.AdminRequest
import common.FutureEither._
import common.ui.ViewBuilder
import security.PasswordManager
import services.admin.AdminUserService
import services.auth._
import models.admin._

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.data._
import play.api.i18n.Langs
import play.api.i18n.MessagesApi
import play.api.i18n.I18nSupport
import anorm.NamedParameter
import play.api.i18n.Messages

import _root_.controllers.admin.AbstractAdminController


class AdminUserController(adminUserRepo: AdminUserRepo,
                          adminRoleRepo: AdminRoleRepo,
                          adminUserSessionRepo: AdminUserSessionRepo,
                          adminUserService: AdminUserService,
                          viewBuilder: ViewBuilder,
                          langs: Langs, 
                          messagesApi: MessagesApi,
                          cc: ControllerComponents) extends AbstractAdminController(adminUserRepo, adminRoleRepo,
                          adminUserSessionRepo, viewBuilder, cc, messagesApi) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global
  import AdminUserForm._


  def list(msg: Option[String], offset: Long, limit: Long) = AdminAction { implicit request =>
    val result = adminUserRepo.list(offset, limit)
    val pageResolver = { (offset: Long, limit: Long) =>
      _root_.controllers.admin.adminUser.routes.AdminUserController.list(None, offset, limit)
    }
    buildAdminView(views.html.admin.adminUser.list(result.count, result.list, offset, limit, msg, pageResolver))
  }

  def create = AdminAction { implicit request =>
    buildAdminView(views.html.admin.adminUser.create(form, adminRoleRepo.listAll.list))
  }  

  def doCreate = AdminAction { implicit request =>
    val errorFunction = { formWithErrors: Form[Data] =>
      buildAdminView(400, views.html.admin.adminUser.create(formWithErrors, adminRoleRepo.listAll.list))
    }

    val successFunction = { data: Data =>
      val adminUser = AdminUserRequest(data.name, data.username, data.password, data.rolesIds)
      adminUserService.create(adminUser).fold(err => {
        Logger.error(s"Cannot create admin user. Error: $err")
        buildAdminView(400, views.html.admin.adminUser.create(form.fill(data).withGlobalError("error.admin.auth.cannotCreate"), adminRoleRepo.listAll.list))
      }, loginCredentials => {
        Logger.debug(s"Successful create admin user ${data.username}")
        Redirect(_root_.controllers.admin.adminUser.routes.AdminUserController.list(msg = Some("adminUser.created.successfully")))
      })
    }

    form.bindFromRequest.fold(errorFunction, successFunction)
  }

  def edit(adminUserId: Long) = AdminAction { implicit request =>
    val messages: Messages = messagesApi.preferred(request)
    (for {
      adminUser <- adminUserRepo.findById(adminUserId).toRight(error400("error.adminUser.notExist")).right
      _ <- Either.cond(canEditUserData(adminUser, adminUser), "ok", forbidden(messages("error.superAdmin.edit"))).right
    } yield adminUser).fold(err => err, adminUser => {
      val roles = adminRoleRepo.listByAdminUserId(adminUserId).map(_.id)
      val editData = EditData(adminUser.username, adminUser.name, roles)
      buildAdminView(views.html.admin.adminUser.edit(adminUser, editForm.fill(editData), adminRoleRepo.listAll.list))
    })
  }

  def doEdit(adminUserId: Long) = AdminAction { implicit request =>
    val messages: Messages = messagesApi.preferred(request)
    (for {
      adminUser <- adminUserRepo.findById(adminUserId).toRight(error400("error.adminUser.notExist")).right
      _ <- Either.cond(canEditUserData(adminUser, adminUser), "ok", forbidden(messages("error.superAdmin.edit"))).right
    } yield adminUser).fold(err => err, adminUser => {
      val errorFunction = { formWithErrors: Form[EditData] =>
        buildAdminView(400, views.html.admin.adminUser.edit(adminUser, formWithErrors, adminRoleRepo.listAll.list))
      }

      val successFunction = { data: EditData =>
        adminUserService.update(adminUserId, data.name, data.username, data.rolesIds).fold(err => {
          Logger.error(s"Cannot edit admin user $adminUserId. Error: $err")
          buildAdminView(400, views.html.admin.adminUser.edit(adminUser, editForm.fill(data).withGlobalError("error.admin.auth.cannotEdit"), 
            adminRoleRepo.listAll.list))
        }, loginCredentials => {
          Logger.debug(s"Successful edit admin user ${data.username}")
          Redirect(_root_.controllers.admin.adminUser.routes.AdminUserController.list(msg = Some("adminUser.edit.successfully")))
        })
      }

      editForm.bindFromRequest.fold(errorFunction, successFunction)
    })
  }

  def editPassword(adminUserId: Long) = AdminAction { implicit request =>
    val messages: Messages = messagesApi.preferred(request)
    (for {
      adminUser <- adminUserRepo.findById(adminUserId).toRight(error400("error.adminUser.notExist")).right
      _ <- Either.cond(canEditUserData(adminUser, adminUser), "ok", forbidden(messages("error.superAdmin.edit"))).right
    } yield adminUser).fold(err => err, adminUser => {
      buildAdminView(views.html.admin.adminUser.editPassword(adminUser, passwordForm))
    })
  }

  def doEditPassword(adminUserId: Long) = AdminAction { implicit request =>
    val messages: Messages = messagesApi.preferred(request)
    (for {
      adminUser <- adminUserRepo.findById(adminUserId).toRight(error400("error.adminUser.notExist")).right
      _ <- Either.cond(canEditUserData(adminUser, adminUser), "ok", forbidden(messages("error.superAdmin.edit"))).right
    } yield adminUser).fold(err => err, adminUser => {
      val errorFunction = { formWithErrors: Form[PasswordData] =>
        buildAdminView(400, views.html.admin.adminUser.editPassword(adminUser, formWithErrors))
      }

      val successFunction = { data: PasswordData =>
        adminUserRepo.updatePassword(adminUser.id, PasswordManager.encodePassword(data.newPassword)).fold(err => {
          Logger.error(s"Cannot update password. Error: $err")
          buildAdminView(500, views.html.admin.adminUser.editPassword(adminUser, passwordForm.fill(data)
            .withGlobalError("error.adminUser.cannotModifyPassword")))
        }, ok => {
          Logger.debug(s"Successful password update of admin user ${adminUser.username}")
          Redirect(_root_.controllers.admin.adminUser.routes.AdminUserController.list(msg = Some("adminUser.editPassword.successfully")))
        })
      }

      passwordForm.bindFromRequest.fold(errorFunction, successFunction)
    })
  }

  def delete(adminUserId: Long) = AdminAction { implicit request =>
    adminUserRepo.findById(adminUserId).map { adminUser =>
      val deletedUsername = s"deleted-${System.currentTimeMillis}-${adminUser.username}"
      adminUserRepo.update(adminUserId, Seq(NamedParameter("is_deleted", true), NamedParameter("username", deletedUsername))).fold(err => {
        error500("error.adminUser.cannotDelete")
      }, ok => {
        Redirect(_root_.controllers.admin.adminUser.routes.AdminUserController.list(msg = Some("adminUser.delete.successfully")))
      })
    }.getOrElse {
      error400("error.adminUser.notExist")
    }
  }

  private def canEditUserData(adminUser: AdminUser, editor: AdminUser): Boolean = {
    editor.id == AdminUser.SuperUserId || (editor.id != AdminUser.SuperUserId && adminUser.id != AdminUser.SuperUserId)
  }

}
