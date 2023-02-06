package ru.yandex.tours.util

import org.scalatest.WordSpecLike
import Collections._
import org.scalatest.Matchers._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 05.02.15
 */
class RichIterableSpec extends WordSpecLike {

  "toMultiMap" should {
    "convert `Iterable[(A, B)]` to `Map[A, List[B]]`" in {
      val list = List(1 -> 2, 1 -> 3, 2 -> 4)
      val expected = Map(1 -> List(2, 3), 2 -> List(4))
      list.toMultiMap shouldBe expected
    }
    "not return lazy map" in {
      val list = List(1 -> 2, 1 -> 3, 2 -> 4)
      val result = list.toMultiMap

      val lazyMapClass = Class.forName("scala.collection.MapLike$MappedValues")
      lazyMapClass.isInstance(result) shouldBe false
    }
  }
}
