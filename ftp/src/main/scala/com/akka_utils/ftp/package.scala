package com.akka_utils

/**
  * @author Maksim Ochenashko
  */
package object ftp {

  trait FtpCredentials {
    def address: String

    def port: Int

    def login: Option[String]

    def password: Option[String]
  }

  case class Credentials(address: String, port: Int, login: Option[String], password: Option[String]) extends FtpCredentials

}
