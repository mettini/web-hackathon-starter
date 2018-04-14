package models.auth

import models.auth.LoginCredentialStatus.LoginCredentialStatus
import com.github.nscala_time.time.Imports._

import anorm._
import anorm.JodaParameterMetaData._
import anorm.SqlParser._
import play.api.db.Database

import play.api.libs.json._
import play.api.Logger

import models.BaseModel

object LoginCredentialStatus extends Enumeration {
  type LoginCredentialStatus = Value
  val ENABLED = Value("ENABLED")
  val DISABLED = Value("DISABLED")

  implicit val loginCredentialFormat = new Format[LoginCredentialStatus] {
    def reads(json: JsValue) = JsSuccess(LoginCredentialStatus.withName(json.as[String]))
    def writes(myEnum: LoginCredentialStatus) = JsString(myEnum.toString)
  }
}

case class LoginCredential(
  id: Long,
  userId: Long,
  authToken: String,
  status: LoginCredentialStatus.LoginCredentialStatus,
  expirationDate: DateTime,
  isDeleted: Boolean,
  creationDate: DateTime,
  lastModificationDate: DateTime
)

class LoginCredentialRepo(val db: Database) extends BaseModel[LoginCredential] {

  override def parser: RowParser[LoginCredential] = {
    long("id") ~
    long("user_id") ~
    str("auth_token") ~
    str("status") ~
    get[DateTime]("expiration_date") ~
    bool("is_deleted") ~
    get[DateTime]("creation_date") ~
    get[DateTime]("last_modification_date") map { case
      id~
      userId~
      authToken~
      status~
      expirationDate~
      isDeleted~
      creationDate~
      lastModificationDate => LoginCredential(
        id,
        userId,
        authToken,
        LoginCredentialStatus.withName(status),
        expirationDate,
        isDeleted,
        creationDate,
        lastModificationDate
      )
    }
  }

  def tableName: String = "login_credentials"

  def findValidByAuthToken(authToken: String): Option[LoginCredential] = {
    findByAuthToken(authToken).flatMap {
      case lc if lc.status == LoginCredentialStatus.ENABLED && lc.expirationDate >= DateTime.now(DateTimeZone.UTC) => Some(lc)
      case _ => None
    }
  }

  def findByAuthToken(authToken: String): Option[LoginCredential] = {
    Logger.debug(s"LoginCredential find authoken #$authToken")
    db.withConnection { implicit connection =>
      SQL("""
        SELECT *
          FROM login_credentials
          WHERE auth_token = {authToken}
      """).on('authToken -> authToken).as(parser.singleOpt)
    }
  }

  def findByUserId(userId: Long): Seq[LoginCredential] = {
    Logger.debug(s"LoginCredential find userId #$userId")
    db.withConnection { implicit connection =>
      SQL("""
        SELECT *
          FROM login_credentials
          WHERE user_id = {userId}
      """).on('userId -> userId).as(parser.*)
    }
  }

  def save(userId: Long, authToken: String, status: LoginCredentialStatus, expirationDate: DateTime): Option[LoginCredential] = {
    Logger.debug(s"LoginCredential save userId: $userId authToken: $authToken status: $status expirationDate: $expirationDate")
    db.withConnection { implicit connection =>
      insert(
        "user_id" -> userId,
        "auth_token"-> authToken,
        "status" -> status.toString,
        "expiration_date" -> expirationDate,
        "creation_date" -> DateTime.now(DateTimeZone.UTC),
        "last_modification_date" -> DateTime.now(DateTimeZone.UTC)
      ).flatMap(findById)
    }
  }

  def habeasData(userId: Long): Either[String, Int] = {
    Logger.debug(s"LoginCredential habeasData userId #$userId")
    val beginPosition: Int = 0
    val endPosition: Int = 9
    def checkEitherList(sequence: Seq[Either[String, Int]], count: Int): Either[String, Int] = {
      sequence match {
        case Left(error) :: xs =>
          Left(error)
        case Right(_) :: xs =>
          checkEitherList(xs, count + 1)
        case Nil => Right(count)
      }
    }
    checkEitherList(
      findByUserId(userId) map { loginCredential =>
        update(loginCredential.id,
          Seq(NamedParameter("authToken", "habeasdata-" +
            DateTime.now(DateTimeZone.UTC).toString.substring(beginPosition, endPosition) + "-" + loginCredential.authToken)))
      },
      0
    )
  }

  def logout(authToken: String): Either[String, LoginCredential] = {
    findByAuthToken(authToken).fold{
      Left[String, LoginCredential](s"there was a prolem while logging out the user with authToken $authToken").left.e
    }{
      lc => {
        update(lc.id,
          Seq('status -> LoginCredentialStatus.DISABLED.toString, 'expiration_date -> (DateTime.now(DateTimeZone.UTC) - 1.day)))
          .right.map(_ => lc)
      }
    }
  }

}
