package com.akka_utils

import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}

import scala.language.implicitConversions

/**
  * @author Maksim Ochenashko
  */
package object time {

  def localDateTimeUTC: LocalDateTime = LocalDateTime now ZoneOffset.UTC

  def zonedDateTimeUTC: ZonedDateTime = ZonedDateTime now ZoneOffset.UTC

  def now: LocalDateTime = localDateTimeUTC

  implicit def zonedDateTime2zoneDateTimeExt(time: ZonedDateTime): ZonedDateTimeExt = new ZonedDateTimeExt(time)

  implicit def localDateTime2zoneDateTimeExt(time: LocalDateTime): LocalDateTimeExt = new LocalDateTimeExt(time)

}
