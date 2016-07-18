package db.slick.errors

import errors.BaseError

/**
  * @author Maxim Ochenashko
  */
abstract class SlickError(message: String, cause: Option[Throwable] = None) extends BaseError(message, cause)

case object NotFound extends SlickError("Entity not found")

case object NotExist extends SlickError("Entity not exists")

case object Conflict extends SlickError("Entity already exists")

case object SaveError extends SlickError("Entity save error")

case object UpdateError extends SlickError("Entity update error")

case object DeleteError extends SlickError("Entity delete error")