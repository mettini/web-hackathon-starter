package services.auth

import play.api.mvc.Request
import scala.util.Random

import security.PasswordManager
import services.user.UserEmailService
import models.user.User.UserVerificationStatus
import models.user._
import models.auth.requests.SignupRequest
import models.auth.LoginCredential


class SignupService(loginService: LoginService,
                    userEmailService: UserEmailService,
                    userRepo: UserRepo,
                    userProfileRepo: UserProfileRepo) {

  def signup(signupRequest: SignupRequest)(implicit request: Request[_]): Either[String, LoginCredential] = {
    for {
      _ <- userRepo.findByEmail(signupRequest.email).map(_ => "email.already.used").toLeft("").right
      user <- doSignup(signupRequest).right
      loginCredential <- loginService.login(signupRequest.email, signupRequest.password).right
      _ <- sendVerificationEmail(user).right
    } yield loginCredential
  }

  def sendVerificationEmail(user: User): Either[String, String] = userEmailService.sendVerificationEmail(user)

  private def doSignup(signupRequest: SignupRequest)(implicit request: Request[_]): Either[String, User] = {
    val verificationStatus = UserVerificationStatus.Pending
    val password = PasswordManager.encodePassword(signupRequest.password)
    for {
      user <- userRepo.save(email = signupRequest.email, password = password, verificationStatus = verificationStatus).right
      userProfile <- signupProfile(user.id, signupRequest.firstName, signupRequest.lastName).right
    } yield user
  }

  private def signupProfile(userId: Long, firstname: String, lastname: String): Either[String, UserProfile] = {
    userProfileRepo.save(userId = userId, firstname = firstname, lastname = Some(lastname))
  }

}
