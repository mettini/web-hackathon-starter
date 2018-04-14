package models.email

import com.github.nscala_time.time.Imports._

import anorm._
import anorm.SqlParser._
import play.api.db.Database

import play.api.libs.json._
import play.api.Logger
import models.BaseModel

case class EmailTemplate(
  id: Long,
  name: String,
  templateId: String,
  subject: String,
  bodyHtml: String,
  bodyText: String,
  isDeleted: Boolean,
  creationDate: DateTime,
  lastModificationDate: DateTime) {

  def replaceBody(bodyHtml: String, bodyText: String): EmailTemplate = {
    this.copy(bodyHtml = bodyHtml, bodyText = bodyText)
  }
}

class EmailTemplateRepo(val db: Database) extends BaseModel[EmailTemplate] {

  override def tableName: String = "sendgrid_email_templates"

  override def parser: RowParser[EmailTemplate] = {
    long("id") ~
    str("name") ~
    str("template_id") ~
    str("subject") ~
    str("body_html") ~
    str("body_text") ~
    bool("is_deleted") ~
    get[DateTime]("creation_date") ~
    get[DateTime]("last_modification_date") map { case
      id~
      name~
      templateId~
      subject~
      bodyHtml~
      bodyText~
      isDeleted~
      creationDate~
      lastModificationDate =>
      EmailTemplate(
        id,
        name,
        templateId,
        subject,
        bodyHtml,
        bodyText,
        isDeleted,
        creationDate,
        lastModificationDate
      )
    }
  }

  def findByName(name: String): Option[EmailTemplate] = {
    Logger.debug(s"EmailTemaplate findByName name: $name")
    db.withConnection { implicit connection =>
      SQL("""
        SELECT *
          FROM sendgrid_email_templates
          WHERE name = {name}
      """).on('name -> name).as(parser.singleOpt)
    }
  }

}

object EmailTemplateRepo {
  import common.formatters.DateTimeFormatter._
  implicit val emailTemplateFormat = Json.format[EmailTemplate]
}
