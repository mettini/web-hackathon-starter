package models.user

import com.github.nscala_time.time.Imports._

import anorm._
import anorm.JodaParameterMetaData._
import anorm.SqlParser._

import play.api.db.Database
import play.api.libs.json.Json
import play.api.Logger

import models.BaseModel

sealed abstract class EmailVerificationStatus(val value: String)
case object VERIFIED extends EmailVerificationStatus("verified")
case object PENDING extends EmailVerificationStatus("pending")
case object EXPIRED extends EmailVerificationStatus("expired")

case class EmailVerification(
  id: Long,
  userId: Long,
  email: String,
  hash: String,
  verificationStatus: String,
  expirationDate: DateTime,
  isDeleted: Boolean,
  creationDate: DateTime,
  lastModificationDate: DateTime
)

class EmailVerificationRepo(val db: Database) extends BaseModel[EmailVerification] {

  override def parser: RowParser[EmailVerification] = {
    long("id") ~
    long("user_id") ~
    str("email") ~
    str("verification_code") ~
    str("verification_status") ~
    get[DateTime]("expiration_date") ~
    bool("is_deleted") ~
    get[DateTime]("creation_date") ~
    get[DateTime]("last_modification_date") map { case
      id~
      userId~
      email~
      hash~
      verificationStatus~
      expirationDate~
      isDeleted~
      creationDate~
      lastModificationDate => EmailVerification(
        id,
        userId,
        email,
        hash,
        verificationStatus,
        expirationDate,
        isDeleted,
        creationDate,
        lastModificationDate
      )
    }
  }

  def tableName: String = "email_verifications"

  def findByEmail(email: String): List[EmailVerification] = {
    Logger.debug(s"EmailVerification findByEmail email: $email")
    db.withConnection { implicit connection =>
      SQL("""
          SELECT *
          FROM email_verifications
          WHERE email = {email}
          AND verification_status = {verificationStatus}
          AND is_deleted = {isDeleted}
          """).on('email -> email, 'verificationStatus -> PENDING.value, 'isDeleted -> false).as(parser.*)
    }
  }

  def findByHash(hash: String): Option[EmailVerification] = {
    Logger.debug(s"EmailVerification findByHash hash: $hash")
    db.withConnection { implicit connection =>
      SQL("""
        SELECT *
          FROM email_verifications
          WHERE verification_code = {hash}
          ORDER BY id DESC
          LIMIT 1
      """).on('hash -> hash).as(parser.singleOpt)
    }
  }

  def updateVerificationStatusVerified(id: Long): Either[String, Int] =
    update(id, Seq(NamedParameter("verification_status", VERIFIED.value)))

  def updateDeleteEmailVerification(id: Long): Either[String, Int] =
    update(id, Seq(NamedParameter("is_deleted", true)))

  def save(userId: Long,
           email: String,
           hash: String,
           verificationStatus: String = PENDING.value,
           expirationDate: DateTime = DateTime.now(DateTimeZone.UTC) + 6.months): Either[String, EmailVerification] = {
    Logger.debug(s"""EmailVerification save userId: $userId email: $email hash: $hash
     verificationStatus: $verificationStatus expirationDate: $expirationDate""")
    db.withConnection { implicit connection =>
      insert(
        "user_id" -> userId,
        "email" -> email,
        "verification_code" -> hash,
        "verification_status" -> verificationStatus,
        "expiration_date" -> expirationDate,
        "creation_date" -> DateTime.now(DateTimeZone.UTC),
        "last_modification_date" -> DateTime.now(DateTimeZone.UTC)
      ).flatMap(findById).toRight("there was an error while creating the email verification")
    }
  }

}

object EmailVerificationRepo {
  import common.formatters.DateTimeFormatter._
  implicit val emailVerificationFormat = Json.format[EmailVerification]
}
