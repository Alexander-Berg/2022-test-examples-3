package ru.yandex.tours.util.collections

import ru.yandex.tours.testkit.BaseSpec

class RafBasedMapSpec extends BaseSpec {
  def emptyMap = new RafBasedMap[Long, Array[Byte]](x => x, x => x)

  def toBytes(xs: Int*) = xs.map(_.toByte).toArray

  "Raf Based Map" should {
    "empty if created" in {
      emptyMap shouldBe empty
    }

    "add elements" in {
      val map = emptyMap
      map += 1L -> toBytes(1, 2, 3)
      map += 2L -> toBytes(2, 3, 4)
      map should have size 2
    }

    "retrieve elements" in {
      val map = emptyMap
      map += 3L -> toBytes(2, 3, 4)
      map += 1L -> toBytes(2, 1, 4)
      map.get(3L).value.toSeq shouldBe toBytes(2, 3, 4).toSeq
      map.get(1L).value.toSeq shouldBe toBytes(2, 1, 4).toSeq
      map.get(4L) shouldBe None
    }

    "mmap to memory after freeze()" in {
      val map = emptyMap
      map += 1L -> toBytes(1, 2, 3)
      map += 2L -> toBytes(2, 3, 4)
      map.freeze()
      map.get(1L).value.toSeq shouldBe toBytes(1, 2, 3).toSeq
    }
  }
}
