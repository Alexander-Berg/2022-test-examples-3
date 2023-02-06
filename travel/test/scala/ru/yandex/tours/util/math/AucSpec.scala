package ru.yandex.tours.util.math

import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 04.08.16
 */
class AucSpec extends BaseSpec {
  "Auc.of" should {
    "return zero for zero points" in {
      Auc.of(Seq.empty) shouldBe 0d
    }
    "return zero for one point" in {
      Auc.of(Seq(1d)) shouldBe 0d
    }
    "calc auc of 2 points" in {
      Auc.of(Seq(1d, 0d)) shouldBe 0.5
      Auc.of(Seq(0.5d, 0.5d)) shouldBe 0.5
      Auc.of(Seq(1d, 0.5d)) shouldBe 0.75
    }
    "calc auc for 3 points" in {
      Auc.of(Seq(0d, 1d, 0.5d)) shouldBe 1.25
    }
  }
}
