package ru.yandex.tours.indexer.hotels.parsers

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import ru.yandex.tours.model.hotels.HotelsHolder.RawPartnerHotel
import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import ru.yandex.tours.util.ProtoIO

import scala.collection.JavaConverters._

class BackaFormatParserSpec extends BaseSpec with TestData with ScalaFutures with IntegrationPatience {
  "Backa hotel xml parser" should {
    "parse hotels" in {
      val parser = new BackaFormatParser(data.regionTree)
      val file = parser.parse(getClass.getResourceAsStream("/backa-export2.tar")).futureValue
      val hotels = ProtoIO.loadFromFile(file, RawPartnerHotel.PARSER).toList
      hotels should have size 1
      val hotel = hotels.head
      hotel should have ('partnerId ("1028201498"))
      hotel should have ('partner (Partners.backa.id))
      hotel.getNameList should have size 2
      val nameMap = hotel.getNameList.asScala.map(x => x.getLang -> x.getValue).toMap
      nameMap.get("ru") shouldBe Some("Тыгын Дархан")
      nameMap.get("en") shouldBe Some("Tygyn Darkhan")
      hotel.getHotelUrl shouldBe "http://www.tygyn.ru"
      hotel.getPoint should have ('longitude (129.734))
      hotel.getPoint should have ('latitude (62.0263))
      hotel.getPhoneList should have size 2
      hotel.getPhone(0) shouldBe "+7 (4112) 43-51-09"
      hotel.getPhone(1) shouldBe "+7 (4112) 43-51-11"
      hotel should have ('regionId ("74"))
      hotel.getSynonymsList should have size 1
      hotel.getSynonyms(0).getValue shouldBe "Тыгын Дархан 2"
      hotel.getAddressList should have size 2
      hotel.getRawImagesCount shouldBe 0
      hotel.getFeaturesCount shouldBe 15
      hotel.getStars shouldBe 4
      hotel.getAddInfoCount shouldBe 9
      hotel.getAddInfoList.asScala.find(_.getName == "101hotels").map(_.getValue) should contain ("http://101hotels.ru/main/cities/Yakutsk/Gostinitsa_Tigin_Darhan.html")
      hotel.getPartnerUrl shouldBe "https://maps.yandex.ru/org/1028201498"
      val addressMap = hotel.getAddressList.asScala.map(x => x.getLang -> x).toMap
      addressMap("ru").getCountry shouldBe "Россия"
      addressMap("ru").hasAdminName shouldBe false
      addressMap("ru").getLocality shouldBe "Якутск"
      addressMap("ru").hasStreet shouldBe false
      addressMap("ru").hasHouse shouldBe false
      addressMap("ru").getFullAddress shouldBe "Якутск, ул. Аммосова, 9"
      addressMap("en").getCountry shouldBe "Russia"
      addressMap("en").hasAdminName shouldBe false
      addressMap("en").getLocality shouldBe "Yakutsk"
      addressMap("en").hasStreet shouldBe false
      addressMap("en").hasHouse shouldBe false
      addressMap("en").getFullAddress shouldBe "Yakutsk, ul. Ammosova, 9"
    }
  }
}
