package integration.common

import com.github.nscala_time.time.Imports._


object RandomHelper {

  // Random generator
  val random = new scala.util.Random
  val alphabetAlphaNumeric = "abcdefghijklmnopqrstuvwxyz0123456789"
  val alphabetAlphabet = "abcdefghijklmnopqrstuvwxyz"
  val alphabetNumeric = "0123456789"
  val MAX_RANDOM = 80

  // Generate a random string of length n from the given alphabet
  def randomString(alphabet: String = alphabetAlphabet)(n: Int): String = {
    val length = if (n == 0) 1 else n
    Stream.continually(random.nextInt(alphabet.length)).map(alphabet).take(length).mkString
  }

  // Generate a random alphabnumeric string of length n
  def randomAlphanumericString(n: Int = random.nextInt(MAX_RANDOM)): String =
    randomString(alphabetAlphaNumeric)(n)

  def randomAlphabetString(n: Int = random.nextInt(MAX_RANDOM)): String =
    randomString(alphabetAlphabet)(n)

  def randomNumericString(n: Int = random.nextInt(MAX_RANDOM)): String =
    randomString(alphabetNumeric)(n)

  def randomPermalink: String = randomString(alphabetAlphabet)(40) // scalastyle:ignore

  val phoneLength = 12
  val usernameLength = 10
  val emailLength = 5
  val dniLength = 7
  val firstnameLength = 5
  val passwordLength = 7

  def randomPhoneNumber: String = randomNumericString(phoneLength)

  def randomEmail: String = s"somedude+${DateTime.now(DateTimeZone.UTC).getMillis}${randomAlphanumericString(emailLength)}@gmail.com"

  def randomUsername: String = randomAlphabetString(usernameLength)

  def randomFirstname: String = {
    val num = random.nextInt(firstnameLength) + 3 // scalastyle:ignore
    randomAlphabetString(num)
  }

  def randomPassword: String = "prefix" + randomNumericString(passwordLength)

}
