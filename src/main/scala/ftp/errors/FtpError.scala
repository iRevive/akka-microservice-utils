package ftp.errors

import errors.BaseError

/**
  * @author Maxim Ochenashko
  */
sealed abstract class FtpError(message: String, cause: Option[Throwable] = None) extends BaseError(message, cause)

final case class GenericError(message: String) extends FtpError(message)

final case class FtpThrowableError(t: Throwable) extends FtpError(t.getMessage, Some(t))

final case class InvalidCredentials(login: String, password: String) extends FtpError(s"Invalid credentials. Login: $login, pwd: $password")

final case class UnableToChangeDirectory(path: String) extends FtpError(s"Unable to change directory. Path: $path")

case object EnterLocalActiveModeError extends FtpError("EnterLocalActiveMode error")

final case class RetrieveFileError(path: String) extends FtpError(s"Retrieve file stream error. Path: $path")

final case class StoreFileError(path: String) extends FtpError(s"Store file error. Path: $path")

case object Disconnected extends FtpError("Client already disconnected")

case object DeleteFileError extends FtpError("Delete file error")

case object FileNotFound extends FtpError("File not found")

final case class DirectoryCreationError(path: String) extends FtpError(s"Directory creation error. Path: $path")

final case class DirectoryNotExist(path: String) extends FtpError(s"Directory not exist. Path: $path")