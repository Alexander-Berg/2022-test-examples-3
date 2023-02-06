package ru.yandex.tours.filter

import ru.yandex.tours.model.filter.snippet.{TourOperatorFilter, SearchSourceFilter}
import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.serialize.json.CommonJsonSerialization
import ru.yandex.tours.testkit.{BaseSpec, TestData}

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 06.05.15
 */
class TourOperatorFilterSpec extends BaseSpec with TestData {

  val hotel = data.randomHotel()
  val operatorIds = 1 until 20
  val emptyFilter = SearchSourceFilter(TourOperatorFilter.name, Set.empty)
  val bgFilter = SearchSourceFilter(TourOperatorFilter.name, Set(7))
  val bgPegasFilter = SearchSourceFilter(TourOperatorFilter.name, Set(4,7))


  "TourOperatorFilter" should {
    "accept any tour if defined operators is empty" in {
      val tourBuilder = data.randomTour.toBuilder
      for (operatorId <- operatorIds) {
        tourBuilder.getSourceBuilder.setOperatorId(operatorId)
        emptyFilter.fits(tourBuilder.build()) shouldBe true
      }
    }
    "reject tour if tour not contain any of defined operators" in {
      val tourBuilder = data.randomTour.toBuilder
      for (operatorId <- operatorIds.filter(_ != 7)) {
        tourBuilder.getSourceBuilder.setOperatorId(operatorId)
        bgFilter.fits(tourBuilder.build()) shouldBe false
      }
    }
    "accept tour if tour contain any of defined operator" in {
      val tourBuilder = data.randomTour.toBuilder
      tourBuilder.getSourceBuilder.setOperatorId(7)
      bgFilter.fits(tourBuilder.build()) shouldBe true
    }
    "accept hotel snippet if defined operators is empty" in {
      val snippetBuilder = data.randomSnippet.toBuilder.setHotelId(hotel.id)
      emptyFilter.fits(snippetBuilder.build()) shouldBe true
    }
    "reject hotel snippet if snippet not contain any of defined operators" in {
      val snippetBuilder = data.randomSnippet.toBuilder.setHotelId(hotel.id)
      snippetBuilder.clearSource().addSourceBuilder().setPartnerId(Partners.lt.id).setOperatorId(1)
      bgFilter.fits(snippetBuilder.build()) shouldBe false
    }
    "accept hotel snippet if snippet contain any of defined operators" in {
      val snippetBuilder = data.randomSnippet.toBuilder.setHotelId(hotel.id)
      snippetBuilder.clearSource().addSourceBuilder().setPartnerId(Partners.lt.id).setOperatorId(7)
      bgFilter.fits(snippetBuilder.build()) shouldBe true
    }

    "serialize into json (empty)" in {
      val json = CommonJsonSerialization.filtersToJson(Seq(emptyFilter))
      json.toString shouldBe """{}"""
    }
    "serialize into json (bg)" in {
      val json = CommonJsonSerialization.filtersToJson(Seq(bgFilter))
      json.toString shouldBe """{"tour_operator":["7"]}"""
    }
    "serialize into json (multiple operators)" in {
      val json = CommonJsonSerialization.filtersToJson(Seq(TourOperatorFilter(Set(1, 2))))
      json.toString shouldBe """{"tour_operator":["1","2"]}"""
    }
  }
}
