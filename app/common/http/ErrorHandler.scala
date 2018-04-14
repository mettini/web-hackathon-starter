package common.http

import org.slf4j.MDC
import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.{RequestHeader, Result}
import play.api.mvc.Results.{InternalServerError, Status}
import scala.concurrent.Future
import scala.util.Try
import play.api.i18n.{MessagesApi, Messages}


class ErrorHandler(messages: MessagesApi) extends HttpErrorHandler {

  implicit val implicitMessages: Messages = implicitly[Messages]

  def onClientError(request: RequestHeader, statusCode: Int, m: String): Future[Result] = {
    val message = if(m.isEmpty) "there was a problem in the request, please check the path and the headers" else m
    Logger.warn(s"There was a client error with statusCode $statusCode in ${request.method} ${request.path} with message: $message")
    Future.successful(Status(statusCode)(views.html.error(statusCode, message)))
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    val message = s"There was a server error in ${request.method} ${request.path}"
    Logger.error(message, exception)
    Future.successful(InternalServerError(views.html.error(500, message)))
  }

}
