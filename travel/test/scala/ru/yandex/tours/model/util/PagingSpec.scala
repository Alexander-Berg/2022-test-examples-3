package ru.yandex.tours.model.util

import org.scalatest.WordSpecLike
import org.scalatest.Matchers._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 12.01.15
 */
class PagingSpec extends WordSpecLike {
  val collection = 0 until 999

  "Paging" should {
    "slice collection" in {
      Paging(0, 10).apply(collection) shouldBe (0 until 10)
      Paging(1, 10).apply(collection) shouldBe (10 until 20)
      Paging(2, 10).apply(collection) shouldBe (20 until 30)
    }
    "slice collection with correct size" in {
      Paging(0, 10).apply(collection).size shouldBe 10
      Paging(1, 20).apply(collection).size shouldBe 20
      Paging(2, 27).apply(collection).size shouldBe 27
    }
    "be empty for pageSize <= 0" in {
      Paging(0, 0).apply(collection) shouldBe empty
      Paging(1, 0).apply(collection) shouldBe empty
      Paging(999, 0).apply(collection) shouldBe empty
      Paging(0, -1).apply(collection) shouldBe empty
      Paging(0, -10).apply(collection) shouldBe empty
    }
    "be empty for page outside collection size" in {
      Paging(999, 10).apply(collection) shouldBe empty
      Paging(-1, 10).apply(collection) shouldBe empty
    }
  }
}
