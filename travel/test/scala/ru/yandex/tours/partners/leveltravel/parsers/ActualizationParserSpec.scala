package ru.yandex.tours.partners.leveltravel.parsers

import org.scalatest.Matchers._
import org.scalatest.WordSpecLike
import ru.yandex.tours.geo.mapping.GeoMappingHolder

import scala.collection.JavaConversions._

class ActualizationParserSpec extends WordSpecLike {
  "Flight parser" should {
    val geoMappingHolder = GeoMappingHolder(Map.empty, Map((5, 213) -> "2", (5, 13457) -> "1186"), Map.empty, Map.empty)
    val parser = new ActualizationParser(geoMappingHolder)
    "parse actualization" in {
      val json = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/lt/flights.json")).getLines().mkString
      val result = parser.parse(json)
      result.isSuccess shouldBe true
      result.get.isDefined shouldBe true
      val tourActualization = result.get.get
      val flight = tourActualization.getFlights(0)
      val to = flight.getTo
      tourActualization.hasWithTransfer shouldBe true
      tourActualization.getWithTransfer shouldBe false
      to.getIsDirect shouldBe true
      tourActualization.getFuelCharge shouldBe 5874
      tourActualization.hasInfantPrice shouldBe true
      tourActualization.getInfantPrice shouldBe 5799
      to.getPointCount shouldBe 2
      val origin = to.getPointList.head
      val dest = to.getPointList.reverse.head
      origin.getAirport shouldBe "Домодедово"
      origin.getGeoId shouldBe 213
      origin.hasArrival shouldBe false
      origin.getDeparture shouldBe "2015-06-03T00:00:00+00:00"
      dest.getAirport shouldBe "Тиват"
      dest.getGeoId shouldBe 13457
      dest.hasDeparture shouldBe false
      dest.getArrival shouldBe "2015-06-03T05:00:00+00:00"
    }
    "fail with error if no mapping found" in {
      val json = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/lt/flights.json")).getLines().mkString
      val patched = json.replaceAllLiterally("\"id\": 2", "\"id\": 116592")
      val result = parser.parse(patched)
      result.isSuccess shouldBe false
      result.failed.get.getMessage should include ("No valid flights in actualization")
    }
    "fallback to geobase if no mapping found" in {
      val json = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/lt/flights.json")).getLines().mkString
      val patched = json.replaceAllLiterally("\"id\": 2", "\"id\": 999")
      val result = parser.parse(patched)
      result.isSuccess shouldBe true
    }
    "parse city ids represented as strings" in {
      val json = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/lt/flights.json")).getLines().mkString
      val patched = json.replaceAllLiterally("\"id\": 2", "\"id\": \"2\"")
      val result = parser.parse(patched)
      result.isSuccess shouldBe true
    }
  }
}
