package com.akka_utils.server

import java.lang.management.ManagementFactory
import java.util.Properties

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.akka_utils.config.Configuration

import scalaz.{-\/, \/-}

/**
  * @author Maksim Ochenashko
  */
class ServerProcess(val args: Seq[String]) {

  lazy val system = ActorSystem("application")

  lazy val materializer = ActorMaterializer()(system)

  private[server] lazy val serverConfig = {
    ServerConfiguration.fromConfig(Configuration.config) match {
      case -\/(e) =>
        throw ServerStartException("Server configuration loading error", Some(e))
      case \/-(config) =>
        config
    }
  }

  def properties: Properties = System.getProperties

  def pid: Option[String] =
    ManagementFactory.getRuntimeMXBean.getName.split('@').headOption

  def addShutdownHook(hook: => Unit): Unit =
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run(): Unit = hook
    })

  def exit(message: String, cause: Option[Throwable] = None, returnCode: Int = -1): Nothing = {
    System.err.println(message)
    cause.foreach(_.printStackTrace())
    System.exit(returnCode)
    // Code never reached, but throw an exception to give a type of Nothing
    throw new Exception("SystemProcess.exit called")
  }

  private[server] def forceLazyValues() = {
    serverConfig
    system
    materializer
  }

}