package service.scheduler

import akka.actor.{Cancellable, ActorSystem}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * @author Maxim Ochenashko
  */
trait SchedulerTask extends LazyLogging {

  def initialDelay: FiniteDuration = Duration.Zero

  def interval: FiniteDuration

  def action(implicit ec: ExecutionContext): Future[Unit]

}

object Scheduler extends LazyLogging {

  def schedule(tasks: Seq[SchedulerTask])(implicit system: ActorSystem): Seq[Cancellable] = {
    import ext.futures.FutureExt._
    import system.dispatcher

    for (task <- tasks) yield {
      logger.info("Scheduling task: {}, initial delay: {}, interval: {}", task.getClass, task.initialDelay, task.interval)
      system.scheduler.schedule(task.initialDelay, task.interval, new Runnable {
        override def run(): Unit = task.action handleFailure { case (message, cause) =>
          logger.error(s"Task [${task.getClass.getSimpleName}] invocation error: $message", cause)
        }
      })
    }
  }

}
