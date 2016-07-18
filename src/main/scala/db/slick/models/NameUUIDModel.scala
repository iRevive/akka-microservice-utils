package db.slick.models

import slick.lifted.Rep

/**
  * @author Maxim Ochenashko
  */
trait NameUUIDModel extends UUIDModel {

  def name: Rep[String]

}
