package ext.futures

import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
  * @author Maxim Ochenashko
  */
object FutureExt {

  implicit class LoggableFuture[X](underlying: Future[X]) {

    def logInfo(logger: => Logger)(implicit ec: ExecutionContext): Future[X] =
      handleFailure { case (message, cause) => logger.error(message, cause) }

    def logError(logger: => Logger)(implicit ec: ExecutionContext): Future[X] =
      handleFailure { case (message, cause) => logger.error(message, cause) }

    def logWarn(logger: => Logger)(implicit ec: ExecutionContext): Future[X] =
      handleFailure { case (message, cause) => logger.warn(message, cause) }

    def logDebug(logger: => Logger)(implicit ec: ExecutionContext): Future[X] =
      handleFailure { case (message, cause) => logger.debug(message, cause) }

    //todo refactor method format
    def handleFailure(appender: (String, Throwable) => Unit)(implicit ec: ExecutionContext): Future[X] = {
      underlying onFailure { case NonFatal(e) => appender("Future execution error", e)}
      underlying
    }

  }

}
