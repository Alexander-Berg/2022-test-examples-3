package ru.yandex.tours.util.lang

import java.time.Month

import org.joda.time.{DateTime, DateTimeConstants, LocalDate}
import org.scalacheck.Gen
import org.scalatest.Matchers._
import org.scalatest.WordSpecLike
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import ru.yandex.tours.util.lang.Dates._
/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 27.02.15
 */
class DatesSpec extends WordSpecLike {

  def dateTimeGen = {
    val now = DateTime.now()
    for (dateOffset <- Gen.chooseNum(-365 * 10, 365 * 20))
    yield now.plusDays(dateOffset)
  }
  def localDateGen = {
    val now = LocalDate.now()
    for (dateOffset <- Gen.chooseNum(-365 * 10, 365 * 20))
    yield now.plusDays(dateOffset)
  }
  def beBefore(date: LocalDate) = new Matcher[LocalDate] {
    override def apply(left: LocalDate): MatchResult = {
      MatchResult(
        left.isBefore(date),
        s"LocalDate $left not before $date",
        s"LocalDate $left is before $date"
      )
    }
  }


  "Dates" should {
    "convert LocalDate to long and back" in {
      forAll(localDateGen) { date =>
        date.toMillis.toLocalDate shouldBe date
      }
    }
    "convert LocalDate to compact Int and back" in {
      forAll(localDateGen) { date =>
        val int = date.toCompactInt
        int.toLocalDate shouldBe date
      }
    }
    "localDateOrdering compare LocalDates" in {
      forAll(localDateGen, localDateGen) { (date1, date2) =>
        Dates.localDateOrdering.compare(date1, date2) shouldBe date1.compareTo(date2)
      }
    }
    "dateTimeOrdering compare DateTimes" in {
      forAll(dateTimeGen, dateTimeGen) { (date1, date2) =>
        Dates.dateTimeOrdering.compare(date1, date2) shouldBe date1.compareTo(date2)
      }
    }
    "calculate next saturday" in {
      forAll(localDateGen) { date =>
        val nextSaturday = date.nextSaturday
        nextSaturday.getDayOfWeek shouldBe DateTimeConstants.SATURDAY
        date should beBefore(nextSaturday)
      }
    }
    "calculate next saturday from custom date" in {
      val date = LocalDate.parse("2015-02-09")
      val saturday = LocalDate.parse("2015-02-14")
      date.nextSaturday shouldBe saturday
    }
    "calculate next saturday from saturday" in {
      val date = LocalDate.parse("2015-02-07") //saturday
      val saturday = LocalDate.parse("2015-02-14")
      date.nextSaturday shouldBe saturday
    }
    "calculate saturday on next week" in {
      LocalDate.parse("2015-02-07").nextWeekSaturday shouldBe LocalDate.parse("2015-02-14")
      LocalDate.parse("2015-07-21").nextWeekSaturday shouldBe LocalDate.parse("2015-08-01")
      LocalDate.parse("2015-07-25").nextWeekSaturday shouldBe LocalDate.parse("2015-08-01")
      LocalDate.parse("2015-07-26").nextWeekSaturday shouldBe LocalDate.parse("2015-08-01")
      LocalDate.parse("2015-07-27").nextWeekSaturday shouldBe LocalDate.parse("2015-08-08")
      LocalDate.parse("2014-12-27").nextWeekSaturday shouldBe LocalDate.parse("2015-01-03")
    }
    "calculate month start" in {
      LocalDate.parse("2015-02-07").atMonthStart shouldBe LocalDate.parse("2015-02-01")
      LocalDate.parse("2015-07-31").atMonthStart shouldBe LocalDate.parse("2015-07-01")
      LocalDate.parse("2015-12-31").atMonthStart shouldBe LocalDate.parse("2015-12-01")
      LocalDate.parse("2015-04-01").atMonthStart shouldBe LocalDate.parse("2015-04-01")
    }
    "calculate month end" in {
      LocalDate.parse("2015-02-07").atMonthEnd shouldBe LocalDate.parse("2015-02-28")
      LocalDate.parse("2016-02-07").atMonthEnd shouldBe LocalDate.parse("2016-02-29")
      LocalDate.parse("2015-07-31").atMonthEnd shouldBe LocalDate.parse("2015-07-31")
      LocalDate.parse("2015-12-31").atMonthEnd shouldBe LocalDate.parse("2015-12-31")
      LocalDate.parse("2015-04-04").atMonthEnd shouldBe LocalDate.parse("2015-04-30")
    }
    "calculate near start of month" in {
      LocalDate.parse("2015-02-07").nearMonthStart(Month.JUNE) shouldBe LocalDate.parse("2015-06-01")
      LocalDate.parse("2016-02-07").nearMonthStart(Month.FEBRUARY) shouldBe LocalDate.parse("2016-02-01")
      LocalDate.parse("2015-07-31").nearMonthStart(Month.FEBRUARY) shouldBe LocalDate.parse("2016-02-01")
      LocalDate.parse("2015-12-31").nearMonthStart(Month.JANUARY) shouldBe LocalDate.parse("2016-01-01")
      LocalDate.parse("2015-04-04").nearMonthStart(Month.AUGUST) shouldBe LocalDate.parse("2015-08-01")
    }
  }
}
