package com.akka_utils.slick.connection

import slick.basic.BasicProfile

/**
  * @author Maksim Ochenashko
  */
trait DBComponent[P <: BasicProfile] {

  protected val profile: P

  import profile.api._

  protected val db: Database

}