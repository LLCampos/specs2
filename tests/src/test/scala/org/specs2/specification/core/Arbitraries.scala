package org.specs2
package specification
package core

import org.scalacheck._, Gen._, Arbitrary._
import create._
import execute.StandardResults._
import FormattingFragments._
import DefaultFragmentFactory._

object Arbitraries:

  given FragmentArbitrary: Arbitrary[Fragment] =
    Arbitrary {
      Gen.oneOf(
          genExample,
          genText,
          genStep,
          genFormatting
        )
    }

  given FragmentsArbitrary: Arbitrary[Fragments] =
    Arbitrary {
      Gen.listOf(arbitrary[Fragment]).map(fs => Fragments(fs:_*))
    }

  def genExample: Gen[Fragment] =
    alphaStr.map(text => example(text, Execution.executed(success)))

  def genText: Gen[Fragment] =
    alphaStr.map(text)

  def genStep: Gen[Fragment] =
    Gen.const(step(success))

  def genFormatting: Gen[Fragment] =
    Gen.oneOf(
      Seq(br, t, t(2), bt, bt(2), FormattingFragments.end)
    )
