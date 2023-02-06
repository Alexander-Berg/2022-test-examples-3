package ru.yandex.tours.util.collections

import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 10.06.15
 */
class Top3Spec extends BaseSpec {

  case class Obj(identity: Int, order: Int = 0)

  def newTop = new Top3[Obj, Int](_.identity)(Ordering.by(_.order), Ordering.by(_.order))

  "empty Top3" should {
    val e = newTop
    "be empty" in {
      e.isEmpty shouldBe true
      e.nonEmpty shouldBe false
      e.size shouldBe 0
    }
    "have head == null" in {
      e.head shouldBe null
    }
    "convert to empty List" in {
      e.toList shouldBe Nil
    }
  }
  "Top3 with 1 element" should {
    val e1 = newTop
    e1 += Obj(1)
    "not be empty" in {
      e1.isEmpty shouldBe false
      e1.nonEmpty shouldBe true
    }
    "correct size" in {
      e1.size shouldBe 1
    }
    "have head" in {
      e1.head shouldBe Obj(1)
    }
    "convert to List" in {
      e1.toList shouldBe List(Obj(1))
    }
  }
  "Top3 with 2 elements" should {
    "have correct size" in {
      val e = newTop
      e += Obj(1)
      e += Obj(2)
      e.size shouldBe 2
    }
  }
  "Top3" should {
    "swap head on append if new element is lesser than current head" in {
      val e = newTop
      e += Obj(1, 10)
      e.head shouldBe Obj(1, 10)
      e += Obj(1, 9)
      e.head shouldBe Obj(1, 9)
    }
    "keep smallest element in cluster determined by `distinctBy` function" in {
      val e = newTop

      e += Obj(1, 10)
      e should have size 1

      e += Obj(1, 9)
      e should have size 1
    }
    "keep 3 smallest elements across many clusters" in {
      val e = newTop

      e += Obj(1, 1)
      e += Obj(2, 2)
      e += Obj(3, 3)
      e += Obj(4, 4)
      e += Obj(5, 5)

      e should have size 3
      e.toList shouldBe List(Obj(1, 1), Obj(2, 2), Obj(3, 3))
    }
    "ignore order of insertion" in {
      val elements = Seq(Obj(1, 1), Obj(1, 2), Obj(2, 2), Obj(2, 12), Obj(3, 3), Obj(3, 33), Obj(4, 4), Obj(5, 5))
      for (permutation <- elements.permutations) {
        val e = newTop

        permutation.foreach { e += _ }

        e should have size 3
        e.toList shouldBe List(Obj(1, 1), Obj(2, 2), Obj(3, 3))
      }
    }
  }
}
