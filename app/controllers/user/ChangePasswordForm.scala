package controllers.user

object ChangePasswordForm {
  import play.api.data.Forms._
  import play.api.data.Form

  import common.auth.AuthValidator

  case class Data(currentPassword: String, newPassword: String)

  val form = Form(
    mapping(
      "currentPassword" -> text
          .verifying("error.emptyCurrentPassword", {!_.isEmpty})
          .verifying("error.currentPasswordTooShort", _.length >= 8),
      "passwordTuple" -> tuple(
        "password" -> text
          .verifying("error.emptyNewPassword", {!_.isEmpty})
          .verifying("error.newPasswordTooShort", _.length >= 8),
        "repeatPassword" -> text.verifying("error.emptyRepeatPassword", {!_.isEmpty})
      ).verifying("error.passwordsNotMatch", password => password._1 == password._2)
    )((currentPassword, passwords) => Data(currentPassword, passwords._1))
    (data => Some(Tuple2(data.currentPassword, Tuple2(data.newPassword, data.newPassword))))
  )
}
