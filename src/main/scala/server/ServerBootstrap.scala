package server

import java.io.{File, FileOutputStream}

import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

/**
  * @author Maxim Ochenashko
  */
abstract class ServerBootstrap {

  private[this] val logger = LoggerFactory.getLogger(classOf[ServerBootstrap])

  def main(args: Array[String]): Unit = {
    val process = new ServerProcess(args)
    try {
      process.forceLazyValues()
      val system = process.system
      val pidFile = createPidFile(process)
      logger.info("PID file location: {}", pidFile.map(_.getAbsolutePath) getOrElse "undefined")

      process.addShutdownHook {
        system.terminate()
        pidFile foreach (_.delete)
      }

      try {
        start(process)
      } catch {
        case NonFatal(e) =>
          logger.error("'start' method produce an exception", e)
          throw e
      }
    } catch {
      case ServerStartException(message, cause) =>
        logger.error("Server startup exception", message)
        process.exit(message, cause)
      case NonFatal(e) =>
        process.exit("Oops, cannot start the server.", cause = Some(e))
    }
  }

  def start(process: ServerProcess): Unit

  private def createPidFile(process: ServerProcess): Option[File] = {
    val pidFilePath = process.serverConfig.pidLocation
    if (pidFilePath == "/dev/null") None else {
      val pidFile = new File(pidFilePath).getAbsoluteFile

      if (pidFile.exists) {
        throw ServerStartException(s"This application is already running (Or delete ${pidFile.getPath} file).")
      }

      val pid = process.pid getOrElse (throw ServerStartException("Couldn't determine current process's pid"))
      val out = new FileOutputStream(pidFile)
      try out.write(pid.getBytes) finally out.close()

      Some(pidFile)
    }
  }

}
