package db.slick.extensions

import db.slick.extensions.SlickQueryExtension.FilterFunction
import slick.driver.JdbcProfile

/**
  * @author Maxim Ochenashko
  */
trait ODataSupport {
  self: {val driver: JdbcProfile} =>

  import driver.api._

  private[this] val andRegex = "(?<=(?:(?:\\)\\s)|(?:[\\w\\']\\s)))and"
  private[this] val orRegex = "(?<=(?:[\\(\\)\\w\\']\\s))or"

  def buildFilter[T](oDataQuery: String, filterFn: FilterFunction[T]) = {
    val andExpressions = oDataQuery.split(andRegex, -1).map { expr =>
      val orExpressions = expr.trim.split(orRegex, -1)
        .map(_.trim)
        .map(filterFn.lift)
        .collect { case Some(x) => x }
        .toList

      foldExpressions[T, Boolean](orExpressions, (a, b) => a || b)
    } collect { case Some(x) => x }
    foldExpressions[T, Boolean](andExpressions.toList, (a, b) => a && b)
  }

  def foldExpressions[X, Z](expressions: Iterable[(X) => Rep[Z]], op: (Rep[Z], Rep[Z]) => Rep[Z]): Option[X => Rep[Z]] =
    expressions match {
      case head :: Nil => Some(head)
      case Seq(head, tail@_*) => Some(tail.foldLeft(expressions.head)((left, right) => (x: X) => op(left(x), right(x))))
      case _ => None
    }

}

