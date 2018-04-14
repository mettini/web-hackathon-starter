package models.user

import com.github.nscala_time.time.Imports._

import anorm._
import anorm.JodaParameterMetaData._
import anorm.SqlParser._
import play.api.db.Database

import play.api.libs.json.Json
import play.api.Logger

import models.BaseModel

import models.user.requests.UserProfileRequest

case class UserProfile(
  id: Long,
  userId: Long,
  firstname: String,
  lastname: Option[String],
  creationDate: DateTime,
  lastModificationDate: DateTime
)

class UserProfileRepo(val db: Database) extends BaseModel[UserProfile] {

  override def parser: RowParser[UserProfile] = {
    long("id") ~
    long("user_id") ~
    str("firstname") ~
    str("lastname").? ~
    get[DateTime]("creation_date") ~
    get[DateTime]("last_modification_date") map { case
      id~
      userId~
      firstname~
      lastname~
      creationDate~
      lastModificationDate => UserProfile(
        id,
        userId,
        firstname,
        lastname,
        creationDate,
        lastModificationDate
      )
    }
  }

  override def tableName: String = "user_profiles"

  def findByUserId(userId: Long): Option[UserProfile] = {
    Logger.trace(s"UserProfile find by userId #$userId")
    db.withConnection { implicit connection =>
      SQL("""
          SELECT *
          FROM user_profiles
          WHERE user_id = {userId}
      """).on('userId -> userId).as(parser.singleOpt)
    }
  }

  def save(userId: Long, firstname: String, lastname: Option[String] = None): Either[String, UserProfile] = {
    Logger.debug(s"""UserProfile save userId: $userId firstname: $firstname lastname: $lastname""")
    db.withConnection { implicit connection =>
      insert(
        "user_id" -> userId,
        "firstname" -> firstname,
        "lastname" -> lastname,
        "creation_date" -> DateTime.now(DateTimeZone.UTC),
        "last_modification_date" -> DateTime.now(DateTimeZone.UTC)
      ).flatMap(findById).toRight("error.userprofile.save")
    }
  }

  def updateProfile(userId: Long, userProfileRequest: UserProfileRequest): Either[String, UserProfile] = {
    Logger.debug(s"UserProfile update user: #$userId, userProfileRequest: $userProfileRequest")
    val updateResult = update(userId, Seq(
      NamedParameter("firstname", userProfileRequest.firstname),
      NamedParameter("lastname", userProfileRequest.lastname)
    ))
    updateResult match {
      case Right(1) => val updatedProfile = findByUserId(userId)
                updatedProfile match {
                    case Some(profile: UserProfile) => Right(profile)
                    case _ => Left(s"there was an error retrieving updated user profile for user #$userId")
                }
      case _ => Left(s"there was an error retrieving updated user profile for user #$userId")
    }
  }

  def updateProfileModificationDateToNow(userId: Long): Either[String, Int] = {
    db.withConnection { implicit connection =>
      val updateModificationDateSQL = " update user_profiles set last_modification_date = NOW() where user_id = {userId}"
      SQL(updateModificationDateSQL).on(NamedParameter("userId", userId)).executeUpdate() match {
        case 1 => Right(1)
        case 0 => Left(s"there was an error updating user profile modification date for user #$userId")
      }
    }
  }

}

object UserProfileRepo {
  import common.formatters.DateTimeFormatter._
  implicit val userProfileFormat = Json.format[UserProfile]
}
