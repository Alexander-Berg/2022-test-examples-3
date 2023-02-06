package ru.yandex.tours.indexer.hotels.parsers

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.tours.model.hotels.HotelsHolder.{HotelType, RawPartnerHotel}
import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.util.{ProtoIO, IO}
import scala.collection.JavaConversions._

class OstrovokV3FormatParserSpec extends WordSpec with Matchers with ScalaFutures {
  "OstrovokV3 parser" should {
    "parse hotels" in {
      val is = getClass.getResourceAsStream("/ostrovokv3_sample.xml")
      val parser = new OstrovokV3FormatParser()
      val file = parser.parse(is).futureValue
      try {
        val hotels = ProtoIO.loadFromFile(file, RawPartnerHotel.PARSER).toVector
        hotels should have size 4
        val hotel = hotels.head
        hotel.hasPoint shouldBe true
        hotel.getPoint.getLatitude shouldBe 40.946220
        hotel.getPoint.getLongitude shouldBe 8.225700
        hotel.getPartnerId shouldBe "7476164"
        hotel.getPartner shouldBe Partners.ostrovokv3.id
        hotel.getPartnerUrl shouldBe "https://ostrovok.ru/rooms/park_hotel_asinara/"
        hotel.getStars shouldBe 3
        hotel.getAddressCount shouldBe 1

        hotel.getNameCount shouldBe 1
        val names = hotel.getNameList.map(n => n.getLang -> n.getValue).toMap
        names("ru") shouldBe "Park Hotel Asinara"

        val address = hotel.getAddressList.map(a => a.getLang -> a).toMap
        address should have size 1
        address("ru").getCountry shouldBe "Италия"
        address("ru").getLocality shouldBe "Сцинтио"
        address("ru").getFullAddress shouldBe "Località Cala Lupo, Сцинтио"

        hotel.hasType shouldBe true
        hotel.getType shouldBe HotelType.HOTEL


        hotel.getAddInfoCount shouldBe 2

        val matching = hotel.getAddInfoList.map(a => a.getName -> a.getValue).toMap
        matching("booking") shouldBe "89738"
        matching("expedia") shouldBe "303086"

        hotel.getRawImagesCount shouldBe 10
        hotel.getRawImages(0) shouldBe "https://cdn.ostrovok.ru/t/1024x768/mec/35/5a/355a5951eadc21f3bc5ab6a26c9a95633306ce95.jpeg"
        hotel.getFeaturesCount shouldBe 3

      } finally {
        IO.deleteFile(file)
      }
    }
  }
}
