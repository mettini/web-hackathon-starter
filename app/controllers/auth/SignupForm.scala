package controllers.auth

object SignupForm {
  import play.api.data.Forms._
  import play.api.data.Form

  import common.auth.AuthValidator

  case class Data(email: String, password: String, firstName: String, lastName: String)

  val form = Form(
    mapping(
      "email" -> text
        .verifying("error.emptyEmail", { !_.isEmpty })
        .verifying("error.invalidEmail", { AuthValidator.isValidEmail(_) }),
      "password" -> text
        .verifying("error.emptyPassword", {!_.isEmpty})
        .verifying("error.passwordTooShort", _.length >= 8),
      "firstName" -> text.verifying("error.emptyfirstName", { !_.isEmpty }),
      "lastName" -> text.verifying("error.emptylastName", { !_.isEmpty })
    )(Data.apply)(Data.unapply)
  )
}
