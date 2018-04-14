package services.user

import play.api.Logger
import com.github.nscala_time.time.Imports._
import play.api.Configuration
import security.MD5
import services.user.EmailType.EmailType
import models.user._
import com.sendgrid._

import models.email.{EmailTemplateRepo, EmailTemplate}


object EmailType extends Enumeration {
  type EmailType = Value
  val Verification = Value("VERIFICATION")
  val ResetPassword = Value("RESET_PASSWORD")
}

object EmailTemplateName extends Enumeration {
  type EmailTemplateName = Value
  val Verification = Value("account-activation")
  val ResetPassword = Value("password-reset")
}

class UserEmailService(userRepo: UserRepo,
                       config: Configuration,
                       userProfileRepo: UserProfileRepo,
                       emailVerificationRepo: EmailVerificationRepo,
                       emailResetPasswordRepo: EmailResetPasswordRepo,
                       emailTemplateRepo: EmailTemplateRepo) {

  lazy val emailEnabled = config.get[Boolean]("email.enabled")
  lazy val sendgridApikey = config.get[String]("app.sendgrid.apikey")
  lazy val sendgridFrom = config.get[String]("app.sendgrid.from")
  lazy val sendgridFromName = config.get[String]("app.sendgrid.fromname")
  lazy val siteHost = config.get[String]("app.site.host")
  lazy val activationUri = config.get[String]("app.site.uris.activation")
  lazy val resetPasswordUri = config.get[String]("app.site.uris.resetPassword")

  lazy val emailValidationHash: String = "email-verification"

  def sendVerificationEmail(userId: Long): Either[String, String] = {
    Logger.debug(s"Email service sendVerificationMail user #$userId")
    for {
      user <- userRepo.findById(userId).toRight("error.user.notfound").right
      activationUrl <- sendVerificationEmail(user).right
    } yield activationUrl
  }

  def sendVerificationEmail(user: User): Either[String, String] = {
    Logger.debug(s"Email service sendVerificationMail user #${user.id}")
    for {
      userProfile <- userProfileRepo.findByUserId(user.id).toRight("error.userprofile.notfound").right
      deletePrevious <- deletePreviousActiveEmailVerifications(user.email).right
      hash <- verificationHash(user.email, user.id).right
      activationUrl <- activationUrl(hash.toString, user.email).right
      activationEmailTemplate <- resolveEmailTemplate(EmailType.Verification).right
      activationEmail <- buildActivationEmail(activationEmailTemplate, userProfile.firstname, activationUrl).right
      sendTemplate <- sendTemplateEmail(Array(user.email), activationEmail).right
      emailVerification <- emailVerificationRepo.save(user.id, user.email, hash.toString).right
    } yield {
      Logger.debug(s"The activation url for user ${user.id} is $activationUrl")
      activationUrl
    }
  }

  def sendResetPasswordMail(userId: Long): Either[String, String] = {
    Logger.debug(s"Email service sendResetPasswordMail user #$userId")
    for {
      user <- userRepo.findById(userId).toRight("error.user.notfound").right
      userProfile <- userProfileRepo.findByUserId(user.id).toRight("error.userprofile.notfound").right
      hash <- verificationHash(user.email, user.id).right
      resetPasswordUrl <- resetPasswordUrl(hash.toString).right
      resetPasswordEmailTemplate <- resolveEmailTemplate(EmailType.ResetPassword).right
      resetPasswordEmail <- buildResetPasswordEmail(resetPasswordEmailTemplate, userProfile.firstname, resetPasswordUrl).right
      sendTemplate <- sendTemplateEmail(Array(user.email), resetPasswordEmail).right
      emailVerification <- emailResetPasswordRepo.save(user.id, hash).toRight("internal.problem").right
    } yield hash
  }

  private def verificationHash(email: String, userId: Long) =
    Right(MD5.md5Hex(userId + email + emailValidationHash + DateTime.now(DateTimeZone.UTC).getMillis))

  private def buildActivationEmail(emailTemplate: EmailTemplate, firstname: String, activationUrl: String): Either[String, EmailTemplate] = {
    val bodyHtmlRaw = emailTemplate.bodyHtml
    val bodyHtmlUsername = bodyHtmlRaw.replace("*|USERNAME|*", firstname)
    val bodyHtml = bodyHtmlUsername.replace("*|ACTIVATION_URL|*", activationUrl)
    val bodyTextRaw = emailTemplate.bodyText
    val bodyTextUsername = bodyTextRaw.replace("*|USERNAME|*", firstname)
    val bodyText = bodyTextUsername.replace("*|ACTIVATION_URL|*", activationUrl)
    Right(emailTemplate.replaceBody(bodyHtml, bodyText))
  }

  private def buildResetPasswordEmail(emailTemplate: EmailTemplate, firstname: String, resetPasswordUrl: String): Either[String, EmailTemplate] = {
    val bodyHtmlRaw = emailTemplate.bodyHtml
    val bodyHtmlUsername = bodyHtmlRaw.replace("*|USERNAME|*", firstname)
    val bodyHtml = bodyHtmlUsername.replace("*|RESETPASS_URL|*", resetPasswordUrl)
    val bodyTextRaw = emailTemplate.bodyText
    val bodyTextUsername = bodyTextRaw.replace("*|USERNAME|*", firstname)
    val bodyText = bodyTextUsername.replace("*|RESETPASS_URL|*", resetPasswordUrl)
    Right(emailTemplate.replaceBody(bodyHtml, bodyText))
  }
  
  def sendTemplateEmail(toEmails: Array[String], emailTemplate: EmailTemplate): Either[String, String] = {
    if (emailEnabled) {
      val sendgrid = new SendGrid(sendgridApikey)
      val email = new Mail()
      val personalization = new Personalization()
      val content = new Content()

      val from = new Email()
      from.setName(sendgridFromName)
      from.setEmail(sendgridFrom)
      email.setFrom(from)
      email.setTemplateId(emailTemplate.templateId)


      toEmails.foreach { e =>
        val to = new Email()
        to.setName("")
        to.setEmail(e)
        personalization.addTo(to)
      }
      
      email.setSubject(emailTemplate.subject)
      content.setType("text/html")
      content.setValue(emailTemplate.bodyHtml)
      // TODO: agregarle el body txt
      email.addContent(content)
      email.addPersonalization(personalization)
      // email.addSubstitution("*|CURRENT_YEAR|*", Array(DateTime.now(DateTimeZone.UTC).getYear.toString))

      try {
        val request = new Request()
        request.setMethod(Method.POST)
        request.setEndpoint("mail/send")
        request.setBody(email.build())
        val response = sendgrid.api(request)
        Logger.debug(s"Sendgrid API response statusCode: ${response.getStatusCode()} -- body: ${response.getBody()} -- header: ${response.getHeaders()}")
        Right("done")
      } catch {
        case ex: Exception => {
          Logger.error(s"Problems sending email: $ex")
          Left("cannot_send_email")
        }
      }
    } else {
      Logger.debug(s"Email service send template off -- to: $toEmails emailTemplate: $emailTemplate ")
      Right("done")
    }
  }

  private def deletePreviousActiveEmailVerifications(email: String): Either[String, String] = {
    val listEitherDeletedEmailVerifications = emailVerificationRepo.findByEmail(email).map { activeEmailVerification =>
      emailVerificationRepo.updateDeleteEmailVerification(activeEmailVerification.id)
    }
    checkList(listEitherDeletedEmailVerifications, 0)
  }

  private def checkList(l: List[Either[String, Int]], goodCount: Int): Either[String, String] = l match {
    case Left(err) :: xs => Left(err)
    case Right(_) :: xs => checkList(xs, goodCount + 1)
    case Nil => Right("previous.emailverifications.deleted")
  }

  private def resolveEmailTemplate(emailType: EmailType): Either[String, EmailTemplate] = emailType match {
    case EmailType.Verification =>
      emailTemplateRepo.findByName(EmailTemplateName.Verification.toString).toRight("error.emailtemplate.notfound")
    case EmailType.ResetPassword =>
      emailTemplateRepo.findByName(EmailTemplateName.ResetPassword.toString).toRight("error.emailtemplate.notfound")
    case _ =>
      Left("error.emailtemplate.notfound")
  }

  private def activationUrl(hash: String, email: String): Either[String, String] = {
    val activationUrl = siteHost + activationUri
    Right(activationUrl.replace("{activationHash}", hash).replace("{email}", email))
  }

  private def resetPasswordUrl(hash: String) : Either[String, String] = {
    val resetUrl = siteHost + resetPasswordUri
    Right(resetUrl.replace("{verificationCode}", hash))
  }

}
