package ru.yandex.tours.geo

import ru.yandex.tours.geo.base.Region
import ru.yandex.tours.geo.base.region.{Tree, Types}
import ru.yandex.tours.model.{LocalizedString, MapRectangle}
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import ru.yandex.tours.model.Languages._
import shapeless._

/* @author berkut@yandex-team.ru */

class DeparturesSpec extends BaseSpec with TestData {
  val tree = new Tree(Iterable(
    Region(10, Types.Country, 100, LocalizedString(Map(ru -> "country")), "", "", "", "", "", Set.empty, 0, 30, 60, MapRectangle.empty),
    Region(1, Types.Other, 10, LocalizedString(Map(ru -> "test")), "", "", "", "", "", Set.empty, 0, 30, 60, MapRectangle.empty),
    Region(2, Types.Other, 1, LocalizedString(Map(ru -> "test-child")), "", "", "", "", "", Set.empty, 0, 31, 61, MapRectangle.empty),
    Region(3, Types.Other, 1, LocalizedString(Map(ru -> "test-child-2")), "", "", "", "", "", Set.empty, 0, 30.5, 60.5, MapRectangle.empty),
    Region(8, Types.Country, 1, LocalizedString(Map(ru -> "some-country")), "", "", "", "", "", Set.empty, 0, 30.5, 60.5, MapRectangle.empty)
  ))

  val departures = Map(
    7 -> Seq(8, 9)
  )

  val departuresToCountry = Map(
    (7, 8) -> Seq(10, 11)
  )

  val holder = new Departures(departures, departuresToCountry, tree, Set(2, 3))

  val realHolder = Departures.fromFile(root / "departure_stats.tsv", data.regionTree :: data.geoMapping :: HNil)

  val yakutsk = 74
  val thailand = 995
  val egypt = 1056
  val novosibirsk = 65
  val moscow = 213
  val astrahan = 37

  "Departures" should {
    "getNearestAirport" should {
      "return nearest airport for known region" in {
        holder.getNearestAirport(1) shouldBe 3
      }

      "return same region if its known departure city" in {
        holder.getNearestAirport(2) shouldBe 2
        holder.getNearestAirport(3) shouldBe 3
      }

      "return Moscow for unknown region" in {
        holder.getNearestAirport(34) shouldBe 213
      }
    }

    "getBestDeparture" should {
      "return best departure" in {
        holder.getBestDeparture(7) shouldBe 8
      }
      "fallback to nearest airport" in {
        holder.getBestDeparture(1) shouldBe 3
      }
      "return itself for known region" in {
        holder.getBestDeparture(1) shouldBe 3
      }
    }

    "getBestDeparture with country" should {
      "return best departure for country" in {
        holder.getBestDeparture(7, 8) shouldBe 10
      }
      "fallback to general best departure" in {
        holder.getBestDeparture(7, 9) shouldBe 8
      }
      "fallback to nearest airport" in {
        holder.getBestDeparture(1, 8) shouldBe 3
      }
      "return itself for known region" in {
        holder.getBestDeparture(1) shouldBe 3
      }
    }

    "ignore unknown airports, but tell about them" in {
      var containsUnknown = false
      new Departures(Map.empty, Map.empty, tree, Set(2,3,4)) {
        override protected def onUnknownAirport(id: Int) = containsUnknown = true
      }
      containsUnknown shouldBe true
    }
  }
  "Departures with real data" should {
    "return itself for known regions" in {
      for (geoId <- data.geoMapping.departuresGeoIds) {
        realHolder.getBestDeparture(geoId) shouldBe geoId
      }
    }
    "return moscow for all foreign regions" in {
      def inRussia(region: Region): Boolean = data.regionTree.pathToRoot(region).exists(_.id == 225)
      for (region <- data.regionTree.regions if !inRussia(region)) {
        realHolder.getBestDeparture(region.id) shouldBe 213
      }
    }
    "find departure for Yakutsk to specific country" in {
      realHolder.getBestDeparture(yakutsk, thailand) shouldBe novosibirsk
      realHolder.getBestDeparture(yakutsk, egypt) shouldBe moscow
    }
    "find best departure for Yakutsk" in {
      realHolder.getBestDeparture(yakutsk) shouldBe moscow
    }
    "find best departure for Astrahan to Egypt" in {
      realHolder.getBestDeparture(astrahan, egypt) shouldBe moscow
    }
    "find best departure for Astrahan" in {
      realHolder.getBestDeparture(astrahan) shouldBe astrahan
    }
    "always return non-empty list of departures" in {
      for (r <- data.regionTree.regions) {
        withClue("Region: " + r.id) {
          realHolder.getDepartures(r.id) should not be empty
        }
      }
    }
  }
}