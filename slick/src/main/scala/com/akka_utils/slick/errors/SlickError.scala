package com.akka_utils.slick.errors

import com.akka_utils.errors.AbstractError
import com.logless.source.Source

/**
  * @author Maksim Ochenashko
  */
abstract class SlickError(message: String, cause: Option[Throwable] = None)(implicit source: Source) extends AbstractError(message, cause)

case class SlickThrowableError(message: String, cause: Option[Throwable] = None)(implicit source: Source) extends SlickError(message, cause)

case class NotFound(implicit source: Source) extends SlickError("Entity not found")

case class NotExist(implicit source: Source) extends SlickError("Entity not exists")

case class Conflict(implicit source: Source) extends SlickError("Entity already exists")

case class SaveError(implicit source: Source) extends SlickError("Entity save error")

case class UpdateError(implicit source: Source) extends SlickError("Entity update error")

case class DeleteError(implicit source: Source) extends SlickError("Entity delete error")