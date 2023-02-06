package ru.yandex.tours.util

import org.scalatest.WordSpecLike
import Collections._
import org.scalatest.Matchers._
import org.scalatest.matchers.{MatchResult, Matcher}

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 22.04.15
 */
class RichIteratorSpec extends WordSpecLike {

  "iterator.onFinish" should {
    "call callback if iteration is complete" in {
      var called: Boolean = false
      val it = Iterator(1, 2, 3).onFinish { called = true }

      it.toList shouldBe List(1, 2, 3)

      called should beCalled
    }
    "not call callback if iteration is not complete" in {
      var called: Boolean = false
      val it = Iterator(1, 2, 3).onFinish { called = true }

      it.hasNext shouldBe true
      it.next shouldBe 1
      it.hasNext shouldBe true
      it.next shouldBe 2

      called shouldNot beCalled
    }
    "execute callback on final hasNext" in {
      var called: Boolean = false
      val it = Iterator.empty.onFinish { called = true }

      called shouldNot beCalled

      it.hasNext shouldBe false
      called should beCalled
    }
    "call callback only once" in {
      var called: Int = 0
      val it = Iterator.empty.onFinish { called += 1 }

      it.hasNext shouldBe false
      called shouldBe 1

      it.hasNext shouldBe false
      called shouldBe 1
    }
    "cache result of hasNext" in {
      var called: Boolean = false
      val it = new Iterator[Int] {
        override def hasNext: Boolean = {
          if (called) {
            fail("Iterator.hasNext on closed iterator")
          } else false
        }
        override def next(): Int = 0
      }.onFinish { called = true }
      it.hasNext shouldBe false
      called should beCalled
      it.hasNext shouldBe false
      called should beCalled
    }
  }

  val beCalled = new Matcher[Boolean] {
    override def apply(left: Boolean): MatchResult = {
      MatchResult(
        left,
        "Method is not called",
        "Method is called"
      )
    }
  }
}
