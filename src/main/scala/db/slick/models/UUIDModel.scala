package db.slick.models

import java.util.UUID

import slick.lifted.Rep

/**
  * @author Maxim Ochenashko
  */
trait UUIDModel {

  def uuid: Rep[UUID]

}
