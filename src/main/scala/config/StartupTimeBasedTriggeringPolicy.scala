package config

import java.io.File

import ch.qos.logback.core.joran.spi.NoAutoStart
import ch.qos.logback.core.rolling.{DefaultTimeBasedFileNamingAndTriggeringPolicy, RolloverFailure}

/**
  * Creates new file every app start
  *
  * @author Maxim Ochenashko
  */
@NoAutoStart
class StartupTimeBasedTriggeringPolicy[X] extends DefaultTimeBasedFileNamingAndTriggeringPolicy[X] {

  override def start(): Unit = {
    super.start()
    nextCheck = 0L
    isTriggeringEvent(null.asInstanceOf[File], null.asInstanceOf[X])
    try {
      tbrp.rollover()
    } catch {
      case _: RolloverFailure =>
    }
  }

}
