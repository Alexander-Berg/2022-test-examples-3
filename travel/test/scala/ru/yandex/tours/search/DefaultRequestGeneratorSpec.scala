package ru.yandex.tours.search

import java.time.Month

import org.joda.time.{DateTimeConstants, LocalDate}
import org.scalatest.Inspectors
import ru.yandex.tours.model.search.SearchType
import ru.yandex.tours.testkit.BaseSpec
import ru.yandex.tours.util.lang.Dates._

import scala.util.Random

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 28.07.15
 */
class DefaultRequestGeneratorSpec extends BaseSpec with Inspectors {
  "DefaultRequestGenerator.getDefaultFlightDay" should {
    val result = DefaultRequestGenerator.getDefaultFlightDay
    result.nights shouldBe DefaultRequestGenerator.DEFAULT_NIGHTS
    result.when.getDayOfWeek shouldBe DateTimeConstants.SATURDAY
    result.when shouldBe > (LocalDate.now)
  }
  "DefaultRequestGenerator.getDefaultRequest" should {
    "return request with default parameters and some date in given month" in {
      forAll(Month.values().toSeq) { month =>
        val from = Random.nextInt().abs
        val to = Random.nextInt().abs
        val request = DefaultRequestGenerator.getDefaultRequest(from, to, month, SearchType.TOURS)
        request should have(
          'from (from),
          'to (to),
          'nights (DefaultRequestGenerator.DEFAULT_NIGHTS),
          'ages (DefaultRequestGenerator.DEFAULT_AGES),
          'flexWhen (false),
          'flexNights (false)
        )
        request.when.getMonthOfYear should (be (month.getValue) or be (LocalDate.now.nextSaturday.getMonthOfYear))
        request.when.getDayOfWeek shouldBe DateTimeConstants.SATURDAY
        request.when shouldBe > (LocalDate.now)
      }
    }
  }
}
