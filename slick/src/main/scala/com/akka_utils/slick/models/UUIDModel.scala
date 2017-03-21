package com.akka_utils.slick.models

import java.util.UUID

import slick.lifted.Rep

/**
  * @author Maksim Ochenashko
  */
trait UUIDModel {

  def uuid: Rep[UUID]

}
