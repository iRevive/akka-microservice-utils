package com.akka_utils.ftp.errors

import com.akka_utils.errors.AbstractError
import com.logless.source.Source

/**
  * @author Maksim Ochenashko
  */
sealed abstract class FtpError(message: String, cause: Option[Throwable] = None)(implicit source: Source) extends AbstractError(message, cause)

case class GenericError(message: String)(implicit source: Source) extends FtpError(message)

case class FtpThrowableError(t: Throwable)(implicit source: Source) extends FtpError(t.getMessage, Some(t))

case class InvalidCredentials(login: String, password: String)(implicit source: Source) extends FtpError(s"Invalid credentials. Login: $login, pwd: $password")

case class UnableToChangeDirectory(path: String)(implicit source: Source) extends FtpError(s"Unable to change directory. Path: $path")

case class EnterLocalActiveModeError(implicit source: Source) extends FtpError("EnterLocalActiveMode error")

case class RetrieveFileError(path: String)(implicit source: Source) extends FtpError(s"Retrieve file stream error. Path: $path")

case class StoreFileError(path: String)(implicit source: Source) extends FtpError(s"Store file error. Path: $path")

case class Disconnected(implicit source: Source) extends FtpError("Client already disconnected")

case class DeleteFileError(implicit source: Source) extends FtpError("Delete file error")

case class FileNotFound(implicit source: Source) extends FtpError("File not found")

case class DirectoryCreationError(path: String)(implicit source: Source) extends FtpError(s"Directory creation error. Path: $path")

case class DirectoryNotExist(path: String)(implicit source: Source) extends FtpError(s"Directory not exist. Path: $path")