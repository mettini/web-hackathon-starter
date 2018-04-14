package models.admin

import java.sql.Connection

import anorm.JodaParameterMetaData._
import anorm.SqlParser._
import anorm._
import com.github.nscala_time.time.Imports._
import play.api.Logger
import play.api.db.Database
import play.api.libs.json._

import models.BaseModel

case class AdminUserRequest(
  name: String,
  username: String,
  password: String,
  rolesIds: List[Long])

case class AdminUser(
  id: Long,
  name: String,
  username: String,
  password: String,
  roles: List[AdminRole],
  isDeleted: Boolean,
  creationDate: DateTime,
  lastModificationDate: DateTime) {

  val rolesNames = roles.map(_.name)

  def hasAnyRole(checkRoles: List[String]) = !rolesNames.intersect(checkRoles).isEmpty

}

object AdminUser {
  val SuperUserId = 1L
}

class AdminUserRepo(val db: Database, adminRoleRepo: AdminRoleRepo, adminUserRoleRepo: AdminUserRoleRepo) extends BaseModel[AdminUser] {

  override def tableName: String = "admin_users"

  def parser: RowParser[AdminUser] = {
    long("id") ~
    str("name") ~
    str("username") ~
    str("password") ~
    bool("is_deleted") ~
    get[DateTime]("creation_date") ~
    get[DateTime]("last_modification_date") map {
      case id~name~username~password~isDeleted~creationDate~lastModificationDate => 
        AdminUser(id, name, username, password, adminRoleRepo.listByAdminUserId(id),isDeleted, creationDate, lastModificationDate)
    }
  }

  def save(name: String,
           username: String,
           password: String,
           rolesIds: List[Long]): Either[String, AdminUser] = {

    Logger.trace(s"""AdminUser save name: $name username: $username""")
    db.withTransaction { implicit connection =>
      for {
        adminUserId <- insertTransaction(name, username, password).right
        _ <- adminUserRoleRepo.addRolesToUserTransaction(adminUserId, rolesIds).right
        adminUser <- findByIdTransaction(adminUserId).toRight(s"problem retriving adminUserId $adminUserId").right
      } yield adminUser
    }
  }

  def findByUsername(username: String): Option[AdminUser] = {
    db.withConnection { implicit connection =>
      SQL(s"""
        SELECT *
        FROM $tableName
        WHERE username = {username} AND is_deleted = false
        """).on('username -> username).as(parser.singleOpt)
    }
  }

  def update(adminUser: AdminUser, name: String, username: String, rolesIds: List[Long]): Either[String, String] = {
    db.withTransaction { implicit connection =>
      for {
        _ <- updateTransaction(adminUser.id, Seq(NamedParameter("name", name), NamedParameter("username", username))).right
        _ <- adminUserRoleRepo.revokeRolesOfUserTransaction(adminUser.id).right
        result <- adminUserRoleRepo.addRolesToUserTransaction(adminUser.id, rolesIds).right
      } yield result
    }
  }

  def updatePassword(userId: Long, newPass: String): Either[String, Long] =
    for {
      _ <- update(userId, Seq(NamedParameter("password", newPass))).right
    } yield userId

  private def insertTransaction(name: String,
                                username: String,
                                password: String)(implicit connection: Connection): Either[String, Long] = {
    insert(
      "name" -> name,
      "username" -> username,
      "password" -> password,
      "creation_date" -> DateTime.now(DateTimeZone.UTC),
      "last_modification_date" -> DateTime.now(DateTimeZone.UTC)
    ).toRight("there was an error while creating the admin user")
  }

}
