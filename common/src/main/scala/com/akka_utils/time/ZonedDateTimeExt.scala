package com.akka_utils.time

import java.time.{LocalDateTime, ZonedDateTime}

/**
  * @author Maksim Ochenashko
  */
final class ZonedDateTimeExt(val time: ZonedDateTime) extends AnyVal {

  def >=(other: ZonedDateTime): Boolean = (time isAfter other) || (time isEqual other)

  def <=(other: ZonedDateTime): Boolean = (time isBefore other) || (time isEqual other)
}

final class LocalDateTimeExt(val time: LocalDateTime) extends AnyVal {

  def >=(other: LocalDateTime): Boolean = (time isAfter other) || (time isEqual other)

  def <=(other: LocalDateTime): Boolean = (time isBefore other) || (time isEqual other)
}
