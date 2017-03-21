package com.akka_utils.scheduler

import akka.actor.{ActorSystem, Cancellable}
import com.logless.LazyLogging

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
  * @author Maksim Ochenashko
  */
trait SchedulerTask {

  def initialDelay: FiniteDuration = Duration.Zero

  def interval: FiniteDuration

  def action(implicit ec: ExecutionContext): Future[Unit]

}

object Scheduler extends LazyLogging {

  def schedule(tasks: Seq[SchedulerTask])(implicit system: ActorSystem, ec: ExecutionContext): Seq[Cancellable] =
    for (task <- tasks) yield {
      sourceLogger.info("Scheduling task: {}, initial delay: {}, interval: {}", task.getClass, task.initialDelay, task.interval)
      system.scheduler.schedule(task.initialDelay, task.interval, new Runnable {
        override def run(): Unit = task.action onFailure { case NonFatal(e) =>
          sourceLogger.error(s"Task [${task.getClass.getSimpleName}] invocation error: ${e.getMessage}", e)
        }
      })
    }

}
