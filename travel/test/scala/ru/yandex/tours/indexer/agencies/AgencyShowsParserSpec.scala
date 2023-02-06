package ru.yandex.tours.indexer.agencies

import java.io.ByteArrayInputStream

import org.scalatest.{WordSpec, Matchers}

class AgencyShowsParserSpec extends WordSpec with Matchers {
  "Agency shows parser" should {
    "parse shows" in {
      val raw = """1116050261	http://maps.yandex.ru/org/1116050261	Авиа- и железнодорожные билеты (184108279);Бронирование гостиниц (184106412);Турфирма (184106432)	РИО	Москва, Ленинский просп., 41/2, оф. 462	+7 (495) 943-92-26;+7 (495) 983-59-56	пн-пт 10:00-19:00	 192 863
                 |1163262035	http://maps.yandex.ru/org/1163262035	Турфирма (184106432)	Метро Тур	Москва, Новоспасский пер., 3, корп.2	+7 (499) 653-83-82;+7 (499) 714-70-01	пн-пт 12:00-20:00, сб 14:00-20:00	 89 564
                 |""".stripMargin
      val is = new ByteArrayInputStream(raw.getBytes)
      val map = AgencyShowsParser.parse(is)
      map should have size 2
      map.contains(1116050261) shouldBe true
      map.contains(1163262035) shouldBe true
      map(1116050261) should be (192863)
      map(1163262035) should be (89564)
      is.available() should be (0)
    }
  }
}
