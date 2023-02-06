package ru.yandex.tours.model.hotels

import org.junit.Assert._
import ru.yandex.tours.testkit.BaseSpec
import ru.yandex.tours.model.Languages._

/* @author berkut@yandex-team.ru */

class NameTest extends BaseSpec {

  "Name" should {
    "serialize" in {
      val name = Name(ru, "Гостиница\tЦентральная")
      name.serialize shouldBe "ru\tГостиница Центральная"
    }
    "deserialize" in {
      val name = Name.deserialize("ru\tГостиница Центральная")
      name(ru) shouldBe Name(ru, "Гостиница Центральная")
    }
    "serialize name map" in {
      val names = Seq(Name(ru, "Гостиница Центральная"), Name(en, "Central Hotel")).map(t => t.lang -> t).toMap
      val serialized = Name.serialize(names)
      assertTrue(serialized.contains("ru\tГостиница Центральная"))
      assertTrue(serialized.contains("en\tCentral Hotel"))
      serialized shouldBe "ru\tГостиница Центральная\ten\tCentral Hotel"
    }
    "deserialize name map" in {
      val map = Name.deserialize("ru\tГостиница Центральная\ten\tCentral Hotel")
      map(ru) shouldBe Name(ru, "Гостиница Центральная")
      map(en) shouldBe Name(en, "Central Hotel")
    }
  }

}
