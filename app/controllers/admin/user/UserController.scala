package controllers.admin.user

import common.auth.AuthValidator._
import common.actions.RequestType._
import common.actions.AdminRequest
import common.FutureEither._
import common.ui.ViewBuilder
import security.PasswordManager
import services.admin.AdminUserService
import services.auth._
import models.admin._
import models.user.{UserRepo, UserProfileRepo, UserConfigRepo}

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


class UserController(adminUserRepo: AdminUserRepo,
                     adminRoleRepo: AdminRoleRepo,
                     userRepo: UserRepo,
                     userProfileRepo: UserProfileRepo,
                     userConfigRepo: UserConfigRepo,
                     adminUserSessionRepo: AdminUserSessionRepo,
                     adminUserService: AdminUserService,
                     viewBuilder: ViewBuilder,
                     langs: Langs, 
                     messagesApi: MessagesApi,
                     cc: ControllerComponents) extends AbstractAdminController(adminUserRepo, adminRoleRepo,
                     adminUserSessionRepo, viewBuilder, cc, messagesApi) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global


  def list(msg: Option[String], offset: Long, limit: Long) = OpsAdminAction { implicit request =>
    val result = userRepo.list(offset, limit)
    val pageResolver = { (offset: Long, limit: Long) =>
      _root_.controllers.admin.user.routes.UserController.list(None, offset, limit)
    }
    buildAdminView(views.html.admin.user.list(result.count, result.list, offset, limit, msg, pageResolver))
  }

  def detail(userId: Long) = OpsAdminAction { implicit request =>
    (for {
      user <- userRepo.findById(userId).toRight(error400("error.user.notFound")).right
      userProfile <- userProfileRepo.findByUserId(userId).toRight(error400("error.user.notFound")).right
    } yield (user -> userProfile)).fold(err => err, result => {
      val (user, userProfile) = result
      buildAdminView(views.html.admin.user.detail(user, userProfile))
    })
  }

  def delete(userId: Long) = OpsAdminAction { implicit request =>
    userRepo.findById(userId).map { user =>
      val deletedEmail = s"deleted-${System.currentTimeMillis}-${user.email}"
      userRepo.update(userId, Seq(NamedParameter("is_deleted", true), NamedParameter("email", deletedEmail))).fold(err => {
        error500("error.user.cannotDelete")
      }, ok => {
        Redirect(_root_.controllers.admin.user.routes.UserController.list(msg = Some("user.delete.successfully")))
      })
    }.getOrElse {
      error400("error.user.notFound")
    }
  }

  def moderate(userId: Long) = OpsAdminAction { implicit request =>
    userRepo.findById(userId).map { user =>
      userConfigRepo.setModeratedConfig(userId, true).fold(err => {
        error500("error.user.cannotModerate")
      }, ok => {
        Logger.debug(s"User $userId moderated")
        Redirect(_root_.controllers.admin.user.routes.UserController.detail(userId))
      })
    }.getOrElse {
      error400("error.user.notFound")
    }
  }

  def removeModeration(userId: Long) = OpsAdminAction { implicit request =>
    userRepo.findById(userId).map { user =>
      userConfigRepo.setModeratedConfig(userId, false).fold(err => {
        error500("error.user.cannotRemoveModeration")
      }, ok => {
        Logger.debug(s"User $userId moderation removed")
        Redirect(_root_.controllers.admin.user.routes.UserController.detail(userId))
      })
    }.getOrElse {
      error400("error.user.notFound")
    }
  }

}
