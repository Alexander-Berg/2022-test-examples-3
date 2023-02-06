package ru.yandex.tours.geo.mapping

import java.io.ByteArrayInputStream

import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 17.08.15
 */
class GeoMappingSpec extends BaseSpec {
  "GeoMapping" should {
    "parse comma separated partner codes" in {
      val mapping = CityMapping.parse(new ByteArrayInputStream(
        """5	2	2,3,4
          |7	213	5,6,7
          |123	213	11232""".stripMargin.getBytes))

      mapping.isKnown(2) shouldBe true
      mapping.isKnown(213) shouldBe true
      mapping.isKnown(3) shouldBe false

      mapping.getPartnerRegion(Partners.lt, 2) shouldBe Some("2,3,4")
      mapping.getPartnerRegion(Partners.lt, 213) shouldBe None
      mapping.getPartnerRegion(Partners.sunmar, 2) shouldBe None
      mapping.getPartnerRegion(Partners.sunmar, 213) shouldBe Some("5,6,7")

      mapping.getYaRegion(Partners.lt, "2") shouldBe Some(2)
      mapping.getYaRegion(Partners.lt, "3") shouldBe Some(2)
      mapping.getYaRegion(Partners.lt, "5") shouldBe None
      mapping.getYaRegion(Partners.sunmar, "2") shouldBe None
      mapping.getYaRegion(Partners.sunmar, "3") shouldBe None
      mapping.getYaRegion(Partners.sunmar, "5") shouldBe Some(213)
      mapping.getYaRegion(Partners.sunmar, "7") shouldBe Some(213)
    }
  }
}
