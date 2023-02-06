package ru.yandex.tours.util.collections

import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 21.07.15
 */
class Top1Spec extends BaseSpec {
  case class I(i: Int)
  def newTop = new Top1()(Ordering.by[I, Int](_.i))

  "empty Top1" should {
    "be empty" in {
      val top = newTop
      top.isEmpty shouldBe true
      top.nonEmpty shouldBe false
      top.size shouldBe 0
    }
    "have head == null" in {
      newTop.head shouldBe null
    }
    "convert to empty list" in {
      newTop.toList shouldBe Nil
    }
  }
  "non empty Top1" should {
    "be non empty after insertion" in {
      val top = newTop
      top += I(10)
      top.size shouldBe 1
      top.isEmpty shouldBe false
      top.nonEmpty shouldBe true
      top.toList shouldBe List(I(10))
    }
    "have correct head" in {
      val top = newTop
      top += I(10)
      top.head shouldBe I(10)
    }
    "have correct toList" in {
      val top = newTop
      top += I(10)
      top.toList shouldBe List(I(10))
    }
  }
  "Top1" should {
    "contain smallest element" in {
      val top = newTop
      top += I(10)
      top.head shouldBe I(10)
      top.toList shouldBe List(I(10))

      top += I(12)
      top.head shouldBe I(10)
      top.toList shouldBe List(I(10))

      top += I(1)
      top.head shouldBe I(1)
      top.toList shouldBe List(I(1))
    }
  }
}
