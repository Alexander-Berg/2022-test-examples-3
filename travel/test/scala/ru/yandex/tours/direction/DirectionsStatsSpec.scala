package ru.yandex.tours.direction

import ru.yandex.tours.testkit.{BaseSpec, TestData}

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 12.11.15
 */
class DirectionsStatsSpec extends BaseSpec with TestData {
  val Finland = 123
  val Sweden = 127
  val UK = 102
  val France = 124
  val London = 10393
  val Ruka = 1000000136

  "DirectionStats" should {
    "load data" in {
      data.directionsStats
    }

    "calculate near countries for given country" in {
      val stats = data.directionsStats

      stats.getNearCountries(Finland).map(_.id) should contain (Sweden)
      stats.getNearCountries(UK).map(_.id) should contain (France)
    }

    "calculate near countries for given non-country" in {
      val stats = data.directionsStats

      stats.getNearCountries(Ruka).map(_.id) should contain (Sweden)
      stats.getNearCountries(London).map(_.id) should contain (France)
    }
  }
}
