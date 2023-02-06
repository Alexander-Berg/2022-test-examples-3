package ru.yandex.tours.filter

import ru.yandex.tours.model.BaseModel.Pansion
import ru.yandex.tours.model.filter.snippet.PansionFilter
import ru.yandex.tours.serialize.json.CommonJsonSerialization
import ru.yandex.tours.testkit.{BaseSpec, TestData}

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 06.05.15
 */
class PansionFilterSpec extends BaseSpec with TestData {
  val hotel = data.randomHotel()
  val emptyFilter = new PansionFilter(Set.empty)
  val aiFilter = new PansionFilter(Set(Pansion.AI))
  val aiFbFilter = new PansionFilter(Set(Pansion.AI, Pansion.FB))

  "PansionFilter" should {
    "accept any tour if defined pansions is empty" in {
      val tourBuilder = data.randomTour.toBuilder
      for (pansion <- Pansion.values()) {
        tourBuilder.setPansion(pansion)
        emptyFilter.fits(tourBuilder.build()) shouldBe true
      }
    }
    "reject tour if tour not contain any of defined pansions" in {
      val tourBuilder = data.randomTour.toBuilder
      for (pansion <- Pansion.values().filter(_ != Pansion.AI)) {
        tourBuilder.setPansion(pansion)
        aiFilter.fits(tourBuilder.build()) shouldBe false
      }
    }
    "accept tour if tour contain any of defined pansions" in {
      val tourBuilder = data.randomTour.toBuilder
      tourBuilder.setPansion(Pansion.AI)
      aiFilter.fits(tourBuilder.build()) shouldBe true
    }
    "accept hotel snippet if defined pansions is empty" in {
      val snippetBuilder = data.randomSnippet.toBuilder.setHotelId(hotel.id)
      for (pansion <- Pansion.values()) {
        snippetBuilder.clearPansions().addPansionsBuilder().setPansion(pansion).setPrice(1)
        emptyFilter.fits(snippetBuilder.build()) shouldBe true
      }
    }
    "reject hotel snippet if snippet not contain any of defined operators" in {
      val snippetBuilder = data.randomSnippet.toBuilder.setHotelId(hotel.id)
      snippetBuilder.clearPansions().addPansionsBuilder().setPansion(Pansion.FB).setPrice(1)
      aiFilter.fits(snippetBuilder.build()) shouldBe false
    }
    "accept hotel snippet if snippet contain any of defined operators" in {
      val snippetBuilder = data.randomSnippet.toBuilder.setHotelId(hotel.id)
      snippetBuilder.clearPansions().addPansionsBuilder().setPansion(Pansion.AI).setPrice(1)
      aiFilter.fits(snippetBuilder.build()) shouldBe true
    }

    "serialize into json (empty)" in {
      val json = CommonJsonSerialization.filtersToJson(Seq(emptyFilter))
      json.toString shouldBe """{}"""
    }
    "serialize into json (ai)" in {
      val json = CommonJsonSerialization.filtersToJson(Seq(aiFilter))
      json.toString shouldBe """{"pansions":["ai"]}"""
    }
    "serialize into json (multiple pansions)" in {
      val json = CommonJsonSerialization.filtersToJson(Seq(PansionFilter(Set(Pansion.AI, Pansion.BB))))
      json.toString shouldBe """{"pansions":["ai","bb"]}"""
    }
  }
}
