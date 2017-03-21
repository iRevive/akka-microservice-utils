package com.akka_utils.slick.extensions

import slick.lifted.{ColumnOrdered, Query, Rep}

import scala.language.implicitConversions

/**
  * @author Maksim Ochenashko
  */
trait SlickQueryExtension extends ODataSupport {

  type OrderByFunction[X] = PartialFunction[String, X => ColumnOrdered[_]]

  type FilterFunction[X] = PartialFunction[String, X => Rep[Boolean]]

  implicit def query2tableQueryExt[A, B](query: Query[A, B, Seq]): TableQueryExt[A, B] = new TableQueryExt[A, B](query)

}

object SlickQueryExtension extends SlickQueryExtension

sealed trait ODataParam

case class Filter(param: String) extends ODataParam

case class OrderBy(param: String) extends ODataParam

case class Pagination(offset: Long, limit: Long)

case class ListQueryParams(filter: Option[Filter], orderBy: Option[OrderBy], pagination: Option[Pagination])
