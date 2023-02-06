package ru.yandex.tours.indexer.agencies

import java.io.ByteArrayInputStream

import org.scalatest.{WordSpec, Matchers}

class SubstitutionPhonesParserSpec extends WordSpec with Matchers {
  "Substitution Phones Parser" should {
    "parse shows" in {
      val raw = """1116050261	+7 (495) 943-92-26
                  |1163262035	+7 (499) 653-83-82
                  |""".stripMargin
      val is = new ByteArrayInputStream(raw.getBytes)
      val map = SubstitutionPhonesParser.parse(is)
      map should have size 2
      map.contains(1116050261) shouldBe true
      map.contains(1163262035) shouldBe true
      map(1116050261) should be ("+7 (495) 943-92-26")
      map(1163262035) should be ("+7 (499) 653-83-82")
      is.available() should be (0)
    }
  }

}
