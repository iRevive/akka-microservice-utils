package db.slick

import db.slick.SlickServiceResults.{SlickMaybeError, SlickResult}
import db.slick.errors._

import scalaz._
import Scalaz._

/**
  * @author Maxim Ochenashko
  */
trait SlickServiceResults {

  protected def checkExists(exists: Boolean): SlickMaybeError =
    exists either unitInstance.zero or NotExist

  protected def checkNotExists(exists: Boolean): SlickMaybeError =
    !exists either unitInstance.zero or Conflict

  protected def checkInsert(rowsAffected: Int): SlickMaybeError =
    check(rowsAffected, SaveError)

  protected def checkUpdate(rowsAffected: Int): SlickMaybeError =
    check(rowsAffected, NotFound)

  protected def checkDelete(rowsAffected: Int): SlickMaybeError =
    check(rowsAffected, DeleteError)

  protected def checkSelect[T](opt: Option[T]): SlickResult[T] =
    opt \/> NotFound

  private def check(rowsAffected: Int, badResult: SlickError): SlickMaybeError =
    (rowsAffected != 0) either unitInstance.zero or badResult

}

object SlickServiceResults {

  type SlickResult[Result] = \/[SlickError, Result]

  type SlickMaybeError = \/[SlickError, Unit]

}
