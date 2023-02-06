package ru.yandex.tours.backend.search

import ru.yandex.tours.partners.PartnerProtocol.{Skipped, SnippetsResult}
import ru.yandex.tours.testkit.{TestData, BaseSpec}

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 17.02.16
 */
class SearcherProgressSpec extends BaseSpec with TestData {
  val operators = data.tourOperators.getAll
  val operator = operators.head

  "SearcherProgress" should {
    "start with not finished state" in {
      val progress = new SearcherProgress(operators.toSet)
      progress should not be 'finished
    }
    "finish when waitMap == results" in {
      val progress = new SearcherProgress[SnippetsResult](Set(operator))
      progress.updateWaitMap(Map(operator -> 1))
      progress should not be 'finished
      progress.add(SnippetsResult(operator, Skipped))
      progress shouldBe 'finished
    }
  }
}
