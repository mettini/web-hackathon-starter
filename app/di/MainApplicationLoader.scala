package di

import _root_.controllers.AssetsComponents
import com.softwaremill.macwire._
import play.api.ApplicationLoader.Context
import play.api._
import play.api.mvc._
import play.api.routing.Router
import router.Routes
import play.api.http.HttpErrorHandler
import common.http.ErrorHandler
import play.filters.gzip.GzipFilter
import play.filters.csrf.CSRF.SignedTokenProvider
import play.filters.csrf.{CSRFConfig, CSRFFilter}

/**
 * Application loader that wires up the application dependencies using Macwire
 */
class MainApplicationLoader extends ApplicationLoader {
  def load(context: Context): Application = new AppComponents(context).application
}

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context)
  with AppModule
  with AssetsComponents
  with play.filters.HttpFiltersComponents {

  // set up logger
  LoggerConfigurator(context.environment.classLoader).foreach {
    _.configure(context.environment, context.initialConfiguration, Map.empty)
  }

  override lazy val httpErrorHandler: HttpErrorHandler = wire[ErrorHandler]

  lazy val appRouter: Router = {
    // add the prefix string in local scope for the Routes constructor
    val prefix: String = "/"
    wire[Routes]
  }
  lazy val router: Router = appRouter
}
