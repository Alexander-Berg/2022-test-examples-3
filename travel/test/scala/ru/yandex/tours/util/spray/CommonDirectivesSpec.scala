package ru.yandex.tours.util.spray

import ru.yandex.tours.testkit.BaseSpec
import spray.http.Uri.Query

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 02.03.16
 */
class CommonDirectivesSpec extends BaseSpec {
  private val ages = CommonDirectives.intArray("ages", isEmptyOk = true)

  "CommonDirectives" should {
    "parse intArray" in {
      extract(Query("ages=88&ages=88"), ages) should contain theSameElementsAs Seq(88, 88)
      extract(Query("ages=88&ages=78"), ages) should contain theSameElementsAs Seq(88, 78)
    }
    "parse comma delimited intArray" in {
      extract(Query("ages=88,88"), ages) should contain theSameElementsAs Seq(88, 88)
      extract(Query("ages=88,89"), ages) should contain theSameElementsAs Seq(88, 89)
    }
    "reject on empty values in intArray" in {
      an[Exception] shouldBe thrownBy {
        extract(Query("ages="), ages)
      }
    }
  }
}
