/*
 * Copyright (c) 2014-2016 by its authors. Some rights reserved.
 * See the project homepage at: https://monix.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monix.execution.schedulers

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent._

import monix.execution.UncaughtExceptionReporter._
import monix.execution.{Scheduler, SchedulerCompanion, UncaughtExceptionReporter}
import monix.execution.internal.ForkJoinPool

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/** @define applyDesc The resulting [[Scheduler]] will piggyback on top of a Java
  *         `ScheduledExecutorService` for scheduling tasks for execution with
  *         a delay and a Scala `ExecutionContext` for actually executing the
  *         tasks.
  *
  * @define executorService is the `ScheduledExecutorService` that handles the
  *         scheduling of tasks into the future. If not provided, an internal
  *         default will be used. You can also create one with
  *         `java.util.concurrent.Executors`.
  *
  * @define executionContext is the execution context in which all tasks will run.
  *         Use `scala.concurrent.ExecutionContext.Implicits.global`
  *         for the default.
  *
  * @define executionModel is the preferred
  *         [[monix.execution.schedulers.ExecutionModel ExecutionModel]],
  *         a guideline for run-loops and producers of data. Use
  *         [[monix.execution.schedulers.ExecutionModel.Default ExecutionModel.Default]]
  *         for the default.
  *
  * @define reporter is the [[UncaughtExceptionReporter]] that logs uncaught exceptions.
  *         Wrap the given `ExecutionContext.reportFailure` or use
  *         [[UncaughtExceptionReporter.LogExceptionsToStandardErr]] for
  *         the default.
  */
private[execution] class SchedulerCompanionImpl extends SchedulerCompanion {
  /** [[monix.execution.Scheduler Scheduler]] builder.
    *
    * The resulting [[Scheduler]] will piggyback on top of a Java
    * `ScheduledExecutorService` for scheduling tasks for execution with
    * a delay and a Scala `ExecutionContext` for actually executing the
    * tasks.
    *
    * @param executor $executorService
    * @param ec $executionContext
    * @param reporter $reporter
    * @param executionModel $executionModel
    */
  def apply(
    executor: ScheduledExecutorService,
    ec: ExecutionContext,
    reporter: UncaughtExceptionReporter,
    executionModel: ExecutionModel): Scheduler =
    AsyncScheduler(executor, ec, reporter, executionModel)

  /** [[monix.execution.Scheduler Scheduler]] builder.
    *
    * @param executor $executorService
    * @param ec $executionContext
    */
  def apply(executor: ScheduledExecutorService, ec: ExecutionContext): Scheduler =
    AsyncScheduler(executor, ec, UncaughtExceptionReporter(ec.reportFailure), ExecutionModel.Default)

  /** [[monix.execution.Scheduler Scheduler]] builder.
    *
    * @param ec $executionContext
    * @param reporter $reporter
    */
  def apply(ec: ExecutionContext, reporter: UncaughtExceptionReporter): Scheduler =
    AsyncScheduler(DefaultScheduledExecutor, ec, reporter, ExecutionModel.Default)

  /** [[monix.execution.Scheduler Scheduler]] builder .
    *
    * @param ec $executionContext
    * @param reporter $reporter
    * @param executionModel $executionModel
    */
  def apply(ec: ExecutionContext, reporter: UncaughtExceptionReporter, executionModel: ExecutionModel): Scheduler =
    AsyncScheduler(DefaultScheduledExecutor, ec, reporter, executionModel)

  /** [[monix.execution.Scheduler Scheduler]] builder that converts a Java `ScheduledExecutorService` into
    * a scheduler.
    *
    * @param executor $executorService
    * @param reporter $reporter
    */
  def apply(executor: ScheduledExecutorService, reporter: UncaughtExceptionReporter): Scheduler =
    ExecutorScheduler(executor, reporter, ExecutionModel.Default)

  /** [[monix.execution.Scheduler Scheduler]] builder that converts a Java `ScheduledExecutorService` into
    * a scheduler.
    *
    * @param executor $executorService
    * @param reporter $reporter
    * @param executionModel $executionModel
    */
  def apply(executor: ScheduledExecutorService, reporter: UncaughtExceptionReporter, executionModel: ExecutionModel): Scheduler =
    ExecutorScheduler(executor, reporter, executionModel)

  /** [[monix.execution.Scheduler Scheduler]] builder that converts a Java `ScheduledExecutorService` into
    * a scheduler.
    *
    * @param executor $executorService
    */
  def apply(executor: ScheduledExecutorService): Scheduler =
    ExecutorScheduler(executor, LogExceptionsToStandardErr, ExecutionModel.Default)

  /** [[monix.execution.Scheduler Scheduler]] builder that converts a Java `ScheduledExecutorService` into
    * a scheduler.
    *
    * @param executor $executorService
    * @param executionModel $executionModel
    */
  def apply(executor: ScheduledExecutorService, executionModel: ExecutionModel): Scheduler =
    ExecutorScheduler(executor, LogExceptionsToStandardErr, executionModel)

  /** [[monix.execution.Scheduler Scheduler]] builder - uses monix's
    * default `ScheduledExecutorService` for handling the scheduling of tasks.
    *
    * @param ec is the execution context in which all tasks will run.
    */
  def apply(ec: ExecutionContext): Scheduler =
    AsyncScheduler(
      DefaultScheduledExecutor, ec,
      UncaughtExceptionReporter(ec.reportFailure),
      ExecutionModel.Default
    )

  /** [[monix.execution.Scheduler Scheduler]] builder - uses monix's
    * default `ScheduledExecutorService` for handling the scheduling of tasks.
    *
    * @param ec is the execution context in which all tasks will run.
    * @param executionModel $executionModel
    */
  def apply(ec: ExecutionContext, executionModel: ExecutionModel): Scheduler =
    AsyncScheduler(
      DefaultScheduledExecutor, ec,
      UncaughtExceptionReporter(ec.reportFailure),
      executionModel
    )

  /** [[monix.execution.Scheduler Scheduler]] builder - uses monix's
    * default `ScheduledExecutorService` for handling the scheduling of tasks.
    * Uses Scala's `s.c.ExecutionContext.global` for actual execution.
    *
    * @param reporter $reporter
    * @param executionModel $executionModel
    */
  def apply(reporter: UncaughtExceptionReporter, executionModel: ExecutionModel): Scheduler = {
    val ec = ExecutionContext.Implicits.global
    AsyncScheduler(DefaultScheduledExecutor, ec, reporter, executionModel)
  }

  /** [[monix.execution.Scheduler Scheduler]] builder - uses monix's
    * default `ScheduledExecutorService` for handling the scheduling of tasks.
    * Uses Scala's `s.c.ExecutionContext.global` for actual execution.
    *
    * @param executionModel $executionModel
    */
  def apply(executionModel: ExecutionModel): Scheduler = {
    val ec = ExecutionContext.Implicits.global
    AsyncScheduler(
      DefaultScheduledExecutor, ec,
      UncaughtExceptionReporter(ec.reportFailure),
      executionModel
    )
  }

  /** Builds a [[monix.execution.schedulers.TrampolineScheduler TrampolineScheduler]].
    *
    * @param underlying is the [[monix.execution.Scheduler Scheduler]]
    *        to which the we defer to in case asynchronous or time-delayed
    *        execution is needed
    *
    * @define executionModel is the preferred
    *         [[monix.execution.schedulers.ExecutionModel ExecutionModel]],
    *         a guideline for run-loops and producers of data. Use
    *         [[monix.execution.schedulers.ExecutionModel.Default ExecutionModel.Default]]
    *         for the default.
    */
  def trampoline(
    underlying: Scheduler = Implicits.global,
    executionModel: ExecutionModel = ExecutionModel.Default): Scheduler =
    TrampolineScheduler(underlying, executionModel)

  /** Creates a [[monix.execution.Scheduler Scheduler]] meant for computational heavy tasks.
    *
    * Characteristics:
    *
    * - backed by Scala's `ForkJoinPool` for the task execution, in async mode
    * - uses monix's default `ScheduledExecutorService` instance for scheduling
    * - all created threads are daemonic
    * - cooperates with Scala's `BlockContext`
    *
    * @param parallelism is the number of threads that can run in parallel
    * @param reporter $reporter
    * @param executionModel $executionModel
    */
  def computation(parallelism: Int,
    reporter: UncaughtExceptionReporter = LogExceptionsToStandardErr,
    executionModel: ExecutionModel = ExecutionModel.Default): Scheduler = {
    val exceptionHandler = new UncaughtExceptionHandler {
      def uncaughtException(t: Thread, e: Throwable) =
        reporter.reportFailure(e)
    }

    val pool = ForkJoinPool(
      parallelism,
      ForkJoinPool.defaultForkJoinWorkerThreadFactory,
      exceptionHandler,
      asyncMode = true
    )

    val context = ExecutionContext.fromExecutor(pool, reporter.reportFailure)
    AsyncScheduler(DefaultScheduledExecutor, context, reporter, executionModel)
  }

  /** Creates a [[monix.execution.Scheduler Scheduler]] meant for blocking I/O tasks.
    *
    * Characteristics:
    *
    * - backed by a cached `ThreadPool` executor with 60 seconds of keep-alive
    * - the maximum number of threads is unbounded, as recommended for blocking I/O
    * - uses monix's default `ScheduledExecutorService` instance for scheduling
    * - doesn't cooperate with Scala's `BlockContext` because it is unbounded
    *
    * @param name the created threads name prefix, for easy identification.
    * @param daemonic specifies whether the created threads should be daemonic
    *        (non-daemonic threads are blocking the JVM process on exit).
    * @param reporter $reporter
    * @param executionModel $executionModel
    */
  def io(name: String = "monix-io", daemonic: Boolean = true,
    reporter: UncaughtExceptionReporter = LogExceptionsToStandardErr,
    executionModel: ExecutionModel = ExecutionModel.Default): Scheduler = {
    val threadFactory = ThreadFactoryBuilder(name, reporter, daemonic)

    val context = ExecutionContext.fromExecutor(
      Executors.newCachedThreadPool(threadFactory),
      reporter.reportFailure
    )

    AsyncScheduler(DefaultScheduledExecutor, context, reporter, executionModel)
  }

  /** Builds a [[Scheduler]] backed by an internal
    * `java.util.concurrent.ThreadPoolExecutor`, that executes each submitted
    * task using one of possibly several pooled threads.
    *
    * @param name the created threads name prefix, for easy identification
    * @param minThreads the number of threads to keep in the pool, even
    *        if they are idle
    * @param maxThreads the maximum number of threads to allow in the pool
    * @param keepAliveTime when the number of threads is greater than
    *        the core, this is the maximum time that excess idle threads
    *        will wait for new tasks before terminating
    * @param daemonic specifies whether the created threads should be daemonic
    *        (non-daemonic threads are blocking the JVM process on exit).
    * @param reporter $reporter
    * @param executionModel $executionModel
    */
  def cached(
    name: String,
    minThreads: Int,
    maxThreads: Int,
    keepAliveTime: FiniteDuration = 60.seconds,
    daemonic: Boolean = true,
    reporter: UncaughtExceptionReporter = LogExceptionsToStandardErr,
    executionModel: ExecutionModel = ExecutionModel.Default): Scheduler = {

    require(minThreads >= 0, "minThreads >= 0")
    require(maxThreads > 0, "maxThreads > 0")
    require(maxThreads >= minThreads, "maxThreads >= minThreads")
    require(keepAliveTime >= Duration.Zero, "keepAliveTime >= 0")

    val threadFactory = ThreadFactoryBuilder(name, reporter, daemonic)
    val executor = new ThreadPoolExecutor(
      minThreads, maxThreads,
      keepAliveTime.toMillis, TimeUnit.MILLISECONDS,
      new SynchronousQueue[Runnable](false),
      threadFactory)

    val context = ExecutionContext.fromExecutor(executor, reporter.reportFailure)
    AsyncScheduler(DefaultScheduledExecutor, context, reporter, executionModel)
  }

  /** Builds a [[monix.execution.Scheduler Scheduler]] that schedules and executes tasks on its own thread.
    *
    * Characteristics:
    *
    * - backed by a single-threaded `ScheduledExecutorService` that takes care
    *   of both scheduling tasks in the future and of executing tasks
    * - does not cooperate with Scala's `BlockingContext`, so tasks should not
    *   block on the result of other tasks scheduled to run on this same thread
    *
    * @param name is the name of the created thread, for easy identification
    * @param daemonic specifies whether the created thread should be daemonic
    *                 (non-daemonic threads are blocking the JVM process on exit)
    * @param reporter $reporter
    * @param executionModel $executionModel
    */
  def singleThread(name: String, daemonic: Boolean = true,
    reporter: UncaughtExceptionReporter = LogExceptionsToStandardErr,
    executionModel: ExecutionModel = ExecutionModel.Default): Scheduler = {

    val executor =
      Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder(name, reporter, daemonic))

    val context = new ExecutionContext {
      def reportFailure(t: Throwable) = reporter.reportFailure(t)
      def execute(runnable: Runnable) = executor.execute(runnable)
    }

    AsyncScheduler(executor, context, reporter, executionModel)
  }

  /** Builds a [[monix.execution.Scheduler Scheduler]] with a fixed thread-pool.
    *
    * Characteristics:
    *
    * - backed by a fixed pool `ScheduledExecutorService` that takes care
    *   of both scheduling tasks in the future and of executing immediate tasks
    * - does not cooperate with Scala's `BlockingContext`, so tasks should not
    *   block on the result of other tasks scheduled to run on this same thread
    *
    * @param name the created threads name prefix, for easy identification.
    * @param daemonic specifies whether the created thread should be daemonic
    * @param reporter is the [[UncaughtExceptionReporter]] that logs uncaught exceptions.
    */
  def fixedPool(name: String, poolSize: Int, daemonic: Boolean = true,
    reporter: UncaughtExceptionReporter = LogExceptionsToStandardErr,
    executionModel: ExecutionModel = ExecutionModel.Default): Scheduler = {

    val executor = Executors.newScheduledThreadPool(
      poolSize, ThreadFactoryBuilder(name, reporter, daemonic))

    ExecutorScheduler(executor, reporter, executionModel)
  }

  /** The default `ScheduledExecutor` instance.
    *
    * Currently it's a single-threaded Java `ScheduledExecutorService`
    * used for scheduling delayed tasks for execution. But the actual
    * execution will not happen on this executor service. In general
    * you can just reuse this one for all your scheduling needs.
    */
  lazy val DefaultScheduledExecutor: ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor(
      ThreadFactoryBuilder("monix-scheduler", LogExceptionsToStandardErr))

  /** The explicit global `Scheduler`. Invoke `global` when you want to provide the global
    * `Scheduler` explicitly.
    *
    * The default `Scheduler` implementation is backed by a work-stealing thread pool, along
    * with a single-threaded `ScheduledExecutionContext` that does the scheduling. By default,
    * the thread pool uses a target number of worker threads equal to the number of
    * [[https://docs.oracle.com/javase/8/docs/api/java/lang/Runtime.html#availableProcessors-- available processors]].
    *
    * @return the global `Scheduler`
    */
  def global: Scheduler = Implicits.global

  object Implicits extends ImplicitsLike {
    /** A global [[monix.execution.Scheduler Scheduler]] instance, provided for convenience, piggy-backing
      * on top of Scala's own `concurrent.ExecutionContext.global`, which is a
      * `ForkJoinPool`.
      *
      * It can be tuned by setting the following JVM system properties:
      *
      * - "scala.concurrent.context.minThreads" an integer specifying the minimum
      *   number of active threads in the pool
      *
      * - "scala.concurrent.context.maxThreads" an integer specifying the maximum
      *   number of active threads in the pool
      *
      * - "scala.concurrent.context.numThreads" can be either an integer,
      *   specifying the parallelism directly or a string with the format "xNUM"
      *   (e.g. "x1.5") specifying the multiplication factor of the number of
      *   available processors (taken with `Runtime.availableProcessors`)
      *
      * The formula for calculating the parallelism in our pool is
      * `min(maxThreads, max(minThreads, numThreads))`.
      *
      * To set a system property from the command line, a JVM parameter must be
      * given to the `java` command as `-Dname=value`. So as an example, to customize
      * this global scheduler, we could start our process like this:
      *
      * <pre>
      *   java -Dscala.concurrent.context.minThreads=2 \
      *        -Dscala.concurrent.context.maxThreads=30 \
      *        -Dscala.concurrent.context.numThreads=x1.5 \
      *        ...
      * </pre>
      *
      * As a note, this being backed by Scala's own global execution context,
      * it is cooperating with Scala's BlockContext, so when operations marked
      * with `scala.concurrent.blocking` are encountered, the thread-pool may
      * decide to add extra threads in the pool. However this is not a thread-pool
      * that is optimal for doing blocking operations, so for example if you want
      * to do a lot of blocking I/O, then use a Scheduler backed by a
      * thread-pool that is more optimal for blocking. See for example
      * [[io]].
      */
    implicit lazy val global: Scheduler =
      AsyncScheduler(
        DefaultScheduledExecutor,
        ExecutionContext.Implicits.global,
        UncaughtExceptionReporter.LogExceptionsToStandardErr,
        ExecutionModel.Default
      )
  }
}