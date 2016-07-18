import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}

/**
  * @author Maxim Ochenashko
  */
package object time {

  def localDateTimeUTC = LocalDateTime now ZoneOffset.UTC

  def zonedDateTimeUTC = ZonedDateTime now ZoneOffset.UTC

  def now = localDateTimeUTC

  implicit class ZonedDateTimeExt(time: ZonedDateTime) {

    def >=(other: ZonedDateTime): Boolean = (time isAfter other) || (time isEqual other)

    def <=(other: ZonedDateTime): Boolean = (time isBefore other) || (time isEqual other)
  }

  implicit class LocalDateTimeExt(time: LocalDateTime) {

    def >=(other: LocalDateTime): Boolean = (time isAfter other) || (time isEqual other)

    def <=(other: LocalDateTime): Boolean = (time isBefore other) || (time isEqual other)
  }

}
