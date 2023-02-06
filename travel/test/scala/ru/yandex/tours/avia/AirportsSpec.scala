package ru.yandex.tours.avia

import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 15.02.16
 */
class AirportsSpec extends BaseSpec {

  "Airports" should {
    "load from file" in {
      val airports = Airports.fromFile(root / "avia_airports.tsv")
      println(airports)
    }
  }
}
