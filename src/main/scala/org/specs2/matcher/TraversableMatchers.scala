package org.specs2
package matcher

import control._
import data.Sized
import text.Quote._
import text.Plural._
import collection.Iterablex._
import MatchersImplicits._
import scala.collection.{GenSeq, GenTraversableOnce, GenTraversable}

/**
 * Matchers for traversables
 */
trait TraversableMatchers extends TraversableBaseMatchers with TraversableBeHaveMatchers
object TraversableMatchers extends TraversableMatchers

private[specs2]
trait TraversableBaseMatchers extends LazyParameters { outer =>
  
  trait TraversableMatcher[T] extends Matcher[GenTraversable[T]]
  
  /** 
   * match if an traversable contains (t).
   * This definition looks redundant with the one below but it is necessary to 
   * avoid the implicit argThat method from Mockito to infer an improper matcher
   * @see the HtmlPrinterSpec failing with a NPE if that method is missing 
   */
  def contain[T](t: =>T): ContainMatcher[T] = contain(lazyfy(t))
  /** match if traversable contains (seq2). n is a dummy paramter to allow overloading */
  def containAllOf[T](seq: Seq[T]): ContainMatcher[T] = new ContainMatcher(seq)
  /** match if traversable contains (t1, t2) */
  def contain[T](t: LazyParameter[T]*): ContainMatcher[T] = new ContainMatcher(t.map(_.value))
  /** match if traversable contains one of (t1, t2) */
  def containAnyOf[T](seq: Seq[T]): ContainAnyOfMatcher[T] = new ContainAnyOfMatcher(seq)

  /** match if traversable contains (x matches p) */
  def containPattern[T](t: =>String): ContainLikeMatcher[T] = containLike[T](t, "pattern")
  /** match if traversable contains (x matches .*+t+.*) */
  def containMatch[T](t: =>String): ContainLikeMatcher[T] = containLike[T](".*"+t+".*", "match")

  /**
   * Matches if there is one element in the traversable verifying the <code>function</code> parameter: <code>(traversable.exists(function(_))</code>
   */
  def have[T](function: T => Boolean) = new Matcher[GenTraversable[T]]{
    def apply[S <: GenTraversable[T]](traversable: Expectable[S]) = {
      result(traversable.value.exists(function(_)),
             "at least one element verifies the property in " + traversable.description, 
             "no element verifies the property in " + traversable.description,
             traversable)
    }
  }
  /**
   * Matches if there l contains the same elements as the Traversable <code>traversable</code>.<br>
   * This verification does not consider the order of the elements but checks the traversables recursively
   */
  def haveTheSameElementsAs[T](l: =>Traversable[T]) = new HaveTheSameElementsAs(l)

  private def containLike[T](pattern: =>String, matchType: String) =
    new ContainLikeMatcher[T](pattern, matchType) 

  /** match if there is a way to size T */
  def haveSize[T : Sized](n: Int) = new SizedMatcher[T](n, "size")
  /** alias for haveSize */
  def size[T : Sized](n: Int) = haveSize[T](n)
  /** alias for haveSize */
  def haveLength[T : Sized](n: Int) = new SizedMatcher[T](n, "length")
  /** alias for haveSize */
  def length[T : Sized](n: Int) = haveLength[T](n)

  /** @return a matcher checking if the elements are ordered */
  def beSorted[T : Ordering] = new OrderingMatcher[T]
  /** alias for beSorted */
  def sorted[T : Ordering] = beSorted[T]

  /** any scala collection has a size */
  implicit def scalaTraversableIsSized[I <: GenTraversableOnce[_]]: Sized[I] = new Sized[I] {
    def size(t: I) = t.size
  }
  /** any scala array has a size */
  implicit def scalaArrayIsSized[T]: Sized[Array[T]] = new Sized[Array[T]] {
    def size(t: Array[T]) = t.length
  }
  /** any java collection has a size */
  implicit def javaCollectionIsSized[T <: java.util.Collection[_]]: Sized[T] = new Sized[T] {
    def size(t: T) = t.size()
  }
  /** a regular string has a size, without having to be converted to an Traversable */
  implicit def stringIsSized: Sized[String] = new Sized[String] {
    def size(t: String) = t.size
  }
}

private[specs2]
trait TraversableBeHaveMatchers extends LazyParameters { outer: TraversableMatchers =>
  implicit def traversable[T](s: MatchResult[GenTraversable[T]]) = new TraversableBeHaveMatchers(s)
  class TraversableBeHaveMatchers[T](s: MatchResult[GenTraversable[T]]) {
    def contain(t: LazyParameter[T], ts: LazyParameter[T]*) = new ContainMatchResult(s, outer.contain((t +: ts):_*))
    def containMatch(t: =>String) = s(outer.containMatch(t))
    def containPattern(t: =>String) = s(outer.containPattern(t))
    def have(f: T => Boolean) = s(outer.have(f))
  }

  implicit def sized[T : Sized](s: MatchResult[T]) = new HasSize(s)
  class HasSize[T : Sized](s: MatchResult[T]) {
    def size(n: Int) : MatchResult[T] = s(outer.haveSize[T](n))
    def length(n: Int) : MatchResult[T] = size(n)
  }

  implicit def orderedSeqMatchResult[T : Ordering](result: MatchResult[Seq[T]]) = new OrderedSeqMatchResult(result)
  class OrderedSeqMatchResult[T : Ordering](result: MatchResult[Seq[T]]) {
    def sorted = result(outer.beSorted[T])
    def beSorted = result(outer.beSorted[T])
  }

}
class ContainMatchResult[T]  private[specs2](val s: MatchResult[GenTraversable[T]], containMatcher: ContainMatcher[T]) extends AbstractContainMatchResult[T] { outer =>
  val matcher = containMatcher
  def only = new ContainOnlyMatchResult(s, containMatcher.only)
  def inOrder = new ContainInOrderMatchResult(s, containMatcher.inOrder)
}
class ContainOnlyMatchResult[T] private[specs2](val s: MatchResult[GenTraversable[T]], containMatcher: ContainOnlyMatcher[T]) extends AbstractContainMatchResult[T] { outer =>
  val matcher = containMatcher
  def inOrder = new ContainOnlyInOrderMatchResult(s, containMatcher.inOrder)
}
class ContainInOrderMatchResult[T]  private[specs2](val s: MatchResult[GenTraversable[T]], containMatcher: ContainInOrderMatcher[T]) extends AbstractContainMatchResult[T] { outer =>
  val matcher = containMatcher
  def only = new ContainOnlyInOrderMatchResult(s, containMatcher.only)
}
class ContainOnlyInOrderMatchResult[T] private[specs2](val s: MatchResult[GenTraversable[T]], containMatcher: Matcher[GenTraversable[T]]) extends AbstractContainMatchResult[T] { outer =>
  val matcher = containMatcher
}
trait AbstractContainMatchResult[T] extends MatchResult[GenTraversable[T]] {
  val matcher: Matcher[GenTraversable[T]]
  protected val s: MatchResult[GenTraversable[T]]
  val expectable = s.expectable
  lazy val matchResult = s(matcher)

  override def toResult = matchResult.toResult
  def not: MatchResult[GenTraversable[T]] = matchResult.not
  def apply(matcher: Matcher[GenTraversable[T]]): MatchResult[GenTraversable[T]] = matchResult(matcher)
}

class ContainMatcher[T](expected: Seq[T], equality: (T, T) => Boolean = (_:T) == (_:T)) extends Matcher[GenTraversable[T]] {
  def apply[S <: GenTraversable[T]](actual: Expectable[S]) = {
    result(actual.value.toList.intersect(expected).sameElementsAs(expected, equality),
           actual.description + " contains " + q(expected.mkString(", ")),
           actual.description + " doesn't contain " + q(expected.mkString(", ")), actual)
  }
  def inOrder = new ContainInOrderMatcher(expected, equality)
  def only = new ContainOnlyMatcher(expected, equality)
  def exactlyOnce = new ContainExactlyOnceMatcher(expected, equality)

  /** use a specific equality function */
  def ^^[S](eq: (T, T) => Boolean) = new ContainMatcher[T](expected, eq)
  /** use a matcher function to define if 2 values are equal. The first value defines a matcher to use with the second one */
  def ^^[S](m: T => Matcher[T]) = new ContainMatcher[T](expected, (t1: T, t2: T) => m(t1).apply(Expectable(t2)).isSuccess)
  /** use a specific adaption function before checking for equality */
  def ^^^[S](adaptator: T => S) = new ContainMatcher[T](expected, (t1: T, t2: T) => adaptator(t1) == adaptator(t2))

}

class ContainExactlyOnceMatcher[T](expected: Seq[T], equality: (T, T) => Boolean = (_:T) == (_:T)) extends Matcher[GenTraversable[T]] {
  def apply[S <: GenTraversable[T]](actual: Expectable[S]) = {
    result(expected.forall(e => actual.value.filter(v => equality(v, e)).size == 1),
           actual.description + " contains exactly once " + q(expected.mkString(", ")),
           actual.description + " doesn't contain exactly once " + q(expected.mkString(", ")), actual)
  }

  /** use a specific equality function */
  def ^^[S](eq: (T, T) => Boolean) = new ContainExactlyOnceMatcher[T](expected, eq)
  /** use a matcher function to define if 2 values are equal. The first value defines a matcher to use with the second one */
  def ^^[S](m: T => Matcher[T]) = new ContainExactlyOnceMatcher[T](expected, (t1: T, t2: T) => m(t1).apply(Expectable(t2)).isSuccess)
  /** use a specific adaption function before checking for equality */
  def ^^^[S](adaptator: T => S) = new ContainExactlyOnceMatcher[T](expected, (t1: T, t2: T) => adaptator(t1) == adaptator(t2))

}

class ContainAnyOfMatcher[T](expected: Seq[T], equality: (T, T) => Boolean = (_:T) == (_:T)) extends Matcher[GenTraversable[T]] {
  def apply[S <: GenTraversable[T]](actual: Expectable[S]) = {
    result(actual.value.toList.exists((v: T) => expected.exists((e: T) => equality(v, e))),
           actual.description + " contains at least one of " + q(expected.mkString(", ")),
           actual.description + " doesn't contain any of " + q(expected.mkString(", ")), actual)
  }

  /** use a specific equality function */
  def ^^[S](eq: (T, T) => Boolean) = new ContainAnyOfMatcher[T](expected, eq)

  /** use a matcher function to define if 2 values are equal. The first value defines a matcher to use with the second one */
  def ^^[S](m: T => Matcher[T]) = new ContainAnyOfMatcher[T](expected, (t1: T, t2: T) => m(t1).apply(Expectable(t2)).isSuccess)

  /** use a specific adaption function before checking for equality */
  def ^^^[S](adaptator: T => S) = new ContainAnyOfMatcher[T](expected, (t1: T, t2: T) => adaptator(t1) == adaptator(t2))

}

class ContainInOrderMatcher[T](expected: Seq[T], equality: (T, T) => Boolean = (_:T) == (_:T)) extends Matcher[GenTraversable[T]] {
  def apply[S <: GenTraversable[T]](actual: Expectable[S]) = {
    result(inOrder(actual.value.toList, expected, equality),
           actual.description + " contains in order " + q(expected.mkString(", ")),
           actual.description + " doesn't contain in order " + q(expected.mkString(", ")), actual)
  }
  
  private def inOrder[T](l1: Seq[T], l2: Seq[T], equality: (T, T) => Boolean): Boolean = {
   l1.toList match {
      case Nil                      => l2.isEmpty
      case x :: rest if !l2.isEmpty => equality(x, l2.head) && inOrder(l1.drop(1), l2.drop(1), equality) ||
                                       inOrder(l1.drop(1), l2, equality)
      case other                    => false
    }
  }

  def only: Matcher[GenTraversable[T]] = (this and new ContainOnlyMatcher(expected))

  /** use a specific equality function */
  def ^^[S](eq: (T, T) => Boolean) = new ContainInOrderMatcher[T](expected, eq)

  /** use a matcher function to define if 2 values are equal. The first value defines a matcher to use with the second one */
  def ^^[S](m: T => Matcher[T]) = new ContainInOrderMatcher[T](expected, (t1: T, t2: T) => m(t1).apply(Expectable(t2)).isSuccess)

  /** use a specific adaption function before checking for equality */
  def ^^^[S](adaptator: T => S) = new ContainInOrderMatcher[T](expected, (t1: T, t2: T) => adaptator(t1) == adaptator(t2))

}

class ContainOnlyMatcher[T](expected: Seq[T], equality: (T, T) => Boolean = (_:T) == (_:T)) extends Matcher[GenTraversable[T]] {
  def apply[S <: GenTraversable[T]](traversable: Expectable[S]) = {
    val actual = traversable.value
    result(actual.toList.intersect(expected).sameElementsAs(expected, equality) && expected.size == actual.size,
           traversable.description + " contains only " + q(expected.mkString(", ")),
           traversable.description + " doesn't contain only " + q(expected.mkString(", ")), traversable)
  }
  def inOrder: Matcher[GenTraversable[T]] = (this and new ContainInOrderMatcher(expected))
}

class ContainLikeMatcher[T](pattern: =>String, matchType: String) extends Matcher[GenTraversable[T]] {
  def apply[S <: GenTraversable[T]](traversable: Expectable[S]) = {
    val a = pattern
    result(traversable.value.exists(_.toString.matches(a)), 
           traversable.description + " contains "+matchType+ " " + q(a), 
           traversable.description + " doesn't contain "+matchType+ " " + q(a), traversable)
  }
  def onlyOnce = new ContainLikeOnlyOnceMatcher[T](pattern, matchType)
}

class ContainLikeOnlyOnceMatcher[T](pattern: =>String, matchType: String) extends Matcher[GenTraversable[T]] {
  def apply[S <: GenTraversable[T]](traversable: Expectable[S]) = {
    val a = pattern
    val matchNumber = traversable.value.filter(_.toString.matches(a)).size
    val koMessage = 
      if (matchNumber == 0)
        traversable.description + " doesn't contain "+matchType+ " " + q(a)
      else
        traversable.description + " contains "+matchType+ " " + q(a) + " "+ (matchNumber qty "time")
        
    result(matchNumber == 1, 
           traversable.description + " contains "+matchType+ " " + q(a) + " only once", 
           koMessage, 
           traversable)
  }
}

/**
 * This matcher checks if a traversable has the same elements as another one (that is, recursively, in any order)
 */
class HaveTheSameElementsAs[T](l: =>Traversable[T], equality: (T, T) => Boolean = (_:T) == (_:T)) extends Matcher[GenTraversable[T]] {
  def apply[S <: GenTraversable[T]](traversable: Expectable[S]) = {
    result(l.toSeq.sameElementsAs(traversable.value.toSeq, equality),
           traversable.value.toSeq.toDeepString + " has the same elements as " + q(l.toSeq.toDeepString),
           traversable.value.toSeq.toDeepString + " doesn't have the same elements as " + q(l.toSeq.toDeepString),
           traversable)
  }

  /** use a specific equality function */
  def ^^[S](equality: (T, T) => Boolean) = new HaveTheSameElementsAs[T](l, equality)

  /** use a matcher function to define if 2 values are equal. The first value defines a matcher to use with the second one */
  def ^^[S](m: T => Matcher[T]) = new HaveTheSameElementsAs[T](l, (t1: T, t2: T) => m(t1).apply(Expectable(t2)).isSuccess)

  /** use a specific adaption function before checking for equality */
  def ^^^[S](adaptator: T => S) = new HaveTheSameElementsAs[T](l, (t1: T, t2: T) => adaptator(t1) == adaptator(t2))

}

class SizedMatcher[T : Sized](n: Int, sizeWord: String) extends Matcher[T] {
  def apply[S <: T](traversable: Expectable[S]) = {
    val s = implicitly[Sized[T]]
    val valueSize = s.size(traversable.value)
    result(valueSize == n,
           traversable.description + " have "+sizeWord+" "+ n,
           traversable.description + " doesn't have "+sizeWord+" " + n + " but "+sizeWord+" " + valueSize, traversable)
  }
}

class OrderingMatcher[T : Ordering] extends Matcher[Seq[T]] {
  def apply[S <: Seq[T]](traversable: Expectable[S]) = {
    result(traversable.value == traversable.value.sorted,
      traversable.description + " is sorted",
      traversable.description + " is not sorted", traversable)
  }
}
