package db.connection

import db.slick.driver.PostgresDriverExtended
import slick.backend.DatabaseConfig

/**
  * @author Maxim Ochenashko
  */
trait PostgreSqlDBComponent extends DBComponent[PostgresDriverExtended] {

  protected val driver: PostgresDriverExtended = PostgreSqlDB.dbConfig.driver

  import driver.api._

  protected lazy val db: Database = PostgreSqlDB.connectionPool

}

private[connection] object PostgreSqlDB  {
  val dbConfig = DatabaseConfig.forConfig[PostgresDriverExtended]("slick.dbs.default")

  val connectionPool = dbConfig.db
}
