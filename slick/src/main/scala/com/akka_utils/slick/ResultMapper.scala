package com.akka_utils.slick

import com.akka_utils.slick.errors._
import com.logless.source.Source

import scalaz._
import Scalaz._

/**
  * @author Maksim Ochenashko
  */
trait ResultMapper[A <: Operation, B, R] {

  def map(result: B)(implicit source: Source): SlickError \/ R

}

object ResultMapper {

  def apply[A <: Operation, B, R](implicit op: ResultMapper[A, B, R]): ResultMapper[A, B, R] =
    op

}

trait Operation

object Operation {

  sealed trait Exist extends Operation

  sealed trait NotExist extends Operation

  sealed trait Insert extends Operation

  sealed trait Delete extends Operation

  sealed trait Update extends Operation

  sealed trait Select extends Operation

  sealed trait Identity extends Operation

}

private[slick] trait ResultMapperInstances {

  import Operation._

  abstract class IntMapper[O <: Operation](error: Source => SlickError) extends ResultMapper[O, Int, Unit] {
    override def map(result: Int)(implicit source: Source): SlickError \/ Unit =
      (result != 0) either unitInstance.zero or error(source)
  }

  implicit object InsertMapper extends IntMapper[Insert](Conflict()(_))

  implicit object UpdateMapper extends IntMapper[Update](NotFound()(_))

  implicit object DeleteMapper extends IntMapper[Delete](DeleteError()(_))

  implicit object ExistMapper extends ResultMapper[Exist, Boolean, Unit] {
    override def map(result: Boolean)(implicit source: Source): SlickError \/ Unit =
      result either unitInstance.zero or NotExist()
  }

  implicit object NotExistMapper extends ResultMapper[NotExist, Boolean, Unit] {
    override def map(result: Boolean)(implicit source: Source): SlickError \/ Unit =
      !result either unitInstance.zero or Conflict()
  }

  implicit def singleResultMapper[A]: ResultMapper[Select, Option[A], A] = new ResultMapper[Select, Option[A], A] {
    override def map(result: Option[A])(implicit source: Source): SlickError \/ A =
      result \/> NotFound()
  }

  implicit def identityMapper[A]: ResultMapper[Identity, A, A] = new ResultMapper[Identity, A, A] {
    override def map(result: A)(implicit source: Source): SlickError \/ A =
      result.right
  }

}

private[slick] object ResultMapperInstances extends ResultMapperInstances