package ru.yandex.tours.calendar

import akka.util.Timeout
import org.joda.time.LocalDate
import ru.yandex.tours.clickhouse.DefaultClickHouseClient
import ru.yandex.tours.model.search.{HotelSearchRequest, SearchType}
import ru.yandex.tours.search.Defaults
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import ru.yandex.tours.util.http.NingHttpClient
import ru.yandex.tours.util.lang.Futures._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 25.04.16
 */
class ClickHouseMinPriceServiceIntSpec extends BaseSpec with TestData {

  implicit val patience: PatienceConfig = PatienceConfig(timeout = 30.seconds, interval = 100.millis)
  implicit val timeout = Timeout(30.seconds)

  val httpClient = new NingHttpClient(None)
  val clickHouseClient = new DefaultClickHouseClient(httpClient,
    "", "http",
    "localhost", 8123,
    "tours", "ieWooviechai0lool8te", "tours", FiniteDuration(30, SECONDS))(global, timeout)

  val minPriceService = new ClickHouseMinPriceService(clickHouseClient, SearchType.TOURS, data.tourOperators)


  "ClickHouseMinPriceService" should {
    "get graph" in {
      val req = HotelSearchRequest(213, 995, 12, LocalDate.now, Defaults.TWO_ADULTS)
      for (i <- 1 to 100) {
        minPriceService.getMinPriceGraph(req, 365).logTiming("getMinPriceGraph").futureValue
      }

      val result = minPriceService.getMinPriceGraph(req, 365).logTiming("getMinPriceGraph").futureValue
      println(result)
    }
  }
}
