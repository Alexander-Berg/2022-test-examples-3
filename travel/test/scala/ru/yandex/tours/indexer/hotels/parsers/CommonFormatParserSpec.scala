package ru.yandex.tours.indexer.hotels.parsers

import java.io.ByteArrayInputStream

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.tours.model.Languages
import ru.yandex.tours.model.hotels.HotelsHolder.RawPartnerHotel
import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.util.ProtoIO

import scala.collection.JavaConversions._

class CommonFormatParserSpec extends WordSpec with Matchers with ScalaFutures {
  "Common format parser" should {
    "parse hotels" in {
      val parser = new CommonFormatParser(Partners.sodis, Some("PartnerRegionID"), Languages.en, {
        case ("free_wifi", "true") => ("internet_in_hotel", "free_wi_fi_in_public_areas")
      })
      val resultFile = parser.parse(new ByteArrayInputStream(testXml.getBytes)).futureValue
      val result = ProtoIO.loadFromFile(resultFile, RawPartnerHotel.PARSER).toVector
      result should have size 1
      val hotel = result.head
      hotel.getNameCount shouldBe 1
      hotel.getName(0).getLang shouldBe "en"
      hotel.getName(0).getValue shouldBe "Cap Juluca"
      hotel.getAddressCount shouldBe 2
      val addresses = hotel.getAddressList.map(ad => ad.getLang -> ad).toMap
      addresses("ru").getCountry shouldBe "Ангилья"
      addresses("ru").getLocality shouldBe "Ангилья город"
      addresses("ru").hasAdminName shouldBe false
      addresses("ru").hasFullAddress shouldBe false
      addresses("ru").hasHouse shouldBe false
      addresses("ru").hasSubAdminName shouldBe false
      addresses("ru").hasStreet shouldBe false

      addresses("en").getCountry shouldBe "Anguilla"
      addresses("en").getLocality shouldBe "Anguilla city"
      addresses("en").hasAdminName shouldBe false
      addresses("en").hasFullAddress shouldBe false
      addresses("en").hasHouse shouldBe false
      addresses("en").hasSubAdminName shouldBe false
      addresses("en").hasStreet shouldBe false

      hotel.hasPoint shouldBe true
      hotel.getFeaturesCount shouldBe 1
      hotel.getFeatures(0).getName shouldBe "internet_in_hotel"
      hotel.getFeatures(0).getValue shouldBe "free_wi_fi_in_public_areas"
      hotel.getPoint.getLatitude shouldBe 18.165792
      hotel.getPoint.getLongitude shouldBe -63.144608
      hotel.getPartnerUrl shouldBe "http://www.sodis.ru/disp?s=hotel&id=1195942"
      hotel.getRawImagesCount shouldBe 6
      hotel.getRawImages(0) shouldBe "http://www.sodis.ru/imageservlet?id=103952215&id2=103952216"
      hotel.getPhoneCount shouldBe 0

      hotel.hasRegionId shouldBe true
      hotel.getRegionId shouldBe "7294"

      hotel.getStars shouldBe 5
    }
  }

  val testXml =
    """
      |<companies version="2.1">
      |    <company>
      |        <company-id>1195942</company-id>
      |        <name lang="en">Cap Juluca</name>
      |        <country lang="en">Anguilla</country>
      |        <country lang="ru">Ангилья</country>
      |        <locality-name lang="en">Anguilla city</locality-name>
      |        <locality-name lang="ru">Ангилья город</locality-name>
      |        <coordinates>
      |            <lon>-63.144608</lon>
      |            <lat>18.165792</lat>
      |        </coordinates>
      |        <add-url>http://www.sodis.ru/disp?s=hotel&amp;id=1195942</add-url>
      |        <rubric-id>184106414</rubric-id>
      |        <photos gallery-url="http://www.sodis.ru/disp?s=images&amp;id=3854645">
      |            <photo url="http://www.sodis.ru/imageservlet?id=103952215&amp;id2=103952216"/>
      |            <photo url="http://www.sodis.ru/imageservlet?id=103956703&amp;id2=103956704"/>
      |            <photo url="http://www.sodis.ru/imageservlet?id=103952229&amp;id2=103952230"/>
      |            <photo url="http://www.sodis.ru/imageservlet?id=103952236&amp;id2=103952237"/>
      |            <photo url="http://www.sodis.ru/imageservlet?id=103956409&amp;id2=103956410"/>
      |            <photo url="http://www.sodis.ru/imageservlet?id=103952243&amp;id2=103952244"/>
      |        </photos>
      |        <add-info>
      |            <param>
      |                <name>PartnerRegionID</name>
      |                <value>7294</value>
      |            </param>
      |        </add-info>
      |        <add-info>
      |            <param>
      |                <name>расположение</name>
      |                <value>на юго-западном побережье острова. Международный аэропорт расположен на острове Cен-Мартен в 10
      |                    км к югу от Ангильи. Путь от Cен-Мартена до отеля на катере занимает 15 минут, на самолете 10 минут.
      |                </value>
      |            </param>
      |            <param>
      |                <name>в отеле</name>
      |                <value>2 ресторана, бар, открытый бассейн, 3 теннисных корта, фитнес-центр, центр водного спорта,
      |                    Спа-центр, массаж, услуги няни, детский клуб, прокат автомобилей, доставка свежей прессы, магазины,
      |                    приветственный напиток, развлекательные программы, библиотека.
      |                </value>
      |            </param>
      |            <param>
      |                <name>в номерах</name>
      |                <value>кондиционер, вентилятор, телефон, холодильник, ванная комната с душевой кабинкой, биде.</value>
      |            </param>
      |        </add-info>
      |        <feature-boolean name="free_wifi" value="1"/>
      |        <feature-enum-single name="star" value="five"/>
      |    </company>
      |</companies>
    """.stripMargin
}
