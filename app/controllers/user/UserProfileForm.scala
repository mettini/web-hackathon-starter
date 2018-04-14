package controllers.user

object UserProfileForm {
  import play.api.data.Forms._
  import play.api.data.Form

  import common.auth.AuthValidator

  case class Data(firstName: String, lastName: String, email: String)

  val form = Form(
    mapping(
      "firstName" -> text.verifying("error.emptyfirstName", { !_.isEmpty }),
      "lastName" -> text.verifying("error.emptylastName", { !_.isEmpty }),
      "email" -> text
        .verifying("error.emptyEmail", {!_.isEmpty})
        .verifying("error.invalidEmail", { AuthValidator.isValidEmail(_) })
    )(Data.apply)(Data.unapply)
  )
}
