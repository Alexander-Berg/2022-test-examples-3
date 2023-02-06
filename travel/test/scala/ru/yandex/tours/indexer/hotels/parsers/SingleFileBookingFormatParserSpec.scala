package ru.yandex.tours.indexer.hotels.parsers

import java.io.ByteArrayInputStream

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{LoneElement, WordSpec, Matchers}
import ru.yandex.tours.indexer.hotels.parsers.booking.StaticHotel2RegionRetriever
import ru.yandex.tours.model.hotels.HotelsHolder.RawPartnerHotel
import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.testkit.TestData
import ru.yandex.tours.util.{ProtoIO, IO}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._

class SingleFileBookingFormatParserSpec extends WordSpec with Matchers with TestData
  with ScalaFutures with IntegrationPatience with LoneElement {

  "Booking parser" should {
    "parser feed" in {
      val retriever = new StaticHotel2RegionRetriever(Map("375281" -> "some_region"))
      val parser = new SingleFileBookingFormatParser(data.regionTree, data.iso2country, retriever)
      val is = new ByteArrayInputStream(xml.getBytes)
      val resultFile = parser.parse(is).futureValue
      val hotels = ProtoIO.loadFromFile(resultFile, RawPartnerHotel.PARSER).toVector
      hotels should have size 1
      val hotel = hotels.head
      hotel.getPartnerId shouldBe "375281"
      hotel.getNameCount shouldBe 1
      hotel.getName(0).getLang shouldBe "en"
      hotel.getName(0).getValue shouldBe "Aparthotel Am See"
      hotel.getPoint.getLongitude shouldBe 12.2828919
      hotel.getPoint.getLatitude shouldBe 53.4713620
      hotel.getPhoneCount shouldBe 1
      hotel.getPhone(0) shouldBe "+49 38735850"
      hotel.getPartnerUrl shouldBe "http://www.booking.com/hotel/de/aparthotel-fulda.html"
      hotel.getStars shouldBe 3
      hotel.getRawImagesCount shouldBe 7
      hotel.getRegionId shouldBe "some_region"
      hotel.getRawImages(0) shouldBe "http://q-ec.bstatic.com/images/hotel/840x460/198/19832698.jpg"
      hotel.getPartner shouldBe Partners.booking.id
      hotel.getAddressCount shouldBe 2
      hotel.getFeaturesCount shouldBe 9
      hotel.getFeaturesList.find(_.getName == "pool_type").map(_.getValue) shouldBe Some("indoor_pool")
      val address = hotel.getAddressList.map(a => a.getLang -> a).toMap
      address("ru").getCountry shouldBe "Германия"
      address("ru").getLocality shouldBe "Плау-ам-Зее"
      address("ru").getFullAddress shouldBe "Kantor-Ehrich-Str. 3D"
      address("en").getCountry shouldBe "Germany"
      address("en").getLocality shouldBe "Plau am See"
      address("en").getFullAddress shouldBe "Kantor-Ehrich-Str. 3D"
      IO.deleteFile(resultFile)
    }

    "multiply rating by 2" in {
      val retriever = new StaticHotel2RegionRetriever(Map.empty)
      val parser = new SingleFileBookingFormatParser(data.regionTree, data.iso2country, retriever)
      val is = new ByteArrayInputStream(xml.getBytes)
      val resultFile = parser.parse(is).futureValue
      try {
        val hotels = ProtoIO.loadFromFile(resultFile, RawPartnerHotel.PARSER).toVector

        val hotel = hotels.loneElement

        hotel should have('rating (8.4))
      } finally {
        IO.deleteFile(resultFile)
      }
    }
  }

  val xml =
    """
      |<listings xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      |  <!-- version : 1.93 -->
      |  <!-- hostname : feedsapp-201 -->
      |  <!-- script_started : 2016-01-10 20:41 -->
      |  <!-- deploy : cron-20160108-163223 -->
      |  <listing>
      |    <id>375281</id>
      |    <name>Aparthotel Am See</name>
      |    <address format="simple">
      |      <component name="addr1">Kantor-Ehrich-Str. 3D</component>
      |      <component name="city">Plau am See</component>
      |      <component lang="ru" name="city">Плау-ам-Зее</component>
      |      <component name="zip">19395</component>
      |    </address>
      |    <country>DE</country>
      |    <latitude>53.4713620</latitude>
      |    <longitude>12.2828919</longitude>
      |    <phone type="main">+49 38735850</phone>
      |    <phone type="fax">+49 3873542042</phone>
      |    <category type="aparthotel"/>
      |    <attributes>
      |      <attr name="num_rooms">18</attr>
      |      <attr name="class">3</attr>
      |      <attr name="rating">4.2</attr>
      |      <attr name="num_reviews">122</attr>
      |      <attr name="check_in">
      |        <from>15:00</from>
      |        <until>22:00</until>
      |      </attr>
      |      <attr name="check_out">
      |        <from>07:00</from>
      |        <until>10:00</until>
      |      </attr>
      |    </attributes>
      |    <link>http://www.booking.com/hotel/de/aparthotel-fulda.html?aid=809127</link>
      |        <link lang="en">http://www.booking.com/hotel/de/aparthotel-fulda.en.html?aid=809127</link>
      |    <link lang="ru">http://www.booking.com/hotel/de/aparthotel-fulda.ru.html?aid=809127</link>
      |    <link lang="tr">http://www.booking.com/hotel/de/aparthotel-fulda.tr.html?aid=809127</link>
      |    <canonical>http://www.booking.com/hotel/de/aparthotel-fulda.html</canonical>
      |    <canonical lang="en">http://www.booking.com/hotel/de/aparthotel-fulda.en.html</canonical>
      |    <canonical lang="ru">http://www.booking.com/hotel/de/aparthotel-fulda.ru.html</canonical>
      |    <canonical lang="tr">http://www.booking.com/hotel/de/aparthotel-fulda.tr.html</canonical>
      |    <facilities>
      |      <facility name="non_smoking_rooms">1</facility>
      |      <facility name="facilities_for_disabled">1</facility>
      |      <facility name="indoor_pool">1</facility>
      |      <facility name="spa_and_wellness_centre">1</facility>
      |      <facility name="pets_allowed">1</facility>
      |      <facility name="wifi">1</facility>
      |      <facility name="terrace">1</facility>
      |      <facility name="bicycle_rental">1</facility>
      |      <facility name="restaurant">1</facility>
      |      <facility name="parking">1</facility>
      |    </facilities>
      |    <payments>
      |      <creditcard name="Visa">1</creditcard>
      |      <creditcard name="Euro/Mastercard">1</creditcard>
      |    </payments>
      |    <content>
      |     <images>
      |         <image type="main" url="http://bstatic.com/images/hotel/org/198/19832698.jpg">1</image>
      |         <image type="photo" url="http://bstatic.com/images/hotel/org/198/19832726.jpg">1</image>
      |         <image type="photo" url="http://bstatic.com/images/hotel/org/407/40704369.jpg">1</image>
      |         <image type="photo" url="http://bstatic.com/images/hotel/org/198/19844258.jpg">1</image>
      |         <image type="photo" url="http://bstatic.com/images/hotel/org/198/19832789.jpg">1</image>
      |         <image type="photo" url="http://bstatic.com/images/hotel/org/753/7539110.jpg">1</image>
      |         <image type="photo" url="http://bstatic.com/images/hotel/org/198/19832750.jpg">1</image>
      |      </images>
      |    </content>
      |  </listing>
      |</listings>
    """.stripMargin
}
