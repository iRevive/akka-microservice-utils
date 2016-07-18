package db.connection

import slick.profile.BasicProfile

/**
  * @author Maxim Ochenashko
  */
trait DBComponent[P <: BasicProfile] {

  protected val driver: P

  import driver.api._

  protected val db: Database

}