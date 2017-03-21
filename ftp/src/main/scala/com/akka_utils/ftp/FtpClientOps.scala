package com.akka_utils.ftp

import com.akka_utils.ftp.client.PureFTPClient
import com.akka_utils.ftp.client.PureFTPClient.IOFtpResult
import com.akka_utils.ftp.errors.UnableToChangeDirectory
import com.logless.source.Source
import org.apache.commons.net.ftp.{FTPFile, FTPFileFilter}

import scalaz._
import Scalaz._
import scalaz.effect.{IO, Resource}

/**
  * @author Maksim Ochenashko
  */
trait FtpClientOps {

  protected val Anonymous = "anonymous"

  protected def openConnection(client: PureFTPClient, c: FtpCredentials)(implicit src: Source): IOFtpResult[PureFTPClient] =
    for {
      _ <- client.connect(c.address, c.port)
      _ <- client.enterLocalActiveMode
      _ <- client.login(c.login | Anonymous, c.password | "")
      _ <- client.setBinaryFileType
    } yield client

  protected def listFilesFromRoot(client: PureFTPClient, path: String,
                                  filter: FTPFileFilter)(implicit src: Source): IOFtpResult[List[FTPFile]] =
    for {
      _ <- client.checkConnection
      _ <- client.cwd("/")
      files <- client.listFiles(path, filter)
    } yield files

  protected def createFtpDirectory(credentials: FtpCredentials, path: String)(implicit src: Source): IOFtpResult[Unit] =
    closeable[Unit] { client =>
      for {
        _ <- openConnection(client, credentials)
        _ <- changeCreateDir(client, path)
      } yield ()
    }

  protected def changeCreateDir(client: PureFTPClient, path: String)(implicit src: Source): IOFtpResult[Unit] =
    path.split("/")
      .map {
        case dir if dir == null || dir.isEmpty =>
          client cwd "/"
        case dir =>
          EitherT apply (client cwd dir).run.flatMap {
            case -\/(e: UnableToChangeDirectory) =>
              (for {
                _ <- client mkdir dir
                _ <- client cwd dir
              } yield ()).run

            case other =>
              IO(other)
          }
      }
      .reduce((a, b) => PureFTPClient.ioFtpMonoid.append(a, b))

  protected def closeable[X](f: PureFTPClient => IOFtpResult[X]): IOFtpResult[X] =
    EitherT apply IO(new PureFTPClient).using(c => f(c).run)(Resource.resourceFromCloseable[PureFTPClient])

}
