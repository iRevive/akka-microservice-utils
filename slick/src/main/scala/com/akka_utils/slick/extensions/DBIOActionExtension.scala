package com.akka_utils.slick.extensions

import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.language.implicitConversions

/**
  * @author Maksim Ochenashko
  */
trait DBIOActionExtension {

  implicit def action2actionExt[R, S <: NoStream, E <: Effect](action: DBIOAction[R, S, E]): DBIOActionExt[R, S, E] =
    new DBIOActionExt[R, S, E](action)

}
