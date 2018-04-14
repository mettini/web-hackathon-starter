package common.ui

import models.user.User
import models.admin.AdminUser

import play.twirl.api.Html
import common.actions.LoggedRequest
import common.actions.MaybeLoggedRequest
import common.actions.AdminRequest
import play.api.i18n.Messages.Implicits._
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import play.api.i18n.MessagesApi

import play.api.i18n._
import play.api.i18n.I18nSupport._


class ViewBuilder {

  def buildLoggedView[A](view: (User, Request[A], Messages) => Html)(implicit request: LoggedRequest[A], messages: MessagesApi): Result = {
    Ok(view(request.user, request, messages.preferred(request)))
  }

  def buildLoggedView[A](statusCode: Int, view: (User, Request[A], Messages) => Html)(implicit request: LoggedRequest[A], messages: MessagesApi): Result = {
    new Status(statusCode)(view(request.user, request, messages.preferred(request)))
  }

  def buildMaybeLoggedView[A](view: (Option[User], Request[A], Messages) => Html)(implicit request: MaybeLoggedRequest[A], messages: MessagesApi): Result = {
    Ok(view(request.user, request, messages.preferred(request)))
  }

  def buildMaybeLoggedView[A](statusCode: Int, view: (Option[User], Request[A], Messages) => Html)(implicit request: MaybeLoggedRequest[A], messages: MessagesApi): Result = {
    new Status(statusCode)(view(request.user, request, messages.preferred(request)))
  }

  def buildAdminView[A](view: (AdminUser, Request[A], Messages) => Html)(implicit request: AdminRequest[A], messages: MessagesApi): Result = {
    Ok(view(request.adminUser, request, messages.preferred(request)))
  }

  def buildAdminView[A](statusCode: Int, view: (AdminUser, Request[A], Messages) => Html)(implicit request: AdminRequest[A], messages: MessagesApi): Result = {
    new Status(statusCode)(view(request.adminUser, request, messages.preferred(request)))
  }


}
