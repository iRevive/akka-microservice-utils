package com.akka_utils.slick.postgres

import com.github.tminglei.slickpg._
import io.circe.Json
import io.circe.jawn._
import slick.jdbc.PostgresProfile

/**
  * @author Maksim Ochenashko
  */
trait PostgresProfileExtended extends PostgresProfile
  with PgArraySupport
  with PgDate2Support
  with PgCirceJsonSupport {

  override def pgjson = "jsonb"

  override val api = MyAPI

  object MyAPI extends API with ArrayImplicits with DateTimeImplicits with JsonImplicits {
    implicit val circeJsonArrayTypeMapper = new AdvancedArrayJdbcType[Json](
      pgjson,
      (s) => utils.SimpleArrayUtils.fromString[Json] { value =>
        parse(value) match {
          case Right(v) => v
          case Left(e) => Json.Null
        }
      }(s).orNull,
      (v) => utils.SimpleArrayUtils.mkString[Json](_.noSpaces)(v)
    ).to(_.toList)
  }

}

object PostgresProfileExtended extends PostgresProfileExtended