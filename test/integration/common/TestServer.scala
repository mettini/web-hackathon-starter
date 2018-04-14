package integration.common

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatest.ParallelTestExecution
import org.scalatestplus.play._
import play.api._
import play.api.libs.json._
import play.api.mvc.Results._
import play.api.mvc._

trait TestServer extends PlaySpec with GuiceOneServerPerSuite with TestLoaderComponents {

  // "app.port" -> port.asInstanceOf[AnyRef],
  val customTestConf: Map[String, AnyRef] = Map (
    "email.enabled" -> false.asInstanceOf[AnyRef],
    "db.default.url" -> "jdbc:h2:mem:play;MODE=MYSQL;DB_CLOSE_DELAY=-1",
    "db.default.driver" -> "org.h2.Driver"
  )

  val customTestRoutes: PartialFunction[(String, String), play.api.mvc.Handler] = {
    case ("GET", "/get-200") => Action { request =>
      Ok(Json.obj("error" -> false))
    }
  }

  override implicit lazy val app: Application = buildTestApp(customTestRoutes, customTestConf)

  def validateAndGet[T](optional: Option[T]): T = {
    optional mustBe defined
    optional.get
  }

  def validateAndGet[T](optional: Option[T], clue: String): T = {
    withClue(clue) { optional mustBe defined }
    optional.get
  }

}
