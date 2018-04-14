package models.admin

import anorm.JodaParameterMetaData._
import anorm.SqlParser._
import anorm._
import com.github.nscala_time.time.Imports._
import play.api.Logger
import play.api.db.Database
import play.api.libs.json._

import java.sql.Connection

import models.BaseModel


object AdminRole {
  val Admin = "ADMIN"
  val Ops = "OPS"
}

case class AdminRole(
  id: Long,
  name: String
)

class AdminRoleRepo(val db: Database, adminUserRoleRepo: AdminUserRoleRepo) extends BaseModel[AdminRole] {

  lazy val adminUserRoles = adminUserRoleRepo.tableName

  override def tableName: String = "admin_roles"

  def parser: RowParser[AdminRole] = {
    long("id") ~
    str("name") map {
      case id ~ name => AdminRole(id, name)
    }
  }

  def listByUserIdQuery(adminUserId: Long)(implicit connection: Connection): List[AdminRole] = {
    SQL(s""" SELECT distinct ar.* FROM $tableName ar
             INNER JOIN $adminUserRoles aur ON ar.id = aur.admin_role_id
             WHERE aur.admin_user_id = {adminUserId} AND ar.is_deleted = false AND aur.is_deleted = false """).on('adminUserId -> adminUserId).as(parser.*)
  }

  def listByAdminUserId(adminUserId: Long): List[AdminRole] = {
    Logger.trace(s"Find user roles by admin user id: $adminUserId")
    db.withConnection { implicit connection =>
      listByUserIdQuery(adminUserId)
    }
  }

}
