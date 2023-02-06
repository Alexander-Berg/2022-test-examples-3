package ru.yandex.tours.model.search

import org.joda.time.LocalDate
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.tours.model.BaseModel.Currency
import ru.yandex.tours.model.util.proto.fromLocalDate

import scala.collection.JavaConverters._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 29.12.14
 */
class HotelSearchRequestSpec extends WordSpecLike with Matchers {

  val request = HotelSearchRequest(
    from = 213,
    to = 442,
    nights = 9,
    when = new LocalDate(2014, 12, 20),
    ages = List(2, 22, 24),
    flexWhen = true,
    flexNights = false
  )

  //hardcoded values. change them if necessary
  val expectedSessionId = "f92d853b39558302db4a6b80145574e3"
  val expectedHashCode = -1043929999L

  "HotelSearchRequest" should {
    "have stable sessionId" in {
      request.sessionId shouldBe expectedSessionId
      request.copy().sessionId shouldBe expectedSessionId
    }
    "have same sessionId if ages have different type" in {
      request.copy(ages = request.ages.toVector).sessionId shouldBe expectedSessionId
      request.copy(ages = request.ages.toArray).sessionId shouldBe expectedSessionId
    }
    "have same sessionId if ages have different order" in {
      request.copy(ages = List(2, 24, 22)).sessionId shouldBe expectedSessionId
      request.copy(ages = List(22, 24, 2)).sessionId shouldBe expectedSessionId
      request.copy(ages = List(24, 22, 2)).sessionId shouldBe expectedSessionId
      request.copy(ages = List(24, 2, 22)).sessionId shouldBe expectedSessionId
    }
    "have different sessionId if any field is different" in {
      request.copy(from = 1).sessionId shouldNot be (expectedSessionId)
      request.copy(to = 1).sessionId shouldNot be (expectedSessionId)
      request.copy(nights = 3).sessionId shouldNot be (expectedSessionId)
      request.copy(when = request.when.plusDays(1)).sessionId shouldNot be (expectedSessionId)
      request.copy(ages = List(1, 2, 3)).sessionId shouldNot be (expectedSessionId)
      request.copy(flexWhen = !request.flexWhen).sessionId shouldNot be (expectedSessionId)
      request.copy(flexNights = !request.flexNights).sessionId shouldNot be (expectedSessionId)
      request.copy(filter = HotelSearchFilter(32)).sessionId should not be request.sessionId
    }
    "have stable hashCode" in {
      request.hashCode shouldBe expectedHashCode
      request.copy().hashCode shouldBe expectedHashCode
    }
    "have same hashCode if ages have different type" in {
      request.copy(ages = request.ages.toVector).hashCode shouldBe expectedHashCode
      request.copy(ages = request.ages.toArray).hashCode shouldBe expectedHashCode
    }
    "have same hashCode if ages have different order" in {
      request.copy(ages = List(2, 24, 22)).hashCode shouldBe expectedHashCode
      request.copy(ages = List(22, 24, 2)).hashCode shouldBe expectedHashCode
      request.copy(ages = List(24, 22, 2)).hashCode shouldBe expectedHashCode
      request.copy(ages = List(24, 2, 22)).hashCode shouldBe expectedHashCode
    }
    "equals to himself" in {
      request shouldBe request
    }
    "equals if ages have different type" in {
      request.copy(ages = request.ages.toVector) shouldBe request
      request.copy(ages = request.ages.toArray) shouldBe request
    }
    "equals if ages have different order" in {
      request.copy(ages = List(2, 24, 22)) shouldBe request
      request.copy(ages = List(22, 24, 2)) shouldBe request
      request.copy(ages = List(24, 22, 2)) shouldBe request
      request.copy(ages = List(24, 2, 22)) shouldBe request
    }

    "correct kidsAges method" in {
      request.kidsAges shouldBe Seq(2)
    }
    "correct adults method" in {
      request.adults shouldBe 2
    }
    "correct kids method" in {
      request.kids shouldBe 1
    }

    "correct nightsRange for flex nights" in {
      request.copy(flexNights = true).nightsRange shouldBe Seq(7, 8, 9, 10, 11)
      request.copy(nights = 1, flexNights = true).nightsRange shouldBe Seq(1, 2, 3)
      request.copy(nights = 2, flexNights = true).nightsRange shouldBe Seq(1, 2, 3, 4)
      request.copy(nights = 3, flexNights = true).nightsRange shouldBe Seq(1, 2, 3, 4, 5)
    }
    "correct nightsRange for non-flex nights" in {
      request.copy(flexNights = false).nightsRange shouldBe Seq(9)
    }
    "correct dateRange for flex dates" in {
      val expected = Seq(request.when.minusDays(2), request.when.minusDays(1), request.when,
        request.when.plusDays(1), request.when.plusDays(2))
      request.copy(flexWhen = true).dateRange shouldBe expected
    }
    "correct dateRange for non-flex dates" in {
      request.copy(flexWhen = false).dateRange shouldBe Seq(request.when)
    }
    "convert to protobuf message" in {
      val proto = request.toProto
      proto.getFrom shouldBe request.from
      proto.getTo shouldBe request.to
      proto.getNights shouldBe request.nights
      proto.getWhen shouldBe fromLocalDate(request.when)
      proto.getAgesList.asScala shouldBe request.ages
      proto.getFlexWhen shouldBe request.flexWhen
      proto.getFlexNights shouldBe request.flexNights
      proto.getCurrency shouldBe request.currency
      proto.getLang shouldBe request.lang.toString
    }
    "be built from protobuf message" in {
      HotelSearchRequest(request.toProto) shouldBe request
    }
    "different sessionId if currency is changed" in {
      request.copy(currency = Currency.EUR).sessionId should not be request.sessionId
    }
    "have correct stars set " in {
      request.copy(filter = HotelSearchFilter(40)).filter.asInstanceOf[HotelSearchFilter].starsSet shouldBe Set(3, 5)
    }
  }
}
