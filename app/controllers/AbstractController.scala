package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.twirl.api.Html
import play.api.i18n.{MessagesApi, Messages}

import services.auth.SessionService
import models.user.{User, UserRepo}
import models.auth.LoginCredential

import common.actions.LoggedRequest
import common.actions.MaybeLoggedRequest
import common.actions.RequestType
import common.actions.AuthActions._
import common.ui.ViewBuilder


abstract class AbstractController(sessionService: SessionService, 
                                  userRepo: UserRepo,
                                  viewBuilder: ViewBuilder,
                                  cc: ControllerComponents,
                                  messages: MessagesApi) extends play.api.mvc.AbstractController(cc) {

  private implicit val implicitMessages: Messages = implicitly[Messages]

  protected[controllers] def badRequestJson(msg: String): Result = {
    Logger.debug(s"BadRequest: error '$msg'")
    BadRequest(Json.obj("error" -> true, "message" -> msg))
  }

  protected[controllers] def errorJson(msg: String): Result = {
    Logger.debug(s"Error: '$msg'")
    InternalServerError(Json.obj("error" -> true, "message" -> msg))
  }

  protected[controllers] def okJson(json: JsValue): Result = {
    Ok(Json.obj("error" -> false, "message" -> "", "content" -> json))
  }

  protected[controllers] def okJson(message: String, json: JsValue): Result = {
    Ok(Json.obj("error" -> false, "message" -> message, "content" -> json))
  }

  protected[controllers] def error500(message: String): Result = {
    InternalServerError(views.html.error(500, message))
  }

  protected[controllers] def obtainJsonField[T](field: String, json: JsValue)(implicit rds: Reads[T]): Either[String, T] = {
    (json \ field).validate[T] match {
      case s: JsSuccess[T] => Right(s.get)
      case e: JsError => Left("invalid." + field)
    }
  }

  protected[controllers] def obtainJsonNullableField[T](field: String, json: JsValue)(implicit rds: Reads[T]): Option[T] = {
    (json \ field).validate[T] match {
      case s: JsSuccess[T] => Some(s.get)
      case e: JsError => None
    }
  }

  protected[controllers] def persistLoginCredentials(lc: LoginCredential, result: Result)(implicit request: Request[AnyContent]): Result = {
    result.withCookies(Cookie(name = "at", value = lc.authToken, path = "/"))
  }

  protected[controllers] def deleteLoginCredentials(result: Result)(implicit request: Request[AnyContent]): Result = {
    result.withCookies(Cookie(name = "at", value = "", path = "/"))
  }

  protected[controllers] def buildLoggedView(view: (User, Request[AnyContent], Messages) => Html)(implicit request: LoggedRequest[AnyContent]): Result = {
    viewBuilder.buildLoggedView(view)(request, messages)
  }

  protected[controllers] def buildLoggedView(statusCode: Int, view: (User, Request[AnyContent], Messages) => Html)(implicit request: LoggedRequest[AnyContent]): Result = {
    viewBuilder.buildLoggedView(statusCode, view)(request, messages)
  }

  protected[controllers] def buildMaybeLoggedView(view: (Option[User], Request[AnyContent], Messages) => Html)(implicit request: MaybeLoggedRequest[AnyContent]): Result = {
    viewBuilder.buildMaybeLoggedView(view)(request, messages)
  }

  protected[controllers] def buildMaybeLoggedView(statusCode: Int, view: (Option[User], Request[AnyContent], Messages) => Html)(implicit request: MaybeLoggedRequest[AnyContent]): Result = {
    viewBuilder.buildMaybeLoggedView(statusCode, view)(request, messages)
  }

  val LoggedAction = { (userId: Long, requestType: RequestType) =>
    Action andThen Logged(userId, sessionService, requestType, userRepo, messages)
  }

  val MaybeLoggedAction = { (requestType: RequestType) =>
    Action andThen MaybeLogged(sessionService, requestType, userRepo, messages)
  }

}
