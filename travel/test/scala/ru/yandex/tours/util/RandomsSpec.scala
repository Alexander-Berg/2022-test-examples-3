package ru.yandex.tours.util

import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 20.01.16
 */
class RandomsSpec extends BaseSpec {
  import Randoms._
  "Randoms" should {
    "return random element from seq with one element" in {
      Seq(1).randomElement shouldBe 1
    }
  }
}
