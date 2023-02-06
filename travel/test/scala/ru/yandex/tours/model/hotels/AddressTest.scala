package ru.yandex.tours.model.hotels

import _root_.ru.yandex.tours.testkit.BaseSpec
import ru.yandex.tours.model.Languages._

/* @author berkut@yandex-team.ru */
class AddressTest extends BaseSpec {
  val ruAddress = Address(ru, country = "Россия", locality = "Питер Святой", fullAddress = "Пушкина")
  val enAddress = Address(en, country = "Russia", locality = "spb", fullAddress = "Pushkin")


  "Address" should {
    "serialize" in {
      val address = Address(ru)
      address.serialize shouldBe "ru\t\t\t\t\t\t\t"
    }
    "deserialize single" in {
      val addresses = Address.deserialize("ru\t\t\t\t\t\t\t")
      addresses should have size 1
      addresses(ru) shouldBe Address.empty(ru)
    }

    "serialize map" in {
      val ruAddress = Address(ru, country = "Россия", locality = "Питер\tСвятой", fullAddress = "Пушкина")
      val enAddress = Address(en, country = "Russia", locality = "spb", fullAddress = "Pushkin")
      val map = Seq(ruAddress, enAddress).map(t => t.lang -> t).toMap
      val serialized = Address.serialize(map)
      val parts = serialized.split(" @@ ")
      parts should contain ("ru\tРоссия\t\tПитер Святой\t\t\t\tПушкина")
      parts should contain ("en\tRussia\t\tspb\t\t\t\tPushkin")
    }

    "deserialize map" in {
      val map = Address.deserialize("en\tRussia\t\tspb\t\t\t\tPushkin @@ ru\tРоссия\t\tПитер\t\t\t\tПушкина")

      map(ru) shouldBe Address(ru, country = "Россия", locality = "Питер", fullAddress = "Пушкина")
      map(en) shouldBe Address(en, country = "Russia", locality = "spb", fullAddress = "Pushkin")
    }
  }
}
