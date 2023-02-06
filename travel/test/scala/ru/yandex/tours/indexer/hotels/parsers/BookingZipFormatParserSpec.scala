package ru.yandex.tours.indexer.hotels.parsers

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.tours.indexer.hotels.parsers.booking.StaticHotel2RegionRetriever
import ru.yandex.tours.model.hotels.HotelsHolder.RawPartnerHotel
import ru.yandex.tours.testkit.TestData
import ru.yandex.tours.util.{IO, ProtoIO}

import scala.concurrent.ExecutionContext.Implicits.global

class BookingZipFormatParserSpec extends WordSpec
with Matchers
with TestData
with ScalaFutures
with IntegrationPatience {
  "Booking zip format parser" should {
    "parse zipped feeds" in {
      val regions = new StaticHotel2RegionRetriever(Map.empty)
      val parser = new BookingZipFormatParser(data.regionTree, data.iso2country, regions)
      val is = getClass.getResourceAsStream("/booking.zip")
      val resultFile = parser.parse(is).futureValue
      val hotels = ProtoIO.loadFromFile(resultFile, RawPartnerHotel.PARSER).toVector
      hotels should have size 2
      IO.deleteFile(resultFile)
    }
  }
}
