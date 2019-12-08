package org.specs2
package specification
package core

import control._
import io.FileSystem
import main.Arguments
import reporter.PrinterLogger.NoPrinterLogger
import specification.process._
import reflect._

object EnvDefault {

  def default: Env =
    create(Arguments())

  def create(arguments: Arguments): Env =
    Env(
      arguments           = arguments,
      systemLogger        = ConsoleLogger(),
      printerLogger       = NoPrinterLogger,
      statsRepository     = StatisticsRepositoryCreation.memory,
      random              = new scala.util.Random,
      fileSystem          = FileSystem(ConsoleLogger()),
      customClassLoader   = None,
      classLoading        = new ClassLoading {}
    )

  def defaultInstances(env: Env) =
    List[AnyRef](
      env.arguments.commandLine,
      env.executionEnv,
      env.executionContext,
      env.arguments,
      env)

}
