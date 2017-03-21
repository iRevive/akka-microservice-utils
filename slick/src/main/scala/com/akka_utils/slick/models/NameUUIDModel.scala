package com.akka_utils.slick.models

import slick.lifted.Rep

/**
  * @author Maksim Ochenashko
  */
trait NameUUIDModel extends UUIDModel {

  def name: Rep[String]

}
