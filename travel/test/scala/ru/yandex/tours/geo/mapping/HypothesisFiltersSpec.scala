package ru.yandex.tours.geo.mapping

import ru.yandex.tours.geo.base.region
import ru.yandex.tours.geo.matching.TypeFilterMap
import ru.yandex.tours.testkit.BaseSpec

/**
  * Created by asoboll on 17.02.16.
  */
object TypeFilterMapTest extends TypeFilterMap {
  def getMap = yandexCategoryMap
}

class HypothesisFiltersSpec extends BaseSpec {
  "TypeFilter" should {
    "be defined for all region types" in {
      region.Types.values.toSet shouldEqual TypeFilterMapTest.getMap.keySet
    }
  }
}
