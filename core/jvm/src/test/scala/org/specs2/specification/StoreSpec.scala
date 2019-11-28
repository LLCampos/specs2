package org.specs2
package specification

import io._
import fp.syntax._
import execute.AsResult
import control._
import process._

class StoreSpec extends Specification { def is = sequential ^ s2"""
 The file store stores values in files where the name of the file is
   defined by the key $e1

 The store can be reset $e2

"""

  def e1 = {
    val store = DirectoryStore("target" / "test", FileSystem(ConsoleLogger()))
    val key = SpecificationStatsKey("name")
    (store.set(key, Stats(1)) >> store.get(key)).map(_ must beSome(Stats(1)))
  }

  def e2 = {
    val store = DirectoryStore("target" / "test", FileSystem(ConsoleLogger()))
    AsResult(e1)

    val key = SpecificationStatsKey("name")
    (store.reset >> store.get(key)).map(_ must beNone)
  }

}
