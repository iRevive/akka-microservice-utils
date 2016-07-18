package db.slick.driver

import com.github.tminglei.slickpg._
import io.circe.Json
import io.circe.jawn._
import slick.driver.PostgresDriver

/**
  * @author Maxim Ochenashko
  */
trait PostgresDriverExtended extends PostgresDriver
  with PgArraySupport
  with PgDate2Support
  with PgCirceJsonSupport {

  override def pgjson = "jsonb"

  override val api = MyAPI

  object MyAPI extends API with ArrayImplicits with DateTimeImplicits with JsonImplicits {
    implicit val circeJsonArrayTypeMapper = new AdvancedArrayJdbcType[Json](pgjson,
      (s) => utils.SimpleArrayUtils.fromString[Json](s => parse(s).getOrElse(Json.Null))(s).orNull,
      (v) => utils.SimpleArrayUtils.mkString[Json](_.noSpaces)(v)
    ).to(_.toList)
  }

}

object PostgresDriverExtended extends PostgresDriverExtended