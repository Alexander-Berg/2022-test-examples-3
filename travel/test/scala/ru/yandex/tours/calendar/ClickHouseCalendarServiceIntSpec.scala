package ru.yandex.tours.calendar

import akka.util.Timeout
import org.joda.time.LocalDate
import ru.yandex.tours.clickhouse.DefaultClickHouseClient
import ru.yandex.tours.model.util.DateInterval
import ru.yandex.tours.testkit.BaseSpec
import ru.yandex.tours.util.collections.SimpleBitSet
import ru.yandex.tours.util.http.NingHttpClient
import ru.yandex.tours.util.lang.Dates._
import ru.yandex.tours.util.lang.Futures._
import ru.yandex.tours.util.parsing.Tabbed

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 04.04.16
 */
class ClickHouseCalendarServiceIntSpec extends BaseSpec {
  implicit val patience: PatienceConfig = PatienceConfig(timeout = 30.seconds, interval = 100.millis)
  implicit val timeout = Timeout(30.seconds)

  val httpClient = new NingHttpClient(None)
  val clickHouseClient = new DefaultClickHouseClient(httpClient,
    "", "http", "vertis-clickhouse01ht.vs.os.yandex.net", 8123,
    "tours", "ieWooviechai0lool8te", "tours",  FiniteDuration(30, SECONDS))(global, timeout)

  val calendarService = new ClickHouseCalendarService(clickHouseClient, None)(global)

  "ClickHouseCalendarService" should {
    "getFlightMatrix" in {
      val matrix = calendarService.getFlightMatrix.logTiming("getFlightMatrix").futureValue
      println(matrix)
    }

    "getNearestFlightDay" in {
      val now = LocalDate.now
      val interval = DateInterval(now, now.plusYears(1))

      for (i <- 1 to 100) {
        calendarService.getNearestFlightDay(2, 239, now, interval).logTiming("getNearestFlightDay").futureValue
      }

      val fd = calendarService.getNearestFlightDay(2, 239, now, interval).logTiming("getNearestFlightDay").futureValue
      println(fd)
    }

    "get hasFlights" in {
      for (i <- 1 to 100) {
        calendarService.getHasFlights(2, 10093, None).logTiming("getHasFlights").futureValue
      }

      val result = calendarService.getHasFlights(2, 10093, None).logTiming("getHasFlights").futureValue
      result.foreach { doc =>
        println(Tabbed(doc.getWhen.toLocalDate, doc.getNights,
          SimpleBitSet.from(doc.getHasDeparture).toSet, SimpleBitSet.from(doc.getNoDeparture).toSet))
      }
      println(result.size)
    }
    "get noFlights" in {
      for (i <- 1 to 100) {
        calendarService.getNoFlights(213, 995, Some(12)).logTiming("getNoFlights").futureValue
      }

      val result = calendarService.getNoFlights(213, 995, Some(12)).logTiming("getNoFlights").futureValue
      result.foreach { doc =>
        println(Tabbed(doc.getWhen.toLocalDate, doc.getNights,
          SimpleBitSet.from(doc.getHasDeparture).toSet, SimpleBitSet.from(doc.getNoDeparture).toSet))
      }
      println(result.size)
    }
  }

}
