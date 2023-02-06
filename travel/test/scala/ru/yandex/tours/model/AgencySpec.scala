package ru.yandex.tours.model

import org.joda.time.{DateTime, DateTimeZone, LocalDateTime, LocalTime}
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.tours.model.Agencies.ProtoAgency
import ru.yandex.tours.model.Agency.{Interval, Timetable}
import ru.yandex.tours.model.BaseModel.Point
import ru.yandex.tours.model.util.proto

class AgencySpec extends WordSpecLike with Matchers {
  private def interval(from: String, to: String) = {
    Interval(time(from), time(to))
  }

  private def time(x: String) = LocalTime.parse(x)

  "Agency" should {
    "Take first name by lang" in {
      val protoAgency = ProtoAgency.newBuilder()
        .setGeoId(0)
        .setPoint(Point.newBuilder().setLatitude(0).setLongitude(0))
        .setShows(0)
        .setId(0)
        .addName(proto.toLangVal("ru", "1"))
        .addName(proto.toLangVal("ru", "2"))
        .build
      Agency.parseAgency(protoAgency).names(Languages.ru) shouldBe "1"
    }
  }

  "Timetable" should {
    val map = Map(4 -> Seq(interval("10:00", "19:00"), interval("21:00", "22:00")), 3 -> Seq(interval("11:00", "13:25")))
    val timetable = Timetable(map, DateTimeZone.UTC)

    "return correct working time description. Open. Simple case" in {
      val wtd = timetable.getWorkingTimeDescription(DateTime.parse("2015-04-02T13:48+00:00"))
      wtd.is24Hour shouldBe false
      wtd.isOpen shouldBe true
      wtd.nextEvent shouldBe LocalDateTime.parse("2015-04-02T19:00")
    }

    "return correct working time description. Close. Simple case" in {
      val wtd = timetable.getWorkingTimeDescription(DateTime.parse("2015-04-02T08:48+00:00"))
      wtd.is24Hour shouldBe false
      wtd.isOpen shouldBe false
      wtd.nextEvent shouldBe LocalDateTime.parse("2015-04-02T10:00")
    }

    "return correct working time description. Close. Next week" in {
      val wtd = timetable.getWorkingTimeDescription(DateTime.parse("2015-04-02T22:01+00:00"))
      wtd.is24Hour shouldBe false
      wtd.isOpen shouldBe false
      wtd.nextEvent shouldBe LocalDateTime.parse("2015-04-08T11:00")
    }

    "return correct working time description. Close. Closed for lunch" in {
      val wtd = timetable.getWorkingTimeDescription(DateTime.parse("2015-04-02T19:05+00:00"))
      wtd.is24Hour shouldBe false
      wtd.isOpen shouldBe false
      wtd.nextEvent shouldBe LocalDateTime.parse("2015-04-02T21:00")
    }

    "return correct working time description. Open. 24 hour" in {
      val map = Map(1 -> Seq(Agency.twentyForHourInterval), 2 -> Seq(Agency.twentyForHourInterval))
      val timetable24 = Timetable(map, DateTimeZone.UTC)
      val wtd1 = timetable24.getWorkingTimeDescription(DateTime.parse("2015-03-30T08:00+00:00"))
      wtd1.is24Hour shouldBe true
      wtd1.isOpen shouldBe true
      wtd1.nextEvent shouldBe LocalDateTime.parse("2015-03-30T23:59:59.999")
      val wtd2 = timetable24.getWorkingTimeDescription(DateTime.parse("2015-03-31T08:00+00:00"))
      wtd2.is24Hour shouldBe false
      wtd2.isOpen shouldBe true
      wtd2.nextEvent shouldBe LocalDateTime.parse("2015-03-31T23:59:59.999")
    }

    "return correct working time for night agencies" in {
      val timetable = Timetable(Map(4 -> Seq(interval("8:00", "0:00"))), DateTimeZone.UTC)
      timetable.getWorkingTimeDescription(DateTime.parse("2015-04-02T19:05+00:00")).isOpen shouldBe true
    }
    "return correct working time for night agencies #2" in {
      pending
      val timetable = Timetable(Map(4 -> Seq(interval("23:00", "5:00"))), DateTimeZone.UTC)
      timetable.getWorkingTimeDescription(DateTime.parse("2015-04-02T19:05+00:00")).isOpen shouldBe false
      timetable.getWorkingTimeDescription(DateTime.parse("2015-04-02T23:05+00:00")).isOpen shouldBe true
    }
  }

}
