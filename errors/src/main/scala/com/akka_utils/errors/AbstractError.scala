package com.akka_utils.errors

import com.logless.{TraceIdentifier, TraceLogger}
import com.logless.source.Source

/**
  * @author Maksim Ochenashko
  */
abstract class AbstractError(message: String, cause: Option[Throwable])(implicit source: Source) {

  final def getMessage: String = message

  final def getCause: Option[Throwable] = cause

  final lazy val loggableMessage: String =
    source.enclosingMethod match {
      case Some(method) => s"${source.enclosingClass}.$method(...) - $message. $causeMessageOrEmpty"
      case None         => s"${source.enclosingClass} - $message. $causeMessageOrEmpty"
    }

  final lazy val causeMessage: Option[String] =
    for {
      c <- cause if c.getMessage != null
    } yield s"Cause: [${c.getMessage}]"

  private final def causeMessageOrEmpty: String = causeMessage getOrElse ""

  final def logError(logger: TraceLogger)(implicit tracer: TraceIdentifier): Unit =
    cause match {
      case Some(error) => logger.error(loggableMessage, error)
      case None        => logger.error(loggableMessage)
    }

}

case class ThrowableError(error: Throwable)(implicit source: Source) extends AbstractError(error.getMessage, Some(error))
