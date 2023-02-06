package ru.yandex.tours.util.parsing

import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 08.05.15
 */
class TSKVSpec extends BaseSpec {
  "TSKV" should {
    "parse valid tskv" in {
      TSKV.unapply("a=b\tc=d") shouldBe Some(Map("a" -> "b", "c" -> "d"))
    }
    "ignore records without value" in {
      TSKV.unapply("a=b\tc") shouldBe Some(Map("a" -> "b"))
    }
    "parse empty line" in {
      TSKV.unapply("") shouldBe Some(Map.empty)
    }
    "build valid tskv" in {
      TSKV(Seq("a" -> "b", "c" -> "d")) shouldBe "a=b\tc=d"
    }
    "build empty line from empty map" in {
      TSKV(Map.empty) shouldBe ""
    }
  }
}
