package ru.yandex.tours.direction.layout

import org.scalatest.Inspectors
import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 08.07.15
 */
class LayoutSpec extends BaseSpec with Inspectors {
  "Layout.bySize" should {
    "provide layout for all size up to 20" in {
      forEvery(1 to 20) { size =>
        withClue(s"Size = $size ") {
          val layout = Layout.bySize(size)
          layout.size shouldBe size
        }
      }
    }

    "provide static layout for all size up to 20" in {
      forEvery(1 to 20) { size =>
        withClue(s"Size = $size ") {
          val layout = Layout.staticBySize(size)
          layout.size shouldBe size
          forAll(layout.rows.flatMap(_.items)) {
            item => item.size shouldBe 2
          }
        }
      }
    }
  }
}
