package db.slick.extensions

import db.slick.driver.PostgresDriverExtended
import slick.lifted.{ColumnOrdered, Query, Rep}

/**
  * @author Maxim Ochenashko
  */
object SlickQueryExtension extends ODataSupport {
  val driver = PostgresDriverExtended

  type OrderByFunction[X] = PartialFunction[String, X => ColumnOrdered[_]]

  type FilterFunction[X] = PartialFunction[String, X => Rep[Boolean]]

  sealed trait ODataParam

  final case class Filter(param: String) extends ODataParam

  final case class OrderBy(param: String) extends ODataParam

  case class Pagination(offset: Long, limit: Long)

  case class ListQueryParams(filter: Option[Filter], orderBy: Option[OrderBy], pagination: Option[Pagination])

  implicit class TableQueryExt[A, B](val query: Query[A, B, Seq]) extends AnyVal {
    import PostgresDriverExtended.api._

    def firstOption = query.take(1).result.headOption

    def pagination(pagination: Option[Pagination]) = pagination.fold(query) { case Pagination(offset, limit) =>
      query.drop(offset).take(limit)
    }

    def applyFilter(filterOpt: Option[Filter], filterFunction: Option[FilterFunction[A]]) = (for {
      filter <- filterOpt
      func <- filterFunction
      filterFunc <- buildFilter(filter.param, func)
      q = query.filter(filterFunc)
    } yield q) getOrElse query

    def applyOrderBy(orderByOpt: Option[OrderBy], orderByFunction: Option[OrderByFunction[A]]) = (for {
      orderBy <- orderByOpt
      func <- orderByFunction
      orderByFunc <- func.lift(orderBy.param)
      q = query.sortBy(orderByFunc)
    } yield q) getOrElse query

    def applyQueryParams(queryParams: ListQueryParams,
                         filterFunction: Option[FilterFunction[A]] = None,
                         orderByFunction: Option[OrderByFunction[A]] = None) = {
      val filteredQuery = applyFilter(queryParams.filter, filterFunction)
      val sortedQuery = filteredQuery.applyOrderBy(queryParams.orderBy, orderByFunction)

      sortedQuery -> filteredQuery
    }

  }

}
