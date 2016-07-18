package server

import com.typesafe.config.Config

import scalaz.\/

/**
  * @author Maxim Ochenashko
  */
final case class ServerConfiguration(pidLocation: String)

object ServerConfiguration {

  def fromConfig(config: Config): \/[Throwable, ServerConfiguration] =
    \/ fromTryCatchNonFatal {
      ServerConfiguration(
        config getString "server.pidfile.path"
      )
    }

}
