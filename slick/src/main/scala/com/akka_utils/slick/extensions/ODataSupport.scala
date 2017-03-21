package com.akka_utils.slick.extensions

import SlickQueryExtension._
import slick.jdbc.JdbcProfile
import slick.lifted.Rep

/**
  * @author Maksim Ochenashko
  */
trait ODataSupport {

  private[this] val AndRegex = "(?<=(?:(?:\\)\\s)|(?:[\\w\\']\\s)))and"
  private[this] val OrRegex = "(?<=(?:[\\(\\)\\w\\']\\s))or"

  def buildFilter[T](oDataQuery: String, filterFn: FilterFunction[T])
                    (implicit profile: JdbcProfile): Option[(T) => Rep[Boolean]] = {
    import profile.api._

    val andExpressions = oDataQuery.split(AndRegex, -1).map { expr =>
      val orExpressions = expr.trim.split(OrRegex, -1)
        .map(_.trim)
        .map(filterFn.lift)
        .collect { case Some(x) => x }
        .toList

      foldExpressions[T, Boolean](orExpressions, (a, b) => a || b)
    } collect { case Some(x) => x }

    foldExpressions[T, Boolean](andExpressions.toList, (a, b) => a && b)
  }

  def foldExpressions[X, Z](expressions: Seq[(X) => Rep[Z]], op: (Rep[Z], Rep[Z]) => Rep[Z]): Option[X => Rep[Z]] =
    expressions match {
      case head :: tail => Some(tail.foldLeft(head)((left, right) => (x: X) => op(left(x), right(x))))
      case _            => None
    }

}

