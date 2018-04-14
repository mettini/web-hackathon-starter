package integration.common

import java.io.File
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.PlaySpec
import play.api.ApplicationLoader.Context
import play.api._
import play.api.libs.ws.WSClient
import play.api.mvc.{EssentialFilter, Handler, RequestHeader}
import play.api.routing.Router
import scala.runtime.AbstractPartialFunction
import di.AppComponents


trait TestLoaderComponents extends TestDB with MockFactory { this: PlaySpec =>

  type Routes = PartialFunction[(String, String), play.api.mvc.Handler]

  def buildTestApp(routes: Routes, conf: Map[String, AnyRef]): Application = {
    val appLoader = new ApplicationLoader {
      override def load(context: Context): Application = new AppComponents(context) {
        override val db = testDatabase
        override lazy val router: Router = FakeRoutes(routes, appRouter)
        override lazy val httpFilters: Seq[EssentialFilter] = Seq()
      }.application
    }

    val context = ApplicationLoader.createContext( initialSettings = conf,
      environment = new Environment(new File("."), ApplicationLoader.getClass.getClassLoader, Mode.Test)
    )

    LoggerConfigurator(context.environment.classLoader).foreach { _.configure(context.environment) }
    appLoader.load(context)
  }
}

case class FakeRoutes(injected: PartialFunction[(String, String), Handler], fallback: Router) extends Router {
  def documentation: Seq[(String, String, String)] = fallback.documentation
  // Use withRoutes first, then delegate to the parentRoutes if no route is defined
  val routes = new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B >: Handler](rh: A, default: A => B) =
      injected.applyOrElse((rh.method, rh.path), (_: (String, String)) => default(rh))
    def isDefinedAt(rh: RequestHeader) = injected.isDefinedAt((rh.method, rh.path))
  } orElse new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B >: Handler](rh: A, default: A => B) =
      fallback.routes.applyOrElse(rh, default)
    def isDefinedAt(x: RequestHeader) = fallback.routes.isDefinedAt(x)
  }
  def withPrefix(prefix: String): Router = {
    new FakeRoutes(injected, fallback.withPrefix(prefix))
  }
}

