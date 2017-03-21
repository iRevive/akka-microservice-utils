package com.akka_utils.ftp.client

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, Closeable, OutputStream}

import com.typesafe.scalalogging.LazyLogging
import com.akka_utils.ftp.client.PureFTPClient._
import com.akka_utils.ftp.errors._
import com.logless.source.Source
import org.apache.commons.net.ftp.{FTP, FTPClient, FTPFile, FTPFileFilter}

import scalaz._
import scalaz.effect._
import Scalaz._
import scala.util.control.NonFatal

/**
  * @author Maksim Ochenashko
  */
final class PureFTPClient(connectionTimeout: Int = 5000) extends Closeable with LazyLogging {

  private val underlying: FTPClient = new FTPClient
  underlying.setConnectTimeout(connectionTimeout)

  def setConnectionTimeout(timeout: Int): Unit =
    underlying.setConnectTimeout(timeout)

  def connect(host: String, port: Int)(implicit src: Source): IOFtpMaybeError =
    safe {
      underlying.connect(host, port)
    }

  def login(login: String, password: String)(implicit src: Source): IOFtpMaybeError =
    ifTrue(underlying.login(login, password), InvalidCredentials(login, password))

  def pwd(implicit src: Source): IOFtpResult[String] =
    safe {
      underlying.printWorkingDirectory
    }

  def cwd(path: String)(implicit src: Source): IOFtpMaybeError =
    ifTrue(underlying.changeWorkingDirectory(path), UnableToChangeDirectory(path))

  def replyCode: Int =
    underlying.getReplyCode

  def enterLocalActiveMode(implicit src: Source): IOFtpMaybeError =
    safe(underlying.enterLocalActiveMode(), _ => EnterLocalActiveModeError())

  def setBinaryFileType(implicit src: Source): IOFtpMaybeError =
    safe {
      underlying.setFileType(FTP.BINARY_FILE_TYPE)
    }

  def listNames(implicit src: Source): IOFtpResult[List[String]] =
    safe {
      underlying.listNames.toList
    }

  def listFiles(implicit src: Source): IOFtpResult[List[FTPFile]] =
    safe {
      underlying.listFiles.toList
    }

  def listFiles(path: String, filter: FTPFileFilter)(implicit src: Source): IOFtpResult[List[FTPFile]] =
    safe {
      underlying.listFiles(path, filter).toList
    }

  def retrieveFile(path: String, output: OutputStream)(implicit src: Source): IOFtpMaybeError =
    ifTrue(underlying.retrieveFile(path, output), RetrieveFileError(path))

  def loadFile(path: String)(implicit src: Source): IOFtpResult[Array[Byte]] =
    EitherT apply IO(new ByteArrayOutputStream())
      .using { out =>
        (for {
          _ <- ifTrue(underlying.retrieveFile(path, out), RetrieveFileError(path))
        } yield out.toByteArray).run
      }(Resource.resourceFromCloseable[ByteArrayOutputStream])

  def loadFile(path: String, fileSize: Int)(implicit src: Source): IOFtpResult[Array[Byte]] =
    EitherT apply IO(new ByteArrayOutputStream(fileSize)).using { out =>
      (for {
        _ <- ifTrue(underlying.retrieveFile(path, out), RetrieveFileError(path))
      } yield out.toByteArray).run
    }(Resource.resourceFromCloseable[ByteArrayOutputStream])

  def storeFile(fileInfo: FileInfo)(implicit src: Source): IOFtpMaybeError =
    EitherT apply IO(new ByteArrayInputStream(fileInfo.content)).using { inputStream =>
      ifTrue(underlying.storeFile(fileInfo.name, inputStream), StoreFileError(fileInfo.name)).run
    }(Resource.resourceFromCloseable[ByteArrayInputStream])

  def delete(path: String): IOFtpMaybeError =
    ifTrue(underlying.deleteFile(path), DeleteFileError())

  def completePendingCommand(implicit src: Source): IOFtpMaybeError =
    safe {
      underlying.completePendingCommand()
    }

  def quit(implicit src: Source): IOFtpMaybeError =
    safe {
      underlying.quit()
    }

  def checkConnection(implicit src: Source): IOFtpMaybeError =
    ifTrue(underlying.isConnected, Disconnected())

  def dirExist(path: String)(implicit src: Source): IOFtpMaybeError =
    ifTrue(underlying.changeWorkingDirectory(path) && underlying.getReplyCode != 550, DirectoryNotExist(path))

  def mkdir(path: String)(implicit src: Source): IOFtpMaybeError =
    ifTrue(underlying.makeDirectory(path), DirectoryCreationError(path))

  private def ifTrue(f: => Boolean, left: => FtpError)(implicit src: Source): IOFtpMaybeError =
    for {
      result <- safe(f)
    } yield result either unitInstance.zero or left

  private def safe[X](f: => X)(implicit src: Source): EitherT[IO, FtpError, X] =
    safe(f, FtpThrowableError(_))

  private def safe[X](f: => X, left: Throwable => FtpError)(implicit source: Source): EitherT[IO, FtpError, X] =
    EitherT apply IO {
      \/ fromTryCatchNonFatal f leftMap left
    }

  override def close(): Unit =
    if (underlying.isConnected) {
      try {
        underlying.quit()
        underlying.disconnect()
      } catch {
        case NonFatal(e) => logger.error("Failed to close ftp connection", e)
      }
    }
}

object PureFTPClient {

  type IOFtpResult[X] = EitherT[IO, FtpError, X]
  type IOFtpMaybeError = IOFtpResult[Unit]

  implicit val PureFtpClientMonad: Monad[IO] = new Monad[IO] {
    def point[A](a: => A): IO[A] = IO(a)

    def bind[A, B](fa: IO[A])(f: (A) => IO[B]): IO[B] = fa flatMap f
  }

  val ioFtpMonoid: Monoid[IOFtpMaybeError] = new Monoid[IOFtpMaybeError] {
    override def zero: IOFtpMaybeError = EitherT apply IO(unitInstance.zero.right)

    override def append(f1: IOFtpMaybeError, f2: => IOFtpMaybeError): IOFtpMaybeError =
      for {
        _ <- f1
        _ <- f2
      } yield ()
  }

}

/**
  * File representation
  *
  * @param name    file name with extension
  * @param content file content
  */
case class FileInfo(name: String, content: Array[Byte])
