package ru.yandex.tours.prices

import akka.util.Timeout
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import ru.yandex.tours.clickhouse.DefaultClickHouseClient
import ru.yandex.tours.model.BaseModel.Pansion
import ru.yandex.tours.model.filter.snippet.{PansionFilter, PriceFilter, TourOperatorFilter}
import ru.yandex.tours.model.search.SearchDates
import ru.yandex.tours.model.util.{CustomDateInterval, CustomNights}
import ru.yandex.tours.search.Defaults
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import ru.yandex.tours.util.Speller
import ru.yandex.tours.util.http.NingHttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 10.11.15
 */
class ClickHousePriceSearcherIntSpec extends BaseSpec with TestData {

  implicit val patience: PatienceConfig = PatienceConfig(timeout = 10.seconds, interval = 100.millis)
  implicit val timeout = Timeout(30.seconds)

  val httpClient = new NingHttpClient(None)
  val clickHouseClient = new DefaultClickHouseClient(httpClient,
    "", "http", "clickhouse-01-sas.test.vertis.yandex.net", 8123,
    "tours", "ieWooviechai0lool8te", "tours", FiniteDuration(30, SECONDS))(global, timeout)
  val priceSearcher = new ClickHousePriceSearcher(clickHouseClient)

  val date = LocalDate.now().plusDays(7)

  "ClickHousePriceSearcher" should {
    "Search last prices" in {
      val res = priceSearcher.searchLastPrices(2, 21241, Defaults.TWO_ADULTS,
        SearchDates(date, 8, flexWhen = true, flexNights = true),
        filters = Seq.empty,
        freshness = 1.day
      ).futureValue
      println(res.size)
    }

    "search last direction prices" in {
      val now = LocalDate.now
      val res = priceSearcher.searchLastDirectionPrices(213, Defaults.TWO_ADULTS,
        SearchDates(
          CustomDateInterval(now.plusDays(1), now.plusDays(10)),
          CustomNights(5 to 12),
          Option.empty
        ),
        filters = Seq.empty,
        freshness = 3.hours
      ).futureValue

      res
        .filter(p ⇒ data.regionTree.region(p.to).isDefined)
        .filter(p ⇒ data.geoMapping.isKnownDestination(p.to))
        .sortBy(_.price)
        .foreach {
        price ⇒
          val region = data.regionTree.region(price.to).get
          val country = data.regionTree.country(price.to).get
          println(
            (if (region != country) region.name.ruName + ", " else "") +
              country.name.ruName + " " +
              price.when.toString(DateTimeFormat.longDate()) + " на " + Speller.nights(price.nights) +
              ": от " + price.price + "₽"
          )
      }
    }

    "search prices with filter" in {
      val res = priceSearcher.searchLastPrices(2, 21241, Defaults.TWO_ADULTS,
        SearchDates(date, 8, flexWhen = true, flexNights = false),
        Seq(
          PriceFilter(Some(0), Some(1000000)),
          PansionFilter(Set(Pansion.FB, Pansion.HB)),
          TourOperatorFilter(Set(1 until 64: _*))
        ),
        freshness = 1.day
      ).futureValue
      println(res.size)
    }

    "get freshness" in {
      val res = priceSearcher.getFreshness(2, 21241, Defaults.TWO_ADULTS,
        SearchDates(date, 8, flexWhen = true, flexNights = false),
        freshness = 7.day
      ).futureValue
      println(res.size)
      res.foreach(println)
    }
  }
}
