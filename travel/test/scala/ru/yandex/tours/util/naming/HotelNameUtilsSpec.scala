package ru.yandex.tours.util.naming

import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 18.03.16
 */
class HotelNameUtilsSpec extends BaseSpec {
  import HotelNameUtils._

  "HotelNameUtils" should {
    "split name by words" in {
      HotelNameUtils.splitNameToWords("4 сезона") shouldBe Array("4 сезона")
      HotelNameUtils.splitNameToWords("четыре сезона") shouldBe Array("четыре", "сезона")
      HotelNameUtils.splitNameToWords("m c beach park hotel") shouldBe Array("m c", "beach", "park", "hotel")
      HotelNameUtils.splitNameToWords("a") shouldBe Array("a")
      HotelNameUtils.splitNameToWords("aa") shouldBe Array("aa")
      HotelNameUtils.splitNameToWords("aa b") shouldBe Array("aa", "b")
      HotelNameUtils.splitNameToWords("b aa") shouldBe Array("b aa")
    }
    "split name with only one whitespace" in {
      HotelNameUtils.splitNameToWords(" ") shouldBe Array.empty[String]
    }
    "split name with deprecated name to separate names" in {
      extractExNames("Санаторий Дружба (Бывш. Днепр)") shouldBe List("Санаторий Дружба", "Днепр")
      extractExNames("Von Resort Elite Hotel (Ex. Von Boutique, Ex. Sentido Von Resort)") shouldBe List(
        "Von Resort Elite Hotel",
        "Von Boutique",
        "Sentido Von Resort"
      )
      extractExNames("Sentido Perissia Hotel") shouldBe List("Sentido Perissia Hotel")
    }
    "split lowerCased with deprecated name to separate names" in {
      extractExNames("санаторий дружба (бывш. днепр)") shouldBe List("санаторий дружба", "днепр")
    }
    "split camelCased name" in {
      splitCamelCase("AlmaPamplona - Muga de Beloso") shouldBe "Alma Pamplona - Muga de Beloso"
      splitCamelCase("СССР") shouldBe "С С С Р"
    }

    "split name to separate names" in {
      splitNameToNames("AlmaPamplona - Muga de Beloso (ex. AlmaPamoplona)") shouldBe List(
        "AlmaPamplona - Muga de Beloso",
        "Alma Pamplona - Muga de Beloso",
        "AlmaPamoplona",
        "Alma Pamoplona"
      )
    }

    "not fail on empty string" in {
      splitNameToNames("") shouldBe List("")
    }
  }
}
