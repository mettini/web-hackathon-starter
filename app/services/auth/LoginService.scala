package services.auth

import common.auth.AuthValidator._
import security.PasswordManager
import play.api.Configuration
import play.api.mvc.Request
import com.github.nscala_time.time.Imports._
import play.api._
import models.user.{User, UserRepo}
import models.auth.{LoginCredential, LoginCredentialRepo}
import models.auth.LoginCredentialStatus

import models.admin.{AdminUserRepo, AdminUser, AdminUserSessionRepo, AdminUserSession}

class LoginService(config: Configuration,
                   userRepo: UserRepo,
                   adminUserRepo: AdminUserRepo,
                   adminUserSessionRepo: AdminUserSessionRepo,
                   loginCredentialRepo: LoginCredentialRepo) {

  lazy val authTokenExpirationDuration = config.get[Int]("app.authToken.expiration.duration")

  def login(email: String, password: String)(implicit request: Request[_]): Either[String, LoginCredential] = {
    Logger.trace(s"LoginService email: $email")
    userRepo.findByEmail(email) match {
      case Some(user) => doLogin(user, password)
      case None => Left("error.email.notFound")
    }
  }

  def adminLogin(username: String, password: String)(implicit request: Request[_]): Either[String, String] = {
    Logger.trace(s"LoginService admin username: $username")
    adminUserRepo.findByUsername(username) match {
      case Some(adminUser) => doAdminLogin(adminUser, password)
      case None => Left("error.username.notFound")
    }
  }

  def updatePassword(userId: Long, newPassword: String): Option[String] = {
    userRepo.updatePassword(userId, PasswordManager.encodePassword(newPassword)).fold(err => {
      Logger.error(s"Cannot update password of user $userId. Error: $err")
      Option("error.user.cannotModifyPassword")
    }, ok => {
      Logger.debug(s"Successful password update of user ${userId}")
      None
    })
  }

  // Private methods

  private def generateAuthToken = java.util.UUID.randomUUID.toString

  private def generateAdminSessionKey = java.util.UUID.randomUUID.toString

  private def doLogin(user: User,
                      password: String)(implicit request: Request[_]): Either[String, LoginCredential] = {
    for {
      _ <- PasswordManager.checkPassword(password, user.password).right
      loginCredential <- executeLogin(user.id).right
    } yield loginCredential
  }

  private def executeLogin(userId: Long)(implicit request: Request[_]): Either[String, LoginCredential] = {
    val authToken = generateAuthToken
    val expirationDate = DateTime.now(DateTimeZone.UTC) + authTokenExpirationDuration.days
    for {
      loginCredential <- loginCredentialRepo.save(userId, authToken, LoginCredentialStatus.ENABLED, expirationDate)
        .toRight(s"couldnt save the login credential for user $userId").right
    } yield loginCredential
  }

  private def doAdminLogin(adminUser: AdminUser,
                           password: String)(implicit request: Request[_]): Either[String, String] = {
    for {
      _ <- PasswordManager.checkPassword(password, adminUser.password).right
      sessionKey <- executeAdminLogin(adminUser).right
    } yield sessionKey
  }

  private def executeAdminLogin(adminUser: AdminUser)(implicit request: Request[_]): Either[String, String] = {
    val sessionKey = generateAdminSessionKey
    for {
      _ <- adminUserSessionRepo.save(adminUser, sessionKey).right
    } yield sessionKey
  }

}
