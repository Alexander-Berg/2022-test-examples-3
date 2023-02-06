package ru.yandex.tours.model

import java.time.Month
import java.time.Month._

import org.joda.time.LocalDate
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.{Inspectors, Matchers, WordSpecLike}

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 02.07.15
 */
class SeasonalitySpec extends WordSpecLike with Matchers with Inspectors {

  val yearDates = Iterator.iterate(LocalDate.now())(_.plusDays(1)).take(365).toList

  "UnknownSeasonality" should {
    "not contain every month" in {
      forAll(Month.values) {
        month => UnknownSeasonality shouldNot containMonth(month)
      }
    }
    "not contain every date" in {
      forAll(yearDates) {
        date => UnknownSeasonality shouldNot containDate(date)
      }
    }
  }

  "AllYear" should {
    "contain every month" in {
      forAll(Month.values) {
        month => AllYear should containMonth(month)
      }
    }
    "contain every date" in {
      forAll(yearDates) {
        date => AllYear should containDate(date)
      }
    }
  }

  "MonthInterval" should {
    "contain months inside range [start,end] if start > end" in {
      val winter = MonthInterval(DECEMBER, FEBRUARY)

      winter should containMonth(DECEMBER)
      winter should containMonth(JANUARY)
      winter should containMonth(FEBRUARY)

      winter shouldNot containMonth(MARCH)
      winter shouldNot containMonth(NOVEMBER)
      winter shouldNot containMonth(JUNE)
    }
    "contain months inside range [start,end] if start == end" in {
      val june = MonthInterval(JUNE, JUNE)

      june should containMonth(JUNE)
      june shouldNot containMonth(JULY)
      june shouldNot containMonth(MAY)
      june shouldNot containMonth(JANUARY)
    }
    "contain only dates inside range [start,end] if start < end" in {
      val summer = MonthInterval(JUNE, AUGUST)

      summer should containDate("2015-06-1")
      summer should containDate("2015-06-28")
      summer should containDate("2015-08-3")

      summer shouldNot containDate("2015-01-1")
      summer shouldNot containDate("2015-09-1")
      summer shouldNot containDate("2015-05-31")
    }
    "contain only dates outside range [start,end] if start >= end" in {
      val winter = MonthInterval(DECEMBER, FEBRUARY)

      winter should containDate("2015-01-1")
      winter should containDate("2015-02-28")
      winter should containDate("2016-02-29")
      winter should containDate("2016-12-31")
      winter should containDate("2016-12-1")

      winter shouldNot containDate("2015-03-1")
      winter shouldNot containDate("2015-09-1")
      winter shouldNot containDate("2015-05-31")
      winter shouldNot containDate("2015-06-1")
      winter shouldNot containDate("2015-06-28")
      winter shouldNot containDate("2015-08-3")
    }
  }

  "MonthSet" should {
    "contains dates with given months" in {
      val summer = MonthSet(Set(6,7,8).map(Month.of))
      summer should containDate("2015-06-1")
      summer should containDate("2015-06-28")
      summer should containDate("2015-08-3")

      summer shouldNot containDate("2015-01-1")
      summer shouldNot containDate("2015-09-1")
      summer shouldNot containDate("2015-05-31")
    }
    "contains dates with given months #2" in {
      val summerAndWinter = MonthSet(Set(1,2,12,6,7,8).map(Month.of))
      summerAndWinter should containDate("2015-06-1")
      summerAndWinter should containDate("2015-06-28")
      summerAndWinter should containDate("2015-08-3")
      summerAndWinter should containDate("2015-01-1")
      summerAndWinter should containDate("2015-02-28")
      summerAndWinter should containDate("2016-02-29")
      summerAndWinter should containDate("2016-12-31")
      summerAndWinter should containDate("2016-12-1")

      summerAndWinter shouldNot containDate("2015-09-1")
      summerAndWinter shouldNot containDate("2015-05-31")
      summerAndWinter shouldNot containDate("2015-11-30")
      summerAndWinter shouldNot containDate("2015-03-1")
    }
  }

  "Combined" should {
    "contains dates and months if any of provided seasons contains it" in {
      val summerAndWinter = Combined(MonthSet(Set(6,7,8).map(Month.of)), MonthInterval(DECEMBER, FEBRUARY))

      summerAndWinter should containMonth(DECEMBER)
      summerAndWinter should containMonth(JANUARY)
      summerAndWinter should containMonth(JUNE)
      summerAndWinter should containMonth(AUGUST)
      summerAndWinter should containDate("2015-06-1")
      summerAndWinter should containDate("2015-06-28")
      summerAndWinter should containDate("2015-08-3")
      summerAndWinter should containDate("2015-01-1")
      summerAndWinter should containDate("2015-02-28")
      summerAndWinter should containDate("2016-02-29")
      summerAndWinter should containDate("2016-12-31")
      summerAndWinter should containDate("2016-12-1")

      summerAndWinter shouldNot containMonth(SEPTEMBER)
      summerAndWinter shouldNot containMonth(MARCH)
      summerAndWinter shouldNot containDate("2015-09-1")
      summerAndWinter shouldNot containDate("2015-05-31")
      summerAndWinter shouldNot containDate("2015-11-30")
      summerAndWinter shouldNot containDate("2015-03-1")
    }
  }

  private def containMonth(month: Month): Matcher[Seasonality] =
    Matcher[Seasonality]({ season =>
      MatchResult(
        season.contains(month),
        s"$season do no contains $month",
        s"$season contains $month"
      )
    })

  private def containDate(date: LocalDate): Matcher[Seasonality] =
    Matcher[Seasonality]({ season =>
      MatchResult(
        season.contains(date),
        s"$season do no contains $date",
        s"$season contains $date"
      )
    })

  private def containDate(date: String): Matcher[Seasonality] =
    containDate(LocalDate.parse(date))
}
