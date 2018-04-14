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


object AdminUserSessionStatus {
  val Active = "ACTIVE"
  var Finished = "FINISHED"
}

case class AdminUserSession(
  id: Long,
  adminUser: AdminUser,
  sessionKey: String,
  sessionStatus: String,
  isDeleted: Boolean,
  creationDate: DateTime,
  lastModificationDate: DateTime
)

class AdminUserSessionRepo(val db: Database, adminUserRepo: AdminUserRepo) extends BaseModel[AdminUserSession] {

  override def tableName: String = "admin_users_sessions"

  def parser: RowParser[AdminUserSession] = {
    long("id") ~
    long("admin_user_id") ~
    str("session_key") ~
    str("session_status") ~
    bool("is_deleted") ~
    get[DateTime]("creation_date") ~
    get[DateTime]("last_modification_date") map {
      case id~adminUserId~sessionKey~sessionStatus~isDeleted~creationDate~lastModificationDate => 
        AdminUserSession(id, adminUserRepo.getById(adminUserId), sessionKey, sessionStatus, isDeleted, 
          creationDate, lastModificationDate)
    }
  }

  def save(adminUser: AdminUser,
           sessionKey: String): Either[String, AdminUserSession] = {

    Logger.trace(s"AdminUserSession save adminUser: ${adminUser.username}")
    db.withTransaction { implicit connection =>
      for {
        adminUserSessionId <- insertTransaction(adminUser, sessionKey).right
        adminUserSession <- findByIdTransaction(adminUserSessionId).toRight(s"problem retriving adminUserSessionId $adminUserSessionId").right
      } yield adminUserSession
    }
  }

  def findBySessionKey(sessionKey: String): Option[AdminUserSession] = {
    Logger.trace(s"Find user by session key: $sessionKey")
    db.withConnection { implicit connection =>
      SQL(s"""
        SELECT *
        FROM $tableName
        WHERE session_key = {sessionKey} AND is_deleted = false
        """).on('sessionKey -> sessionKey).as(parser.singleOpt)
    }
  }

  def finish(adminUserSession: AdminUserSession): Either[String, Int] = {
    update(adminUserSession.id, Seq(NamedParameter("session_status", AdminUserSessionStatus.Finished)))
  }

  private def insertTransaction(adminUser: AdminUser,
                                sessionKey: String)(implicit connection: Connection): Either[String, Long] = {
    insert(
      "admin_user_id" -> adminUser.id,
      "session_key" -> sessionKey,
      "session_status" -> AdminUserSessionStatus.Active,
      "creation_date" -> DateTime.now(DateTimeZone.UTC),
      "last_modification_date" -> DateTime.now(DateTimeZone.UTC)
    ).toRight("there was an error while creating the admin user session")
  }

}
