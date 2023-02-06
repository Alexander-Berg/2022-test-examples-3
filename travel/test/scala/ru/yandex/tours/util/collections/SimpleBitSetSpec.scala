package ru.yandex.tours.util.collections

import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 20.06.16
 */
class SimpleBitSetSpec extends BaseSpec {
  "SimpleBitSet" should {
    "convert long to set" in {
      SimpleBitSet.from(0L).toSet shouldBe Set()
      SimpleBitSet.from(1L).toSet shouldBe Set(0)
      SimpleBitSet.from(2L).toSet shouldBe Set(1)
      SimpleBitSet.from(3L).toSet shouldBe Set(0, 1)
    }
    "convert set to long" in {
      SimpleBitSet(Set.empty).packed shouldBe 0L
      SimpleBitSet(Set(0)).packed shouldBe 1L
      SimpleBitSet(Set(1)).packed shouldBe 2L
      SimpleBitSet(Set(0, 1)).packed shouldBe 3L
    }
    "convert long to set and inverse" in {
      for (i <- 0 to 64) {
        withClue("i = " + i) {
          val set = SimpleBitSet.from(i).toSet
          SimpleBitSet(set).packed shouldBe i
        }
      }
    }
    "convert set to long and inverse" in {
      for (i <- 0 to 63) {
        withClue("i = " + i) {
          val packedSet = SimpleBitSet(Set(i)).packed
          SimpleBitSet.from(packedSet).toSet shouldBe Set(i)
        }
      }
    }
  }
}
