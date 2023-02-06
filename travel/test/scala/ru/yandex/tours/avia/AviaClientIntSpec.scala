package ru.yandex.tours.avia

import org.joda.time.LocalDate
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import ru.yandex.tours.model.search.{FlightSearchRequest, HotelSearchRequest}
import ru.yandex.tours.search.DefaultRequestGenerator
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import ru.yandex.tours.util.http.NingHttpClient
import ru.yandex.tours.util.lang.Futures._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 02.02.16
 */
class AviaClientIntSpec extends BaseSpec with TestData with ScalaFutures with OptionValues {
  override implicit val patienceConfig = PatienceConfig(timeout = 60.seconds)

  val client = new DefaultAviaClient(
    new NingHttpClient(None)(global),
    "http://ticket-daemon-api-production.common.yandex.net",
    "https://api.avia.yandex.net/v1.0",
    "36769c53-9462-4fdb-8270-54f56524cbf3",
    "1d50e1fc1c1e4fcf8cab44fa14e68e38"
  )

  val sbpMsk = FlightSearchRequest(
    HotelSearchRequest(2, 213, 1, LocalDate.now().plusDays(1), DefaultRequestGenerator.DEFAULT_AGES),
    airportId = "c213"
  )

  "AviaClient" should {
    "load prices for spb <-> moscow" in {
      val result = client.getMinPrice(sbpMsk, update = false).logTiming("getMinPrice")
      val minPrice = result.futureValue
      println(minPrice)
    }
    "update prices for spb <-> moscow" in {
      val result = client.getMinPrice(sbpMsk, update = true).logTiming("getMinPrice with update")
      println(result.futureValue)
      val searchId = result.futureValue.search_id.value
      var updated = client.getMinPriceUpdated(searchId).logTiming("getMinPriceUpdated").futureValue
      while (updated.status != "done") {
        println(updated)
        updated = client.getMinPriceUpdated(searchId).logTiming("getMinPriceUpdated").futureValue
      }
      println(updated)
    }
    "update prices" in {
      val from = 213
      val to = 10088
      val when = LocalDate.parse("2016-04-09")
      val nights = 7
      val airport = "c10429" // data.airportRecommendations.recommend(from, to).get.city.id
      val req = FlightSearchRequest(
        HotelSearchRequest(from, to, nights, when, Seq(88, 88)),
        airport
      )

      val result = client.getMinPrice(req, update = true).logTiming("getMinPrice with update")
      println(result.futureValue)
      val searchId = result.futureValue.search_id.value
      var updated = client.getMinPriceUpdated(searchId).logTiming("getMinPriceUpdated").futureValue
      while (updated.status != "done") {
        println(updated)
        updated = client.getMinPriceUpdated(searchId).logTiming("getMinPriceUpdated").futureValue
      }
      println(updated)
    }
    "get dictionaries" in {
      val result = client.getDictionaries.futureValue
      println(result)
      result.airports.filter(_.city_key.isEmpty).foreach(println)
    }
  }
}
