package ru.yandex.tours.indexer.clusterization

import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 20.04.16
 */
class ComparingUtilSpec extends BaseSpec {

  "ComparingUtil" should {
    "translite name" in {
      ComparingUtil.translite("эдем") shouldBe "edem"
    }
    "translite name in upper case" in {
      ComparingUtil.translite("ЭДЕМ") shouldBe "edem"
    }
    "clean name" in {
      ComparingUtil.cleanName("эдем") shouldBe "эдем"
    }
    "split name" in {
      ComparingUtil.split("эдем").toList shouldBe List("эдем")
    }
    "split camelCase" in {
      ComparingUtil.splitCamelCase("aB") shouldBe "a B"
      ComparingUtil.splitCamelCase("1Bgg") shouldBe "1 Bgg"
    }
    "split by non-digits" in {
      ComparingUtil.splitByNonDigits("aa123bbb").toList shouldBe List("123")
      ComparingUtil.splitByNonDigits("aa123b333").toList shouldBe List("123", "333")
      ComparingUtil.splitByNonDigits("aa123bbb333").toList shouldBe List("123", "333")
    }
  }
}
