package controllers.www

import common.auth.AuthValidator._
import common.actions.RequestType._
import common.FutureEither._
import common.ui.ViewBuilder
import models.user.UserRepo
import services.auth._
import controllers.AbstractController

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.i18n.Langs
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi


class HomeController(userRepo: UserRepo,
                     sessionService: SessionService,
                     viewBuilder: ViewBuilder,
                     langs: Langs, 
                     messages: MessagesApi,
                     cc: ControllerComponents) extends AbstractController(sessionService, userRepo, viewBuilder, cc, messages) with I18nSupport {

  def index = MaybeLoggedAction(Web) { implicit request =>
    buildMaybeLoggedView(views.html.www.home())
  }
}
