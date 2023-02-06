package ru.yandex.tours.partners.sunmar

import org.joda.time.LocalDate
import org.scalatest.Ignore
import ru.yandex.tours.model.search.HotelSearchRequest
import ru.yandex.tours.testkit.{TestData, BaseSpec}
import ru.yandex.tours.util.lang.Dates._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 27.05.15
 */
@Ignore
class SunmarTranslatorSpec extends BaseSpec with TestData {

  val translator = new SunmarTranslator(data.geoMapping, data.regionTree, data.hotelsIndex, data.pansions)

  val date = LocalDate.now.plusDays(7)
  val hotelSearchRequest = HotelSearchRequest(213, 1056, 7, date, List(88, 88), flexWhen = false, flexNights = false)

  "SunmarTranslator" should {
    "translate ages" in {
      val request = translator.toPackageListRequest(hotelSearchRequest).get
      request.getAdult shouldBe 2
      request.getChild shouldBe 0
    }
    "translate nights" in {
      val request = translator.toPackageListRequest(hotelSearchRequest).get
      request.getBeginNight shouldBe 7
      request.getEndNight shouldBe 7
    }
    "translate nights with flex nights" in {
      val request = translator.toPackageListRequest(hotelSearchRequest.copy(flexNights = true)).get
      request.getBeginNight shouldBe 5
      request.getEndNight shouldBe 9
    }
    "translate dates" in {
      val request = translator.toPackageListRequest(hotelSearchRequest).get
      request.getBeginDate.toLocalDate shouldBe date
      request.getEndDate.toLocalDate shouldBe date
    }
    "translate dates with flex dates" in {
      val request = translator.toPackageListRequest(hotelSearchRequest.copy(flexWhen = true)).get
      request.getBeginDate.toLocalDate shouldBe date.minusDays(2)
      request.getEndDate.toLocalDate shouldBe date.plusDays(2)
    }
    "translate geography" in {
      val request = translator.toPackageListRequest(hotelSearchRequest).get
      request.getFromArea shouldBe 2671
      request.getToCountry shouldBe 12
    }
  }
}
