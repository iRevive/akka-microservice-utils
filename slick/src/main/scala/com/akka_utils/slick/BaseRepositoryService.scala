package com.akka_utils.slick

import com.akka_utils.slick.connection.DBComponent
import com.akka_utils.slick.errors._
import com.akka_utils.slick.extensions.{DBIOActionExtension, ListQueryParams, SlickQueryExtension}
import com.logless.source.Source
import slick.jdbc.JdbcProfile
import slick.lifted.MappedProjection

import scala.concurrent.{Future, ExecutionContext => EC}
import scalaz.Scalaz._
import scalaz.{EitherT, \/}

/**
  * @author Maksim Ochenashko
  */
trait BaseRepositoryService[P <: JdbcProfile] extends SlickQueryExtension with DBIOActionExtension with ResultMapperInstances {
  self: DBComponent[P] =>

  import BaseRepositoryService._
  import Operation._
  import profile.api._

  protected def executeList[A, B, R, X](query: Query[A, B, Seq],
                                        queryParams: ListQueryParams,
                                        f: A => MappedProjection[X, R],
                                        filterFunction: FilterFunction[A] = PartialFunction.empty,
                                        orderByFunction: OrderByFunction[A] = PartialFunction.empty)
                                       (implicit ec: EC, src: Source): SlickResult[ListQueryResult[X]] = {
    val (sortedQuery, filteredQuery) = query.applyQueryParams(queryParams, filterFunction, orderByFunction)
    list(sortedQuery.map(f), filteredQuery, queryParams)
  }

  protected def list[A, B](sortedQuery: Query[A, B, Seq],
                              countQuery: Query[_, _, Seq],
                              queryParams: ListQueryParams)
                             (implicit ec: EC, src: Source): SlickResult[ListQueryResult[B]] = {
    val paginatedQuery = queryParams.pagination.fold(sortedQuery)(sortedQuery.pagination)

    val query = paginatedQuery.result zip countQuery.length.result

    query.execute[Identity, (Seq[B], Int)](db) map { case (resultSet, total) =>
      val p = queryParams.pagination
      ListQueryResult(resultSet, p.map(_.offset) | 0L, p.map(_.limit) | 0L, total)
    }
  }

  protected final def singleResult[A](query: Query[_, A, Seq])(implicit ctx: EC, src: Source): SlickResult[A] =
    query.take(1).result.headOption.execute[Select, A](db)

  protected final def exists(query: Query[_, _, Seq])(implicit ctx: EC, src: Source): SlickMaybeError =
    query.exists.result.execute[Exist, Unit](db)

  protected final def notExist(query: Query[_, _, Seq])(implicit ctx: EC, src: Source): SlickMaybeError =
    query.exists.result.execute[NotExist, Unit](db)

  protected final def insert(action: DBIOAction[Int, NoStream, Nothing])(implicit ctx: EC, src: Source): SlickMaybeError =
    action2actionExt[Int, NoStream, Nothing](action).execute[Insert, Unit](db)

  protected final def update(action: DBIOAction[Int, NoStream, Nothing])(implicit ctx: EC, src: Source): SlickMaybeError =
    action2actionExt[Int, NoStream, Nothing](action).execute[Update, Unit](db)

  protected final def delete(action: DBIOAction[Int, NoStream, Nothing])(implicit ctx: EC, src: Source): SlickMaybeError =
    action2actionExt[Int, NoStream, Nothing](action).execute[Delete, Unit](db)

  /* private[this] def withCheck[A: ({type ß[x] = ResultChecker[Op, x, Result]})#ß, Result, Op <: Operation, S <: NoStream, E <: Effect](action: DBIOAction[A, S, E])(implicit ctx: EC, src: Source): SlickResult[Result] =
     EitherT apply db.run[A](action)
       .map(ResultChecker[Op, A, Result].checkResult)
       .recover[SlickError \/ Result] { case NonFatal(e) => SlickThrowableError(e.getMessage, Some(e)).left }

   private def execute[A](action: DBIOAction[A, NoStream, Nothing])
                         (implicit ctx: EC, src: Source): SlickResult[A] =
     EitherT apply db.run[A](action)
       .map(_.right)
       .recover[SlickError \/ A] { case NonFatal(e) => SlickThrowableError(e.getMessage, Some(e)).left }*/
}

object BaseRepositoryService {

  type SlickResult[A] = EitherT[Future, SlickError, A]

  type SlickMaybeError = SlickResult[Unit]

  case class ListQueryResult[X](values: Seq[X], offset: Long, limit: Long, total: Long)

}



