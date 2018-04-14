package models.user

import com.github.nscala_time.time.Imports._

import anorm._
import anorm.JodaParameterMetaData._
import anorm.SqlParser._

import play.api.Logger
import play.api.db.Database

import models.BaseModel

case class EmailResetPassword(
  id: Long,
  userId: Long,
  hash: String,
  used: Boolean = false,
  expirationDate: DateTime,
  resetedDate: Option[DateTime],
  isDeleted: Boolean,
  creationDate: DateTime,
  lastModificationDate: DateTime
)


class EmailResetPasswordRepo(val db: Database) extends BaseModel[EmailResetPassword] {

  override def parser: RowParser[EmailResetPassword] = {
    long("email_password_resets.id") ~
    long("email_password_resets.user_id") ~
    str("email_password_resets.verification_code") ~
    bool("email_password_resets.used") ~
    get[DateTime]("email_password_resets.expiration_date") ~
    get[DateTime]("email_password_resets.reseted_date").? ~
    bool("email_password_resets.is_deleted") ~
    get[DateTime]("email_password_resets.creation_date") ~
    get[DateTime]("email_password_resets.last_modification_date") map {
      case id~user~hash~used~expirationDate~resetedDate~isDeleted~creationDate~lastModificationDate =>
        EmailResetPassword(id, user, hash, used, expirationDate, resetedDate, isDeleted, creationDate, lastModificationDate)
    }
  }

  def tableName: String = "email_password_resets"

  def findByVerificationCode(verificationCode: String): Option[EmailResetPassword] = {
    db.withConnection { implicit connection =>
      SQL("select * from email_password_resets where verification_code = {verificationCode}")
        .on('verificationCode -> verificationCode).as(parser.singleOpt)
    }
  }

  def findByUserId(userId: Long): Option[EmailResetPassword] = {
    db.withConnection { implicit connection =>
      SQL("select * from email_password_resets where user_id = {userId}").on('userId -> userId).as(parser.singleOpt)
    }
  }

  def save(userId: Long, verificationCode: String): Option[Long] = {
    Logger.debug(s"ResetPassword save userId: #$userId hash: $verificationCode")
    val expirationDate = DateTime.now(DateTimeZone.UTC) + 2.day
    db.withConnection { implicit connection =>
      insert(
        "user_id" -> userId,
        "verification_code" -> verificationCode,
        "used" -> false,
        "expiration_date" -> expirationDate,
        "creation_date" -> DateTime.now(DateTimeZone.UTC),
        "last_modification_date" -> DateTime.now(DateTimeZone.UTC)
      )
    }
  }

  def passwordReseted(id: Long): Either[String,Int] =
    update(id, Seq[NamedParameter]("used" -> true, "reseted_date" -> DateTime.now))

}
