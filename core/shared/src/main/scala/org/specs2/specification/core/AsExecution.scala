package org
package specs2
package specification.core

import execute._
import scala.concurrent.Future

trait AsExecution[T]:
  def execute(t: =>T): Execution

object AsExecution extends AsExecutionLowImplicits:

  def apply[T](using t: AsExecution[T]): AsExecution[T] =
    t

  given [R : AsResult]: AsExecution[R] with
    def execute(r: => R): Execution =
      Execution.result(AsResult(r))


trait AsExecutionLowImplicits:

  given [R : AsResult]: AsExecution[Future[R]] with
    def execute(r: =>Future[R]): Execution =
      Execution.withEnvAsync(_ => r)
