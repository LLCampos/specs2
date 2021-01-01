package org.specs2.concurrent

import java.util.concurrent._
import scala.util.NotGiven

trait ImplicitExecutorServiceFromExecutionEnv:
  /**
   * if an implicit execution environment is in scope, it can be used as an executor service
   */
  given executionEnvToExecutorService(using ee: ExecutionEnv, not: NotGiven[NoImplicitExecutorServiceFromExecutionEnv]): ExecutorService =
    ee.executorService

/**
 * deactivate the conversion between an implicit execution environment to an executor service
 */
trait NoImplicitExecutorServiceFromExecutionEnv extends ImplicitExecutorServiceFromExecutionEnv:
  given NoImplicitExecutorServiceFromExecutionEnv = ???
