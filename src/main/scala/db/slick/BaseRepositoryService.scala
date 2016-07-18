package db.slick

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import db.connection.PostgreSqlDBComponent
import db.slick.SlickServiceResults.{SlickMaybeError, SlickResult}
import slick.lifted.MappedProjection

import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import Scalaz._

/**
  * @author Maxim Ochenashko
  */
trait BaseRepositoryService extends PostgreSqlDBComponent with SlickServiceResults with LazyLogging {
  self =>

  import BaseRepositoryService._
  import driver.api._
  import extensions.SlickQueryExtension._
  import ext.futures.FutureExt._

  protected def executeList[A, B, R, X](query: Query[A, B, Seq],
                                        queryParams: ListQueryParams,
                                        f: A => MappedProjection[X, R],
                                        filterFunctionOpt: Option[FilterFunction[A]] = None,
                                        orderByFunctionOpt: Option[OrderByFunction[A]] = None)
                                       (implicit ec: ExecutionContext): Future[ListQueryResult[X]] = {
    val (sortedQuery, filteredQuery) = query.applyQueryParams(queryParams, filterFunctionOpt, orderByFunctionOpt)
    executeList0(sortedQuery.map(f), filteredQuery, queryParams)
  }


  protected def executeList0[A, B](sortedQuery: Query[A, B, Seq],
                                   countQuery: Query[_, _, Seq],
                                   queryParams: ListQueryParams)
                                  (implicit ec: ExecutionContext): Future[ListQueryResult[B]] = {
    executeList0[A, B, B](sortedQuery, countQuery, queryParams, identity)
  }

  protected def executeList0[A, B, X](sortedQuery: Query[A, B, Seq],
                                      countQuery: Query[_, _, Seq],
                                      queryParams: ListQueryParams,
                                      f: B => X)
                                     (implicit ec: ExecutionContext): Future[ListQueryResult[X]] = {
    val paginatedQuery = sortedQuery.pagination(queryParams.pagination)
    (for {
      (resultSet, total) <- db.run(paginatedQuery.result zip countQuery.length.result)
      p = queryParams.pagination
      offset = p.map(_.offset) | 0L
      limit = p.map(_.limit) | 0L
    } yield ListQueryResult(resultSet map f, offset, limit, total)) logError logger
  }

  protected def executeSingleResult[B](query: Query[_, B, Seq])
                                      (implicit ec: ExecutionContext): Future[SlickResult[B]] =
    db.run(query.firstOption) map checkSelect logError logger

  protected def executeExists[B](query: Query[_, B, Seq])
                                (implicit ec: ExecutionContext): Future[SlickMaybeError] =
    db.run(query.exists.result) map checkExists logError logger

  protected def executeNotExists[B](query: Query[_, B, Seq])
                                   (implicit ec: ExecutionContext): Future[SlickMaybeError] =
    db.run(query.exists.result) map checkNotExists logError logger

  protected def executeSave(query: DBIOAction[Int, NoStream, Nothing])
                           (implicit ec: ExecutionContext): Future[SlickMaybeError] =
    db.run(query) map checkInsert logError logger

  protected def executeUpdate(query: DBIOAction[Int, NoStream, Nothing])
                             (implicit ec: ExecutionContext): Future[SlickMaybeError] =
    db.run(query) map checkUpdate logError logger

  protected def executeDelete(query: DBIOAction[Int, NoStream, Nothing])
                             (implicit ec: ExecutionContext): Future[SlickMaybeError] =
    db.run(query) map checkDelete logError logger

  protected def newUuid = UUID.randomUUID()

}

object BaseRepositoryService {

  case class ListQueryResult[X](values: Seq[X], offset: Long, limit: Long, total: Long)

}
