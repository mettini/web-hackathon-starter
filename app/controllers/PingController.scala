package controllers

import play.api.i18n.Langs
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents}
import play.twirl.api.Html

class PingController(langs: Langs, cc: ControllerComponents) extends play.api.mvc.AbstractController(cc) {

  def ping = Action {
    Ok
  }

}
