package com.akka_utils.server

import com.typesafe.config.Config

import scalaz.\/

/**
  * @author Maksim Ochenashko
  */
case class ServerConfiguration(pidLocation: String)

object ServerConfiguration {

  def fromConfig(config: Config): \/[Throwable, ServerConfiguration] =
    \/ fromTryCatchNonFatal {
      ServerConfiguration(
        config getString "com.akka_utils.server.pidfile.path"
      )
    }

}
