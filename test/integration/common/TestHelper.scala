package integration.common

import _root_.models.user._
import anorm._
import play.api.Configuration
import org.scalatestplus.play._
import play.api._
import play.api.db.Database
import play.api.libs.json._
import play.api.libs.ws._
import play.api.test.Helpers._
import play.api.test.WsTestClient

object TestHelper extends PlaySpec {

  lazy val port = "19001"
  implicit lazy val portImpl = new play.api.http.Port(port.toInt)

  def put(url: String, body: JsValue, hdrs: (String, String)*): WSResponse = WsTestClient.withClient { client =>
    val wsRequest = client.url(url)
      .addHttpHeaders(hdrs: _*)
      .addHttpHeaders("X-Platform" -> "web", "X-Source" -> "source", "X-Ip" -> "127.0.0.1", "X-Client-Id" -> "tester")
    await(wsRequest.put(body))
  }

  def post(url: String, body: JsValue, hdrs: (String, String)*): WSResponse = WsTestClient.withClient { client =>
    val wsRequest = client.url(url)
      .addHttpHeaders(hdrs: _*)
      .addHttpHeaders("X-Platform" -> "web", "X-Source" -> "source", "X-Ip" -> "127.0.0.1", "X-Client-Id" -> "tester")
    await(wsRequest.post(body))
  }

  def get(url: String, hdrs: (String, String)*): WSResponse = WsTestClient.withClient { client =>
    val wsRequest = client.url(url)
      .addHttpHeaders(hdrs: _*)
      .addHttpHeaders("X-Platform" -> "web", "X-Source" -> "source", "X-Ip" -> "127.0.0.1", "X-Client-Id" -> "tester")
    await(wsRequest.get())
  }

  def delete(url: String, hdrs: (String, String)*): WSResponse = WsTestClient.withClient { client =>
    val wsRequest = client.url(url)
      .addHttpHeaders(hdrs: _*)
      .addHttpHeaders("X-Platform" -> "web", "X-Source" -> "source", "X-Ip" -> "127.0.0.1", "X-Client-Id" -> "tester")
    await(wsRequest.delete())
  }

  def validateAndGet[T](optional: Option[T]): T = {
    optional mustBe defined
    optional.get
  }

  def validateAndGet[L,R](either: Either[L,R]): R = {
    validateAndGet(either.right.toOption)
  }
}
