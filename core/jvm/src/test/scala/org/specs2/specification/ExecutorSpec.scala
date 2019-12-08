package org.specs2
package specification

import execute._

import scala.collection.mutable.ListBuffer
import matcher.{ResultMatchers, ThrownExpectations}

import scala.concurrent.duration._
import main.Arguments
import concurrent.ExecutionEnv
import specification.core._
import specification.process.DefaultExecutor
import fp.syntax._
import ResultMatchers._
import scala.concurrent._

class ExecutorSpec(val env: Env) extends Specification with ThrownExpectations with OwnEnv { def is = section("travis") ^ s2"""

 Steps
 =====
  by step $e1
  stop on failed specified on a step $e2
  stop on error specified on a step $e3
  stop on skip specified in arguments $e4
  stop on failed with a sequential specification $e5
  skipAll from arguments $e6

 Execute
 =======
  sequentially $e7
  with in-between steps $e8
  with a fatal execution error $e9
  with a fatal execution error in a step $e10
  stopOnFail and sequential $e11

  with a timeout $timeout
  with examples using an execution context $userEnv

 Time
 ====

  the timer must be started for each execution $e12
  the timer must be started for each sequential execution $e13
  the timer must be started for each skipped execution $e14

"""

  import factory._

  def e1 = {
    val results = Results(); import results._
    val tf = ownEnv.arguments.execute.timeFactor
    val fragments = Fragments(
      example("slow", slow(tf)),
      example("medium", medium(tf)),
      step(step1),
      example("fast", fast(tf)))
    execute(fragments, ownEnv) must not(contain(beSkipped[Result]))
    messages.toList must_== Seq("medium", "slow", "step", "fast")
  }

  def e2 = {
    val results = Results(); import results._
    val tf = ownEnv.arguments.execute.timeFactor
    val fragments = Fragments(
      example("slow", slow(tf)),
      example("medium", mediumFail(tf)),
      step(step1).stopOnFail,
      example("fast", fast(tf)))
    execute(fragments, ownEnv) must contain(beSkipped[Result])
    messages.toList must_== Seq("medium", "slow", "step")
  }

  def e3 = {
    val results = Results(); import results._
    val tf = ownEnv.arguments.execute.timeFactor
    val fragments = Fragments(
      example("slow", slow(tf)),
      example("medium", mediumError(tf)),
      step(step1).stopOnError,
      example("fast", fast(tf)))
    execute(fragments, ownEnv) must contain(beSkipped[Result])
    messages.toList must_== Seq("medium", "slow", "step")
  }

  def e4 = {
    val results = Results(); import results._
    val tf = ownEnv.arguments.execute.timeFactor
    val fragments = Fragments(
      example("slow", slow(tf)),
      example("medium", mediumSkipped(tf)),
      step(step1),
      example("fast", fast(tf)))
    execute(fragments, ownEnv.setArguments(Arguments("stopOnSkip"))) must contain(beSkipped[Result])
    messages.toList must_== Seq("medium", "slow", "step")
  }

  def e5 = {
    val results = Results(); import results._
    val tf = ownEnv.arguments.execute.timeFactor
    val fragments = Fragments(
      example("slow", slow(tf)),
      example("medium", mediumFail(tf)),
      example("fast", fast(tf)))
    execute(fragments, ownEnv.setArguments(Arguments("stopOnFail", "sequential"))) must contain(beFailing[Result])
    messages.toList must_== Seq("slow", "medium")
  }

  def e6 = {
    val results = Results(); import results._
    val tf = ownEnv.arguments.execute.timeFactor
    val fragments = Fragments(
      example("ex1", fast(tf)),
      example("ex2", fast(tf)))
    execute(fragments, ownEnv.setArguments(Arguments("skipAll"))) must contain(beSkipped[Result])
    messages.toList must beEmpty
  }

  def e7 = {
    val results = Results(); import results._
    val tf = ownEnv.arguments.execute.timeFactor
    val fragments = Fragments(
      example("slow", slow(tf)),
      example("medium", medium(tf)),
      step(step1),
      example("fast", fast(tf)))
    execute(fragments, ownEnv.setArguments(Arguments("sequential"))) must not(contain(beSkipped[Result]))
    messages.toList must_== Seq("slow", "medium", "step", "fast")
  }

  def e8 = {
    val results = Results(); import results._
    val tf = ownEnv.arguments.execute.timeFactor
    val fragments = Fragments(
      example("slow", slow(tf)),
      example("medium", medium(tf)),
      step(step1),
      example("fast", fast(tf)))
    execute(fragments, ownEnv.setArguments(Arguments("sequential")))
    messages.toList must_== Seq("slow", "medium", "step", "fast")
  }

  def e9 = {
    val results = Results()
    val fragments = Fragments(
      example("fast1", results.ok("ok1")),
      step(results.fatalStep),
      example("fast2", results.ok("ok2")))
    execute(fragments, ownEnv)
    results.messages.toList must_== Seq("ok1", "fatal")
  }

  def e10 = {
    val fragments = Fragments(
      step(throw new Exception("fatal")),
      example("e1", ok("ok")),
      step(throw new Exception("fatal")))
    val rs = execute(fragments, ownEnv).map(_.status)
    rs must contain("!", "o", "!")
  }

  def e11 = {
    val fragments = Fragments(
      example("e1", ko("ko1")),
      example("e2", ok("ok2")))
    val env1 = ownEnv.setArguments(Arguments.split("sequential stopOnFail"))
    val rs = execute(fragments, env1).map(_.status)
    rs must contain("x", "o")
  }

  def tf = ownEnv.arguments.execute.timeFactor

  def fragments(results: Results) = {
    import results._
    Fragments(
    example("slow", slow(tf)),
    example("medium", medium(tf)),
    example("fast", fast(tf)))
  }

  def e12 = {
    val results = Results()
    val times = executionTimes(fragments(results), ownEnv)
    times must containMatch("(\\d)+ ms")
  }

  def e13 = {
    val results = Results()
    val times = executionTimes(fragments(results), ownEnv.setArguments(Arguments("sequential")))
    times must containMatch("(\\d)+ ms")
  }

  def e14 = {
    val results = Results()
    val times = executionTimes(fragments(results), ownEnv.setArguments(Arguments("skipAll")))
    times must containMatch("(\\d)+ ms")
  }

  def timeout = {
    val timeFactor = 1 //ownEnv.arguments.execute.timeFactor

    val messages = new ListBuffer[String]
    def verySlow = { Thread.sleep(600 * timeFactor.toLong); messages.append("very slow"); success }

    val fragments = Fragments(example("very slow", verySlow))
    val env1 = ownEnv.setTimeout(100.millis * timeFactor.toLong)

    execute(fragments, env1) must contain(beSkipped[Result]("timed out after "+100*timeFactor+" milliseconds"))
  }

  def userEnv = {
    val fragments =
      Fragments.foreach(1 to 2) { i: Int =>
        "test " + i ! Execution.withExecutionEnv { ee: ExecutionEnv =>
          Await.result(scala.concurrent.Future(1)(ee.executionContext), 5.second) ==== 1
        }
      }
    val e = Env()
    try execute(fragments, e) must contain(beSuccessful[Result]).forall
    finally e.shutdown
  }

  lazy val factory = fragmentFactory

  def execute(fragments: Fragments, env: Env): List[Result] =
    DefaultExecutor(env).execute(env.arguments)(fragments.contents).runList.
      runOption(env.executionEnv).toList.flatten.traverse(_.executionResult).run(env.executionEnv)

  def executionTimes(fragments: Fragments, env: Env): List[String] =
    DefaultExecutor(env).execute(env.arguments)(fragments.contents).runList.
      runOption(env.executionEnv).toList.flatten.traverse(_.executedResult.map(_.timer.time)).run(env.executionEnv)

  def executions(fragments: Fragments, env: Env): List[Execution] =
    DefaultExecutor(env).execute(env.arguments)(fragments.contents).runList.
      runOption(env.executionEnv).toList.flatten.map(_.execution)

  case class Results() {
    val messages = new ListBuffer[String]

    // this cannot be made lazy vals otherwise this will block on 'slow'
    def ok(name: String)              = { messages.append(name); success }
    def ko(name: String)              = { messages.append(name); failure }
    def fast(timeFactor: Int)         = { messages.append("fast"); success }
    def medium(timeFactor: Int)       = { Thread.sleep(10 * timeFactor.toLong);  messages.append("medium"); success }
    def ex(s: String)                 = { messages.append(s); success }
    def mediumFail(timeFactor: Int)   = { Thread.sleep(10 * timeFactor.toLong);  messages.append("medium"); failure }
    def mediumError(timeFactor: Int)  = { Thread.sleep(10 * timeFactor.toLong);  messages.append("medium"); anError }
    def mediumSkipped(timeFactor: Int)= { Thread.sleep(10 * timeFactor.toLong);  messages.append("medium"); skipped }
    def slow(timeFactor: Int)         = { Thread.sleep(400 * timeFactor.toLong); messages.append("slow");   success }
    def verySlow(timeFactor: Int)     = { Thread.sleep(600 * timeFactor.toLong); messages.append("very slow"); success }
    def step1                         = { messages.append("step");   success }
    def fatalStep                     = { messages.append("fatal");  if (true) throw new java.lang.Error("fatal error!"); success }
  }

}
