package ru.yandex.tours.util.collections

import org.scalatest.Matchers._
import org.scalatest.WordSpecLike

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 05.06.15
 */
class BagSpec extends WordSpecLike {
  "Bag" should {
    "be empty" in {
      new Bag[Int] shouldBe empty
    }
    "collect elements" in {
      val bag = new Bag[Int]
      bag += 7
      bag should not be empty
      bag should have size 1
    }
    "collect several elements" in {
      val bag = new Bag[Int]
      bag += 7
      bag += 8
      bag should have size 2
    }
    "return element count" in {
      val bag = new Bag[Int]
      bag += 7
      bag.getCount(7) shouldBe 1
      bag += 7
      bag.getCount(7) shouldBe 2
    }
    "return count == 0 if element is missing" in {
      val bag = new Bag[Int]
      bag.getCount(7) shouldBe 0
    }
    "add element with count" in {
      val bag = new Bag[Int]
      bag += (7, 2)
      bag.getCount(7) shouldBe 2
      bag += (7, 5)
      bag.getCount(7) shouldBe 7
    }
    "filter elements by count" in {
      val bag = new Bag[Int]
      bag += (7, 5)
      bag += (8, 3)
      val newBag = bag.filterByCount(_ > 4)
      bag should not be theSameInstanceAs (newBag)
      newBag should have size 1
      newBag.toSeq shouldBe Seq(7)
      newBag.getCount(7) shouldBe 5
    }
    "sum element counts" in {
      val bag = new Bag[Int]
      bag += 1
      bag += (2, 3)
      bag += (3, 2)
      bag.sum shouldBe (1 + 3 + 2)
    }
    "return element sorted by count" in {
      val bag = new Bag[Int]
      bag += 1
      bag += (2, 3)
      bag += (3, 2)
      bag.keysAsc shouldBe Seq(1, 3, 2)
      bag.keysDesc shouldBe Seq(2, 3, 1)
    }
    "be iterable" in {
      val bag = new Bag[Int]
      bag += 1
      bag += (2, 3)
      bag += (3, 2)
      bag += 1
      bag should contain theSameElementsAs List(1, 2, 3)
    }
  }
}
