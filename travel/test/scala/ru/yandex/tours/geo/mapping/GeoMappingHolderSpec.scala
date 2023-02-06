package ru.yandex.tours.geo.mapping

import java.io.File

import org.scalatest.Matchers._
import org.scalatest.WordSpecLike
import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.testkit.{TemporaryDirectory, TestData}
import ru.yandex.tours.util.file._

/* @author berkut@yandex-team.ru */

class GeoMappingHolderSpec extends WordSpecLike with TemporaryDirectory with TestData {

  var cityMapping: File = null
  var countryMapping: File = null
  var departures: File = null
  var airports: File = null

  override def beforeAll(): Unit = {
    super.beforeAll()
    cityMapping = tempDir.newFile("city_mapping")
    cityMapping.writeLines(
      "5\t2\tSaint-Petersburg",
      "5\t213\tMoscow",
      "432\t213\tMoscow"
    )
    countryMapping = tempDir.newFile("country_mapping")
    countryMapping.writeLines(
      "5\t225\tRussia",
      "5\t143\tUkraine",
      "43\t142\tUkraine"
    )
    departures = tempDir.newFile("departures")
    departures.writeLines(
      "5\t2\tSaint-Petersburg",
      "34\t2\tSaint-Petersburg",
      "5\t213\tMoscow"
    )
    airports = tempDir.newFile("airports")
    airports.writeLines(
      "5\t11508\tIstanbul",
      "123\t11508\tIstanbul",
      "5\t213\tMoscow"
    )
  }

  "Geo Mapping Holder" must {
    "be read from file" in {
      val holder = GeoMappingHolder.parse(cityMapping, countryMapping, departures, airports)

      holder.isKnownDestCountry(225) shouldBe true

      holder.getPartnerCity(Partners.lt, 213) shouldBe Some("Moscow")
      holder.getPartnerCountry(Partners.lt, 143) shouldBe Some("Ukraine")
    }

    "know cities" in {
      val holder = GeoMappingHolder.parse(cityMapping, countryMapping, departures, airports)
      holder.isKnownDestCity(2) shouldBe true
      holder.isKnownDestCity(213) shouldBe true

      holder.isKnownDestCity(0) shouldBe false
      holder.isKnownDestCity(225) shouldBe false
      holder.isKnownDestCity(143) shouldBe false
      holder.isKnownDestCity(9999) shouldBe false
    }

    "know countries" in {
      val holder = GeoMappingHolder.parse(cityMapping, countryMapping, departures, airports)
      holder.isKnownDestCountry(225) shouldBe true
      holder.isKnownDestCountry(143) shouldBe true

      holder.isKnownDestCountry(0) shouldBe false
      holder.isKnownDestCountry(2) shouldBe false
      holder.isKnownDestCountry(213) shouldBe false
      holder.isKnownDestCountry(9999) shouldBe false
    }

    "translate cities" in {
      val holder = GeoMappingHolder.parse(cityMapping, countryMapping, departures, airports)

      holder.getPartnerCity(Partners.lt, 213) shouldBe Some("Moscow")
      holder.getPartnerCity(Partners.lt, 2) shouldBe Some("Saint-Petersburg")
      holder.getPartnerCity(Partners.lt, 0) shouldBe None
      holder.getPartnerCity(Partners.lt, 225) shouldBe None
      holder.getPartnerCity(Partners.lt, 143) shouldBe None
    }

    "translate countries" in {
      val holder = GeoMappingHolder.parse(cityMapping, countryMapping, departures, airports)

      holder.getPartnerCountry(Partners.lt, 225) shouldBe Some("Russia")
      holder.getPartnerCountry(Partners.lt, 143) shouldBe Some("Ukraine")
      holder.getPartnerCountry(Partners.lt, 0) shouldBe None
      holder.getPartnerCountry(Partners.lt, 2) shouldBe None
      holder.getPartnerCountry(Partners.lt, 213) shouldBe None
    }

    "translate airports" in {
      val holder = GeoMappingHolder.parse(cityMapping, countryMapping, departures, airports)
      holder.getAirport(Partners.lt, "Istanbul") shouldBe Some(11508)
      holder.getAirport(Partners.lt, "Moscow") shouldBe Some(213)
      holder.getAirport(Partners.lt, "Saint-Petersburg") shouldBe None
    }

    "check for existing departure cities" in {
      val holder = GeoMappingHolder.parse(cityMapping, countryMapping, departures, airports)
      holder.isDepartureCity(2) shouldBe true
      holder.isDepartureCity(213) shouldBe true
      holder.isDepartureCity(110) shouldBe false
    }

    "not intersect city and country mappings" in {
      for (regionId <- data.geoMapping.countryGeoIds) {
        withClue("Region: " + regionId) {
          data.geoMapping.isKnownDestCity(regionId) shouldBe false
        }
      }
    }
    "contain regions from geobase" in {
      for (regionId <- data.geoMapping.countryGeoIds ++ data.geoMapping.cityGeoIds) {
        if (data.regionTree.region(regionId).isEmpty) {
          fail(s"Region from mapping $regionId not found in geobase")
        }
      }
    }
  }
}
