package ru.yandex.tours.avia

import org.joda.time.LocalDate
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import ru.yandex.tours.model.search.HotelSearchRequest
import ru.yandex.tours.search.DefaultRequestGenerator
import ru.yandex.tours.testkit.{TestData, BaseSpec}
import ru.yandex.tours.util.http.NingHttpClient

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 16.01.16
 */
class AviaCacheClientIntSpec extends BaseSpec with TestData with ScalaFutures with IntegrationPatience {
  val client = new AviaCacheClient(new NingHttpClient(None), data.airportRecommendations)(global)

  "AviaCacheClient" should {
    "load price for spb <-> moscow" in {
      val req = HotelSearchRequest(2, 213, 1, LocalDate.now().plusDays(1), DefaultRequestGenerator.DEFAULT_AGES)
      val minPrice = client.getMinPrice(req).futureValue
      println(minPrice)
      minPrice shouldBe 'defined
    }
    "load price for spb <-> sochi" in {
      val req = HotelSearchRequest(2, 239, 1, LocalDate.now().plusDays(1), DefaultRequestGenerator.DEFAULT_AGES)
      val minPrice = client.getMinPrice(req).futureValue
      println(minPrice)
      minPrice shouldBe 'defined
    }
  }
}
