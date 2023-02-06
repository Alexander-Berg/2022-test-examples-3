package ru.yandex.tours.indexer.suggest

import java.io.ByteArrayInputStream

import ru.yandex.tours.direction.{Priority, Priorities}
import ru.yandex.tours.search.settings.SearchSettingsHolder
import ru.yandex.tours.testkit.{BaseSpec, TestData}

import scala.util.Try

class SuggestGeneratorSpec extends BaseSpec with TestData {
  private def headedIs = new ByteArrayInputStream("some_header".getBytes)

  "Suggest generator" should {
    "generate suggest without errors" in {
      val file = SuggestGenerator.generate(
        data.hotelsIndex.hotels,
        data.geoMapping,
        data.regionTree,
        countryPriorities = new Priorities(Map.empty),
        resortPriorities = new Priorities(Map(11487 -> Priority(11487, 1d, 1d, 1d, 1))),
        hotelPriority = new Priorities(Map.empty),
        new SearchSettingsHolder(Seq.empty, data.regionTree),
        headedIs
      )
      file.delete()
    }
  }
}
