package ru.yandex.tours.geo

import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import ru.yandex.tours.model.MapRectangle
import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 19.02.16
 */
class GeohashSpec extends BaseSpec with PropertyChecks {

  private def checkDecode(geohash: Long, lat: Double, lon: Double) = {
    val (decodedLat, decodedLon) = Geohash.decode(geohash)
    decodedLat shouldBe lat +- 1e-6
    decodedLon shouldBe lon +- 1e-6
  }

  val latGen = Gen.chooseNum(-90d, 90d)
  val lonGen = Gen.chooseNum(-180d, 180d)

  "Geohash" should {
    "encode coordinates to geohash" in {
      Geohash.encode(0d, 0d) shouldBe 4611686018427387904L
      Geohash.encode(90d, 180d) shouldBe 9223372036854775807L
      Geohash.encode(37.51d, -88.42d) shouldBe -1940185115011107268L
    }

    "decode geohash to coordinates" in {
      checkDecode(4611686018427387904L, 0d, 0d)
      checkDecode(9223372036854775807L, 90d, 180d)
      checkDecode(-1940185115011107268L, 37.51d, -88.42d)
    }

    "encode and decode" in {
      forAll(latGen, lonGen) { (lat: Double, lon: Double) =>
        val encoded = Geohash.encode(lat, lon)
        checkDecode(encoded, lat, lon)
      }
    }

    "be comparable" in {
      forAll(latGen, lonGen) { (lat: Double, lon: Double) =>
        val span = MapRectangle.byCenterAndSpan(lon, lat, 5d, 5d)
        val min = Geohash.encode(span.minLat, span.minLon)
        val max = Geohash.encode(span.maxLat, span.maxLon)
        withClue(span) {
          min should be < max
        }
      }
    }
  }
}
