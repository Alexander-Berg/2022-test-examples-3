package ru.yandex.tours.util.parsing

import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 08.05.15
 */
class SeparatedKeyValueSpec extends BaseSpec {
  val AtSKV = SeparatedKeyValue("@@", "=")
  "SeparatedKeyValue" should {
    "parse valid @@skv" in {
      AtSKV.unapply("a=b@@c=d") shouldBe Some(Map("a" -> "b", "c" -> "d"))
    }
    "ignore records without value" in {
      AtSKV.unapply("a=b@@c") shouldBe Some(Map("a" -> "b"))
    }
    "parse empty line" in {
      AtSKV.unapply("") shouldBe Some(Map.empty)
    }
    "build valid @@skv" in {
      AtSKV(Seq("a" -> "b", "c" -> "d")) shouldBe "a=b@@c=d"
    }
    "build empty line from empty map" in {
      AtSKV(Map.empty) shouldBe ""
    }
  }
}
