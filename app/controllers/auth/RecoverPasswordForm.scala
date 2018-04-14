package controllers.auth

object RecoverPasswordForm {
  import play.api.data.Forms._
  import play.api.data.Form

  import common.auth.AuthValidator

  case class Data(email: String)
  case class ResetData(newPassword: String, repeatPassword: String)

  val form = Form(
    mapping(
      "email" -> text
        .verifying("error.emptyEmail", { !_.isEmpty })
        .verifying("error.invalidEmail", { AuthValidator.isValidEmail(_) })
    )(Data.apply)(Data.unapply)
  )

  val resetForm = Form(
    mapping(
      "passwordTuple" -> tuple(
        "password" -> text
          .verifying("error.emptyNewPassword", {!_.isEmpty})
          .verifying("error.newPasswordTooShort", _.length >= 8),
        "repeatPassword" -> text.verifying("error.emptyRepeatPassword", {!_.isEmpty})
      ).verifying("error.passwordsNotMatch", password => password._1 == password._2)
    )(passwords => ResetData(passwords._1, passwords._2))
    (data => Some(data.newPassword -> data.newPassword))
  )

}
