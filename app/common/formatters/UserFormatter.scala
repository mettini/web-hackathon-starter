package common.formatters

import com.github.nscala_time.time.Imports._

import models.user.User.UserVerificationStatus
import models.user.User.UserVerificationStatus.UserVerificationStatus
import play.api.libs.functional.syntax._
import play.api.libs.json._
import models.user.User

object UserFormatter {

  import common.formatters.DateTimeFormatter._

  implicit val userWrites: Writes[User] = (
    (JsPath \ "id").write[Long] and
    (JsPath \ "email").write[String] and
    (JsPath \ "verificationStatus").write[UserVerificationStatus] and
    (JsPath \ "isTest").write[Boolean] and
    (JsPath \ "isDeleted").write[Boolean] and
    (JsPath \ "isModerated").write[Boolean] and
    (JsPath \ "creationDate").write[DateTime] and
    (JsPath \ "lastModificationDate").write[DateTime]
    )(unlift(User.unapplyWithoutPass))

  implicit val userReads: Reads[User] = (
    (JsPath \ "id").read[Long] and
    (JsPath \ "email").read[String] and
    (JsPath \ "verificationStatus").read[UserVerificationStatus] and
    (JsPath \ "isTest").read[Boolean] and
    (JsPath \ "isDeleted").read[Boolean] and
    (JsPath \ "isModerated").read[Boolean] and
    (JsPath \ "creationDate").read[DateTime] and
    (JsPath \ "lastModificationDate").read[DateTime]
    )(User.applyWithoutPass _)

}
