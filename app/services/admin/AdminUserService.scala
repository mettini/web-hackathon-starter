package services.admin

import play.api.mvc.Request
import scala.util.Random

import services.auth.LoginService
import security.PasswordManager
import models.admin._


class AdminUserService(loginService: LoginService,
                       adminUserRepo: AdminUserRepo) {

  def create(adminUser: AdminUserRequest)(implicit request: Request[_]): Either[String, String] = {
    for {
      _ <- adminUserRepo.findByUsername(adminUser.username).map(_ => "username.already.used").toLeft("").right
      user <- doCreate(adminUser).right
      sessionKey <- loginService.adminLogin(adminUser.username, adminUser.password).right
    } yield sessionKey
  }

  def update(adminUserId: Long, name: String, username: String, rolesIds: List[Long]): Either[String, String] = {
    for {
      adminUser <- adminUserRepo.findById(adminUserId).toRight("error.adminUser.notExist").right
      result <- adminUserRepo.update(adminUser, name, username, rolesIds).right
    } yield result
  } 

  private def doCreate(adminUser: AdminUserRequest)(implicit request: Request[_]): Either[String, AdminUser] = {
    val password = PasswordManager.encodePassword(adminUser.password)
    adminUserRepo.save(name = adminUser.name, username = adminUser.username, password = password, rolesIds = adminUser.rolesIds)
  }

}
