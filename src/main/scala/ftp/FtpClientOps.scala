package ftp

import ftp.client.PureFTPClient
import ftp.client.PureFTPClient.{FtpResult, IOFtpResult}
import ftp.errors.UnableToChangeDirectory
import org.apache.commons.net.ftp.{FTPFile, FTPFileFilter}

import scalaz.-\/
import scalaz.EitherT._
import scalaz.Scalaz._
import scalaz.effect.{IO, Resource}

/**
  * @author Maxim Ochenashko
  */
trait FtpClientOps {

  protected val Anonymous = "anonymous"

  protected def openConnection(client: PureFTPClient, c: FtpCredentials): IOFtpResult[PureFTPClient] =
    (for {
      _ <- eitherT(client.connect(c.address, c.port))
      _ <- eitherT(client.enterLocalActiveMode)
      _ <- eitherT(client.login(c.login | Anonymous, c.password | ""))
      _ <- eitherT(client.setBinaryFileType())
    } yield client).run

  protected def listFilesFromRoot(client: PureFTPClient, path: String,
                                  filter: FTPFileFilter): IOFtpResult[List[FTPFile]] =
    (for {
      _ <- eitherT(client.checkConnection)
      _ <- eitherT(client.cwd("/"))
      files <- eitherT(client.listFiles(path, filter))
    } yield files).run

  protected def createFtpDirectory(credentials: FtpCredentials, path: String): IOFtpResult[Unit] =
    closeable[FtpResult[Unit]] { client =>
      (for {
        _ <- eitherT(openConnection(client, credentials))
        _ <- eitherT(changeCreateDir(client, path))
      } yield ()).run
    }

  protected def changeCreateDir(client: PureFTPClient, path: String): IOFtpResult[Unit] =
    path.split("/")
      .map {
        case dir if dir == null || dir.isEmpty =>
          client cwd "/"
        case dir =>
          (client cwd dir) flatMap {
            case -\/(e: UnableToChangeDirectory) =>
              (for {
                _ <- eitherT(client mkdir dir)
                _ <- eitherT(client cwd dir)
              } yield ()).run
            case other =>
              IO(other)
          }
      }
      .reduce((a, b) => PureFTPClient.ioFtpMonoid.append(a, b))

  protected def closeable[X](f: PureFTPClient => IO[X]): IO[X] =
    IO(new PureFTPClient).using(f)(Resource.resourceFromCloseable[PureFTPClient])

}
