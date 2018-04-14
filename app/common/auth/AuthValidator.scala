package common.auth

object AuthValidator {

  def validatePassword(password: String): Either[String, String] = {
    if(password.length < 6) Left("tooshort.password")
    else Right(password)
  }

  def validateEmail(email: String): Either[String, String] = {
    if(isValidEmail(email)) Right(email)
    else Left("invalid.email")
  }

  def validateFirstName(firstName: String): Either[String, String] = {
    if(isValidFirstName(firstName)) Right(firstName)
    else {
      firstName match {
        case name if name.length() < 2 => Left("invalid.firstname.tooshort")
        case name if name.length() > 255 => Left("invalid.firstname.toolong")
        case _ => Left("invalid.firstname")
      }

    }
  }

  def validateLastName(lastName: String): Either[String, String] = {
    if(isValidLastName(lastName)) Right(lastName)
    else {
      lastName match {
        case name if name.length() < 2 => Left("invalid.lastname.tooshort")
        case name if name.length() > 255 => Left("invalid.lastname.toolong")
        case _ => Left("invalid.lastname")
      }

    }
  }

  def isValidEmail(email: String): Boolean = """([\w\.\+-]+)@([\w\.]+)""".r.unapplySeq(email).isDefined

  def isValidFirstName(firstName: String): Boolean ="""^([a-zA-Z0-9\sñáéíóú]){2,255}$""".r.unapplySeq(firstName).isDefined

  def isValidLastName(lastName: String): Boolean ="""^([a-zA-Z0-9\sñáéíóú]){2,255}$""".r.unapplySeq(lastName).isDefined

}
