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


case class AdminUserRole(
  id: Long,
  adminUserId: Long,
  adminRoleId: Long
)

class AdminUserRoleRepo(val db: Database) extends BaseModel[AdminUserRole] {

  override def tableName: String = "admin_users_roles"

  def parser: RowParser[AdminUserRole] = {
    long("id") ~
    long("admin_user_id") ~
    long("admin_role_id") map {
      case id ~ adminUserId ~ adminRoleId => AdminUserRole(id, adminUserId, adminRoleId)
    }
  }

  def addRolesToUserTransaction(adminUserId: Long, rolesIds: List[Long])(implicit connection: Connection): Either[String, String] = {
    val inserted = rolesIds.map { roleId =>
      insert(
        "admin_user_id" -> adminUserId,
        "admin_role_id" -> roleId,
        "creation_date" -> DateTime.now(DateTimeZone.UTC),
        "last_modification_date" -> DateTime.now(DateTimeZone.UTC)
      )
    }.forall(_.isDefined)
    Either.cond(inserted, "ok", s"cannot insert roles ${rolesIds} to admin user ${adminUserId}")
  }

  def revokeRolesOfUserTransaction(adminUserId: Long)(implicit connection: Connection): Either[String, Int] = {
    val updateSQL = s" update $tableName set is_deleted = true, last_modification_date = NOW() where admin_user_id = $adminUserId "
    Right(SQL(updateSQL).executeUpdate())
  }

}
