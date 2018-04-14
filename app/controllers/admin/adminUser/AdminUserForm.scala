package controllers.admin.adminUser

object AdminUserForm {
  import play.api.data.Forms._
  import play.api.data.Form

  case class Data(username: String, password: String, name: String, rolesIds: List[Long])
  case class EditData(username: String, name: String, rolesIds: List[Long])
  case class PasswordData(newPassword: String)

  val form = Form(
    mapping(
      "username" -> text
        .verifying("error.emptyUsername", { !_.isEmpty }),
      "password" -> text
        .verifying("error.emptyPassword", {!_.isEmpty})
        .verifying("error.admin.passwordTooShort", _.length >= 12),
      "name" -> text.verifying("error.emptyfirstName", { !_.isEmpty }),
      "roles" -> list(longNumber).verifying("error.selectARole", { _.nonEmpty })
    )(Data.apply)(Data.unapply)
  )

  val editForm = Form(
    mapping(
      "username" -> text
        .verifying("error.emptyUsername", { !_.isEmpty }),
      "name" -> text.verifying("error.emptyfirstName", { !_.isEmpty }),
      "roles" -> list(longNumber).verifying("error.selectARole", { _.nonEmpty })
    )(EditData.apply)(EditData.unapply)
  )

  val passwordForm = Form(
    mapping(
      "passwordTuple" -> tuple(
        "password" -> text
          .verifying("error.emptyNewPassword", {!_.isEmpty})
          .verifying("error.newPasswordTooShort", _.length >= 8),
        "repeatPassword" -> text.verifying("error.emptyRepeatPassword", {!_.isEmpty})
      ).verifying("error.passwordsNotMatch", password => password._1 == password._2)
    )((passwords) => PasswordData(passwords._1))
    (data => Some(Tuple2(data.newPassword, data.newPassword)))
  )

}
