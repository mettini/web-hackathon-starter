package controllers.admin.auth

object AdminLoginForm {
  import play.api.data.Forms._
  import play.api.data.Form

  case class Data(username: String, password: String)

  val form = Form(
    mapping(
      "username" -> text
        .verifying("error.emptyUsername", {!_.isEmpty}),
      "password" -> text
        .verifying("error.emptyPassword", {!_.isEmpty})
        .verifying("error.passwordTooShort", _.length >= 8)
    )(Data.apply)(Data.unapply)
  )
}
