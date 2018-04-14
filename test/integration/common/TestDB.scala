package integration.common

import play.api.db.Databases
import play.api.db.evolutions.Evolutions

trait TestDB {

  implicit val testDatabase = Databases.inMemory(
    name = "default",
    urlOptions = Map(
      "MODE" -> "MYSQL",
      "DB_CLOSE_DELAY" -> "-1"
    ),
    config = Map(
      "logStatements" -> true
    )
  )

  /* With this line H2 doesn't repalce the NULL, so NOT NULL of MySQL can be tested */
  org.h2.engine.Mode.getInstance("MYSQL").convertInsertNullToZero = false
  Evolutions.applyEvolutions(testDatabase)
}
