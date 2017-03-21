package com.akka_utils.slick.extensions

import com.akka_utils.slick.BaseRepositoryService.SlickResult
import com.akka_utils.slick.errors.{SlickError, SlickThrowableError}
import com.akka_utils.slick.{Operation, ResultMapper}
import com.logless.source.Source
import slick.jdbc.JdbcProfile
import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import scalaz._
import Scalaz._

/**
  * @author Maksim Ochenashko
  */
final class DBIOActionExt[R, +S <: NoStream, -E <: Effect](val action: DBIOAction[R, S, E]) extends AnyVal {

  def execute[Op <: Operation, Result](db: JdbcProfile#API#Database)(implicit c: ResultMapper[Op, R, Result], ctx: ExecutionContext, src: Source): SlickResult[Result] =
    EitherT apply db.run[R](action)
      .map(c.map)
      .recover[SlickError \/ Result] { case NonFatal(e) => SlickThrowableError(e.getMessage, Some(e)).left }

}