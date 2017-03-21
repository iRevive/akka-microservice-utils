package com.akka_utils.slick.extensions

import com.akka_utils.slick.extensions.SlickQueryExtension._
import slick.jdbc.JdbcProfile
import slick.lifted.Query

/**
  * @author Maksim Ochenashko
  */
final class TableQueryExt[A, B](val query: Query[A, B, Seq]) extends AnyVal {

  def pagination(pagination: Pagination): Query[A, B, Seq] =
    query.drop(pagination.offset).take(pagination.limit)

  def applyFilter(filter: Filter, filterFunction: FilterFunction[A])(implicit p: JdbcProfile): Query[A, B, Seq] =
    buildFilter(filter.param, filterFunction).fold(query)(query.filter)

  def applyOrderBy(orderBy: OrderBy, orderByFunction: OrderByFunction[A])(implicit p: JdbcProfile): Query[A, B, Seq] =
    orderByFunction.lift(orderBy.param).fold(query)(v => query.sortBy(v))

  def applyQueryParams(queryParams: ListQueryParams,
                       filterFunction: FilterFunction[A] = PartialFunction.empty,
                       orderByFunction: OrderByFunction[A] = PartialFunction.empty)
                      (implicit p: JdbcProfile): (Query[A, B, Seq], Query[A, B, Seq]) = {
    val filteredQuery = queryParams.filter.fold(query)(applyFilter(_, filterFunction))
    val sortedQuery = queryParams.orderBy.fold(filteredQuery)(applyOrderBy(_, orderByFunction))

    sortedQuery -> filteredQuery
  }

}