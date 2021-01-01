package org.specs2
package matcher

import org.specs2.matcher.describe.Diffable
import scala.util.NotGiven
import execute._

/**
 * This trait adds some implicits to create expectations with the `===` sign
 */
trait TypedEqual:
  this: ExpectationsCreation =>

  /**
   * A value can be tested against another with the === operator.
   * It is equivalent to writing a must ==(b)
   */
  extension [T](t: =>T)(using not: NotGiven[NoTypedEqual])
    /** typed equality matcher on Expectables */
    def ====(other: =>T)(using di: Diffable[T]): Result =
      createExpectable(t).applyMatcher(new EqualityMatcher(other))

    /** ! typed equality matcher on Expectables */
    def !===(other: =>T)(using di: Diffable[T]): Result =
      createExpectable(t).applyMatcher(new EqualityMatcher(other).not)

  extension [T, S >: T](t: =>T)(using not: NotGiven[NoTypedEqual])
    /** equality matcher on Expectables */
    def ===(other: =>S): Result =
      createExpectable(t).applyMatcher[S](new BeEqualTo(other))

    /** ! equality matcher on Expectables */
    def !==(other: =>S): Result =
      createExpectable(t).applyMatcher[S](new BeEqualTo(other).not)

object TypedEqual extends TypedEqual with ExpectationsCreation

/**
 * This trait can be used to suppress the TypedEqual implicit
 */
trait NoTypedEqual extends TypedEqual:
  self: ExpectationsCreation =>
  given NoTypedEqual = ???
