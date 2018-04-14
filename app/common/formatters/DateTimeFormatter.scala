package common.formatters

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json.{JsString, JsSuccess, JsValue, Format}

object DateTimeFormatter {
  private lazy val ISODateTimeFormatter = ISODateTimeFormat.dateTime.withZone(DateTimeZone.UTC)
  private lazy val ISODateTimeParser = ISODateTimeFormat.dateTimeParser

  implicit val dateTimeFormatter = new Format[DateTime] {
    def reads(j: JsValue) = JsSuccess(ISODateTimeParser.parseDateTime(j.as[String]))
    def writes(o: DateTime): JsValue = JsString(ISODateTimeFormatter.print(o))
  }
}
