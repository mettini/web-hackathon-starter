package models.auth.requests

import common.auth.AuthValidator

case class SignupRequest(email: String, password: String, firstName: String, lastName: String) {

  def validateInput: Either[String, String] =
    for {
      _ <- AuthValidator.validateEmail(email).right
      _ <- AuthValidator.validateFirstName(firstName).right
      _ <- AuthValidator.validateLastName(lastName).right
      _ <- AuthValidator.validatePassword(password).right
    } yield "ok"

}
