package models

import anorm.SqlParser._
import anorm._
import play.api.db.Database
import play.api.Logger
import java.sql.Connection

case class ListResult[A](count: Long, list: List[A])

trait BaseModel[T] {
  implicit val dateTimeFormatter = common.formatters.DateTimeFormatter.dateTimeFormatter

  def tableName: String
  def db: Database

  def parser: RowParser[T]

  protected def insert(arg: NamedParameter*)(implicit connection: Connection): Option[Long] = {
    val filteredArg = arg.filter{
      _.tupled match {
        case (_, pv: ParameterValue) => validatePv(pv)
        case _ => true
      }
    }
    val columnNames = " ( " ++ filteredArg.foldLeft("")((s,i) => s ++ (if(s == "") "" else ", \n") ++ i.tupled._1 ) ++ ", is_deleted) "
    val valueNames = " ( " ++ filteredArg.foldLeft("")((s,i) => s ++ (if(s == "") "" else ", \n") ++ "{" ++ i.tupled._1 ++ "}") ++ ", false ) "
    val insertQuery = s"insert into $tableName" ++ columnNames ++ "\n values " ++ valueNames
    SQL(insertQuery).on(filteredArg: _*).executeInsert()
  }

  private def validatePv(pv: ParameterValue): Boolean = {
    pv match {
      case dpv: DefaultParameterValue[_] =>
        dpv.value match {
          case None => false
          case _ => true
        }
      case _ => true
    }
  }

  def getById(id: Long): T = {
    Logger.trace(s"get by id #$id in table: $tableName")
    val selectQuery = s"SELECT * FROM $tableName WHERE id = {id} and is_deleted = false"
    db.withConnection { implicit connection =>
      SQL(selectQuery).on('id -> id).as(parser.single)
    }
  }

  def findById(id: Long): Option[T] = {
    Logger.trace(s"find by id #$id in table: $tableName")
    val selectQuery = s"SELECT * FROM $tableName WHERE id = {id} and is_deleted = false"
    db.withConnection { implicit connection =>
      SQL(selectQuery).on('id -> id).as(parser.singleOpt)
    }
  }

  def findByIdTransaction(id: Long)(implicit connection: Connection): Option[T] = {
    Logger.trace(s"find by id #$id in table: $tableName")
    SQL(s"""
      SELECT *
      FROM $tableName
      WHERE id = {id}
      AND is_deleted = false """).on('id -> id).as(parser.singleOpt)
  }

  def update(id: Long, o: Seq[NamedParameter]): Either[String, Int] = {
     db.withConnection { implicit connection =>
      val updateSQL = s" update $tableName set " + o.map(k => s"${k.name}={${k.name}}").mkString(",") + ", last_modification_date = NOW() where id = {id} "
      SQL(updateSQL).on(o :+ NamedParameter("id", id): _*).executeUpdate() match {
        case 1 => Right(1)
        case 0 => Left(s"there was an error updating the entity #$id of $tableName for parameters ${o.toString}")
      }
    }
  }

  def updateTransaction(id: Long, o: Seq[NamedParameter])(implicit connection: Connection): Either[String, Int] = {
    val updateSQL = s" update $tableName set " + o.map(k => s"${k.name}={${k.name}}").mkString(",") + ", last_modification_date = NOW() where id = {id} "
    SQL(updateSQL).on(o :+ NamedParameter("id", id): _*).executeUpdate() match {
      case 1 => Right(1)
      case 0 => Left(s"there was an error updating the entity #$id of $tableName for parameters ${o.toString}")
    }
  }

  def listAll: ListResult[T] = {
    Logger.trace(s"listing table: $tableName")
    val selectQuery = s"SELECT * FROM $tableName WHERE is_deleted = false"
    val countQuery = s"SELECT count(1) FROM $tableName WHERE is_deleted = false"
    db.withConnection { implicit connection =>
      val list = SQL(selectQuery).as(parser.*)
      val count = SQL(countQuery).as(scalar[Long].single)
      ListResult[T](count, list)
    }
  }

  def list(offset: Long, limit: Long): ListResult[T] = {
    Logger.trace(s"listing table: $tableName offset $offset limit $limit")
    val selectQuery = s"SELECT * FROM $tableName WHERE is_deleted = false LIMIT $offset, $limit"
    val countQuery = s"SELECT count(1) FROM $tableName WHERE is_deleted = false"
    db.withConnection { implicit connection =>
      val list = SQL(selectQuery).as(parser.*)
      val count = SQL(countQuery).as(scalar[Long].single)
      ListResult[T](count, list)
    }
  }

}
