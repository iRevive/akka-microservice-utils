package config

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.FiniteDuration
import scalaz._
import Scalaz._

/**
  * @author Maxim Ochenashko
  */
object Configuration {

  lazy val config = ConfigFactory.load()

  def getString(path: String): Option[String] = ifNotEmpty(path)(_.getString(path))

  def getInt(path: String): Option[Int] = ifNotEmpty(path)(_.getInt(path))

  def getDouble(path: String): Option[Double] = ifNotEmpty(path)(_.getDouble(path))

  def getDuration(path: String): Option[FiniteDuration] =
    ifNotEmpty(path) { cfg =>
      val value = cfg getDuration path
      FiniteDuration(value.toNanos, TimeUnit.NANOSECONDS)
    }

  private[this] def ifNotEmpty[X](path: String)(f: Config => X): Option[X] =
    (config hasPath path) option f(config)

}
