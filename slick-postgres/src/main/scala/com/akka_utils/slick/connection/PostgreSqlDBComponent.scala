package com.akka_utils.slick.connection

import com.akka_utils.slick.postgres.PostgresProfileExtended
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcBackend

/**
  * @author Maksim Ochenashko
  */
trait PostgreSqlDBComponent extends DBComponent[PostgresProfileExtended] {

  protected val profile: PostgresProfileExtended = PostgreSqlDB.dbConfig.profile

  import profile.api._

  protected lazy val db: Database = PostgreSqlDB.connectionPool

}

private[connection] object PostgreSqlDB  {

  val dbConfig: DatabaseConfig[PostgresProfileExtended] =
    DatabaseConfig.forConfig[PostgresProfileExtended]("slick.dbs.default")

  val connectionPool: JdbcBackend#DatabaseDef =
    dbConfig.db

}
