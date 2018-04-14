package models.user

import java.sql.Connection

import anorm.JodaParameterMetaData._
import anorm.SqlParser._
import anorm._
import com.github.nscala_time.time.Imports._
import play.api.Logger
import play.api.db.Database
import play.api.libs.json._

import models.BaseModel
import models.user.User.UserVerificationStatus
import models.user.User.UserVerificationStatus.UserVerificationStatus


case class User(
  id: Long, 
  email: String,
  password: String = "dummy-password",
  verificationStatus: UserVerificationStatus.UserVerificationStatus,
  config: UserConfig,
  isDeleted: Boolean,
  creationDate: DateTime,
  lastModificationDate: DateTime) {

  def isModerated: Boolean = config.isModerated
  def isTest: Boolean = config.isTest

  override def toString: String = 
    s"""
    User:
      id -> $id
      email -> $email
      verificationStatus -> $verificationStatus
      isTest -> $isTest
      isModerated -> $isModerated"""
}
  

case class UserRepo(userConfigRepo: UserConfigRepo, db: Database) extends BaseModel[User] {

  override def parser: RowParser[User] = {
    long("id") ~
    str("email") ~
    str("password") ~
    str("verification_status") ~
    bool("is_deleted") ~
    get[DateTime]("creation_date") ~
    get[DateTime]("last_modification_date") map { case
      id~
      email~
      password~
      activationStatus~
      isDeleted~
      creationDate~
      lastModificationDate => User(
        id,
        email,
        password,
        UserVerificationStatus.withName(activationStatus),
        userConfigRepo.getUserConfig(id),
        isDeleted,
        creationDate,
        lastModificationDate
      )
    }
  }

  override def tableName: String = "users"

  def getActiveUsers: Seq[User] = {
    db.withConnection { implicit connection =>
      SQL("""
        SELECT *
        FROM users
        WHERE is_deleted = false
      """).as(parser.*)
    } filter(!_.config.isModerated)
  }

  def findByEmail(email: String): Option[User] = {
    Logger.debug(s"User findByEmail: $email")
    db.withConnection { implicit connection =>
      SQL("""
          SELECT *
          FROM users
          WHERE email = {email}
          AND is_deleted = false
          ORDER BY verification_status, creation_date
          LIMIT 1
      """).on('email -> email).as(parser.singleOpt)
    }
  }

  def save(email: String,
           password: String,
           verificationStatus: UserVerificationStatus = UserVerificationStatus.Pending,
           isTest: Boolean = false,
           isModerated: Boolean = false): Either[String, User] = {

    Logger.debug(s"""User save email: $email verificationStatus: $verificationStatus isTest: $isTest isModerated: $isModerated""")
    db.withTransaction { implicit connection =>
      for {
        userId <- saveUserWithConfig(email, password, verificationStatus).right
        userConfigId <- userConfigRepo.save(userId, isTest, isModerated).right
        user <- findByIdTransaction(userId).toRight(s"problem retriving userId $userId").right
      } yield user
    }
  }

  private def saveUserWithConfig(email: String,
                                 password: String,
                                 verificationStatus: UserVerificationStatus = UserVerificationStatus.Pending)
                                (implicit connection: Connection): Either[String, Long] = {
    insert(
      "email" -> email,
      "password" -> password,
      "verification_status" -> verificationStatus.toString,
      "creation_date" -> DateTime.now(DateTimeZone.UTC),
      "last_modification_date" -> DateTime.now(DateTimeZone.UTC)
    ).toRight("there was an error while creating the user")
  }

  def habeasData(userId: Long): Either[String, Int] = {
    Logger.debug(s"User habeasDataUser userId #$userId")
    findById(userId) match {
      case Some(user: User) =>
        (for {
          updateEmail <- habeasDataEmail(user).right
          isDeleted <- update(userId, Seq(NamedParameter("is_deleted", true))).right
        } yield isDeleted).fold(
          error => Left(error),
          response => Right(response)
        )
      case _ => Left("error.habeasdata.userIdNotFound")
    }
  }

  def updateVerificationStatusActive(userId: Long): Either[String, Int] =
    update(userId, Seq(NamedParameter("verification_status", UserVerificationStatus.Active.toString)))

  def moderateUser(userId: Long): Either[String, String] =
    userConfigRepo.setModeratedConfig(userId, true)

  def updatePassword(userId: Long, newPass: String): Either[String, Long] =
    for {
      _ <- update(userId, Seq(NamedParameter("password", newPass))).right
    } yield userId

  private def habeasDataEmail(user: User): Either[String, Int] = {
    val beginPosition: Int = 0
    val endPosition: Int = 9
    update(user.id, Seq(NamedParameter("email", "habeasdata-" +
      DateTime.now.toString.substring(beginPosition, endPosition) + "-" + user.email)))
  }

  def updateUserModificationDateToNow(userId: Long): Either[String, Int] = {
    db.withConnection { implicit connection =>
      val updateModificationDateSQL = " update users set last_modification_date = NOW() where id = {userId} "
      SQL(updateModificationDateSQL).on(NamedParameter("userId", userId)).executeUpdate() match {
        case 1 => Right(1)
        case 0 => Left(s"there was an error updating user modification date for user #$userId")
      }
    }
  }
}

object User {

  object UserVerificationStatus extends Enumeration {
    type UserVerificationStatus = Value
    val Active = Value("ACTIVE")
    val Pending = Value("PENDING")

    implicit val userVerificationStatusFormat = new Format[UserVerificationStatus] {
      def reads(json: JsValue) = JsSuccess(UserVerificationStatus.withName(json.as[String]))
      def writes(myEnum: UserVerificationStatus) = JsString(myEnum.toString)
    }
  }

  def apply(id: Long,
           email: String,
           password: String,
           verificationStatus: UserVerificationStatus.UserVerificationStatus,
           isTest: Boolean,
           isDeleted: Boolean,
           isModerated: Boolean,
           creationDate: DateTime,
           lastModificationDate: DateTime) : User = {
    val config = UserConfig(isTest, isModerated)
    User(id, email, password, verificationStatus, config, isDeleted, creationDate, lastModificationDate)
  }

  def applyWithoutPass(id: Long,
                       email: String,
                       verificationStatus: UserVerificationStatus.UserVerificationStatus,
                       isTest: Boolean,
                       isDeleted: Boolean,
                       isModerated: Boolean,
                       creationDate: DateTime,
                       lastModificationDate: DateTime) : User = {
    val config = UserConfig(isTest, isModerated)
    User(id, email, "", verificationStatus, config, isDeleted, creationDate, lastModificationDate)
  }

  def unapplyWithoutPass(u: User) : Some[(Long, String, UserVerificationStatus.UserVerificationStatus,
    Boolean, Boolean, Boolean, DateTime, DateTime)] = {
    Some((u.id, u.email, u.verificationStatus,
      u.isTest, u.isDeleted, u.isModerated,
      u.creationDate, u.lastModificationDate))
  }
}
