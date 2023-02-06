package ru.yandex.tours.wizard.domain


import org.joda.time.{LocalDate, MonthDay}
import org.scalatest.Matchers._
import org.scalatest.WordSpecLike
import ru.yandex.tours.query.DateInterval

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 09.02.15
 */
class TourStartIntervalSpec extends WordSpecLike {

  def dateInterval(from: String, until: String) = DateInterval(
    MonthDay.parse(from),
    MonthDay.parse(until)
  )

  "TourStartInterval" should {
    "convert from DateInterval (from and until in future)" in {
      val now = LocalDate.parse("2015-02-09")
      val interval = dateInterval("--02-22", "--02-28")
      val expected = TourStartInterval(LocalDate.parse("2015-02-22"), LocalDate.parse("2015-02-28"))
      TourStartInterval.fromDateInterval(interval, now) shouldBe expected
    }
    "convert from DateInterval (from in past, until in future)" in {
      val now = LocalDate.parse("2015-02-09")
      val interval = dateInterval("--02-02", "--02-12")
      val expected = TourStartInterval(LocalDate.parse("2015-02-02"), LocalDate.parse("2015-02-12"))
      TourStartInterval.fromDateInterval(interval, now) shouldBe expected
    }
    "convert from DateInterval (from and until in past)" in {
      val now = LocalDate.parse("2015-02-09")
      val interval = dateInterval("--01-02", "--01-12")
      val expected = TourStartInterval(LocalDate.parse("2016-01-02"), LocalDate.parse("2016-01-12"))
      TourStartInterval.fromDateInterval(interval, now) shouldBe expected
    }
    "convert from DateInterval (new year)" in {
      val now = LocalDate.parse("2015-02-09")
      val interval = dateInterval("--12-28", "--01-05")
      val expected = TourStartInterval(LocalDate.parse("2015-12-28"), LocalDate.parse("2016-01-05"))
      TourStartInterval.fromDateInterval(interval, now) shouldBe expected
    }
    "convert from DateInterval (new year #2)" in {
      val now = LocalDate.parse("2015-12-31")
      val interval = dateInterval("--12-28", "--01-05")
      val expected = TourStartInterval(LocalDate.parse("2015-12-28"), LocalDate.parse("2016-01-05"))
      TourStartInterval.fromDateInterval(interval, now) shouldBe expected
    }
    "convert from DateInterval (new year #3)" in {
      val now = LocalDate.parse("2016-01-01")
      val interval = dateInterval("--12-28", "--01-05")
      val expected = TourStartInterval(LocalDate.parse("2015-12-28"), LocalDate.parse("2016-01-05"))
      TourStartInterval.fromDateInterval(interval, now) shouldBe expected
    }
  }
}
