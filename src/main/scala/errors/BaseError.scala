package errors

/**
  * @author Maxim Ochenashko
  */
abstract class BaseError(message: String, cause: Option[Throwable])

final case class GeneralError(message: String) extends BaseError(message, None)

final case class ThrowableError(message: String, cause: Option[Throwable]) extends BaseError(message, cause)


