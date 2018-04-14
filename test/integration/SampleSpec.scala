package integration

import integration.common.{TestServer, RandomHelper, TestHelper}
import play.api.libs.json.Json
import play.api.libs.json.Reads._
import play.api.test.Helpers._

class SampleSpec extends TestServer {

  "Home Spec" must {

    "200 OK at GET request" in {
      val response = TestHelper.get("/")
      response.status mustBe 200
    }
  
  }

}
