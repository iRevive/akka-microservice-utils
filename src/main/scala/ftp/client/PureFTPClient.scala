package ftp.client

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, Closeable, OutputStream}

import com.typesafe.scalalogging.LazyLogging
import ftp.client.PureFTPClient._
import ftp.errors._
import org.apache.commons.net.ftp.{FTP, FTPClient, FTPFile, FTPFileFilter}

import scalaz.EitherT.eitherT
import scalaz._
import scalaz.effect._
import Scalaz._
import scala.util.control.NonFatal

/**
  * @author Maxim Ochenashko
  */
final class PureFTPClient extends Closeable with LazyLogging {

  private val underlying: FTPClient = new FTPClient
  underlying.setConnectTimeout(5000)

  def setConnectionTimeout(timeout: Int): Unit =
    underlying.setConnectTimeout(timeout)

  def connect(host: String, port: Int): IOFtpMaybeError =
    safe {
      underlying.connect(host, port)
    }

  def login(login: String, password: String): IOFtpMaybeError =
    ifTrue(underlying.login(login, password), InvalidCredentials(login, password))

  def pwd: IOFtpResult[String] =
    safe {
      underlying.printWorkingDirectory
    }

  def cwd(path: String): IOFtpMaybeError =
    ifTrue(underlying.changeWorkingDirectory(path), UnableToChangeDirectory(path))

  def replyCode: Int =
    underlying.getReplyCode

  def enterLocalActiveMode: IOFtpMaybeError =
    safe(underlying.enterLocalActiveMode(), _ => EnterLocalActiveModeError)

  def setBinaryFileType(): IOFtpMaybeError =
    safe {
      underlying.setFileType(FTP.BINARY_FILE_TYPE)
    }

  def listNames: IOFtpResult[List[String]] =
    safe {
      underlying.listNames.toList
    }

  def listFiles: IOFtpResult[List[FTPFile]] =
    safe {
      underlying.listFiles.toList
    }

  def listFiles(path: String, filter: FTPFileFilter): IOFtpResult[List[FTPFile]] =
    safe {
      underlying.listFiles(path, filter).toList
    }

  def retrieveFile(path: String, output: OutputStream): IOFtpMaybeError =
    ifTrue(underlying.retrieveFile(path, output), RetrieveFileError(path))

  def loadFile(path: String): IOFtpResult[Array[Byte]] =
    IO(new ByteArrayOutputStream())
      .using { out =>
        (for {
          retrieveResult <- eitherT(ifTrue(underlying.retrieveFile(path, out), RetrieveFileError(path)))
        } yield out.toByteArray).run
      }(Resource.resourceFromCloseable[ByteArrayOutputStream])

  def loadFile(path: String, fileSize: Int): IOFtpResult[Array[Byte]] =
    IO(new ByteArrayOutputStream(fileSize)).using { out =>
      (for {
        retrieveResult <- eitherT(ifTrue(underlying.retrieveFile(path, out), RetrieveFileError(path)))
      } yield out.toByteArray).run
    }(Resource.resourceFromCloseable[ByteArrayOutputStream])

  def storeFile(fileInfo: FileInfo): IOFtpMaybeError =
    IO(new ByteArrayInputStream(fileInfo.content)).using { inputStream =>
      (for {
        _ <- eitherT(ifTrue(underlying.storeFile(fileInfo.name, inputStream), StoreFileError(fileInfo.name)))
      } yield ()).run
    }(Resource.resourceFromCloseable[ByteArrayInputStream])

  def delete(path: String): IOFtpMaybeError =
    ifTrue(underlying.deleteFile(path), DeleteFileError)

  def completePendingCommand: IOFtpMaybeError =
    safe {
      underlying.completePendingCommand()
    }

  def quit: IOFtpMaybeError =
    safe {
      underlying.quit()
    }

  def checkConnection: IOFtpMaybeError =
    ifTrue(underlying.isConnected, Disconnected)

  def dirExist(path: String): IOFtpMaybeError =
    ifTrue(underlying.changeWorkingDirectory(path) && underlying.getReplyCode != 550, DirectoryNotExist(path))

  def mkdir(path: String): IOFtpMaybeError =
    ifTrue(underlying.makeDirectory(path), DirectoryCreationError(path))

  private def ifTrue(f: => Boolean, left: => FtpError): IOFtpMaybeError =
    for {
      result <- safe(f)
    } yield result flatMap { bool => bool either unitInstance.zero or left }

  private def safe[X](f: => X): IOFtpResult[X] =
    safe(f, FtpThrowableError)

  private def safe[X](f: => X, left: Throwable => FtpError): IOFtpResult[X] =
    IO {
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

  type FtpResult[X] = FtpError \/ X
  type IOFtpResult[X] = IO[FtpResult[X]]
  type IOFtpMaybeError = IOFtpResult[Unit]

  val FtpSuccess = \/-
  val FtpFailure = -\/

  implicit val PureFtpClientMonad = new Monad[IO] {
    def point[A](a: => A): IO[A] = IO(a)

    def bind[A, B](fa: IO[A])(f: (A) => IO[B]): IO[B] = fa flatMap f
  }

  val ioFtpMonoid: Monoid[IOFtpMaybeError] = new Monoid[IOFtpMaybeError] {
    override def zero: IOFtpMaybeError = IO(unitInstance.zero.right)

    override def append(f1: IOFtpMaybeError, f2: => IOFtpMaybeError): IOFtpMaybeError =
      (for {
        _ <- eitherT(f1)
        _ <- eitherT(f2)
      } yield ()).run
  }

  /**
    * File representation
    *
    * @param name     file name with extension
    * @param content  file content
    */
  final case class FileInfo(name: String, content: Array[Byte])
}

