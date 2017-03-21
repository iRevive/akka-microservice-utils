package com.akka_utils.server

/**
  * @author Maksim Ochenashko
  */
case class ServerStartException(message: String, cause: Option[Throwable] = None) extends Exception(message, cause.orNull)