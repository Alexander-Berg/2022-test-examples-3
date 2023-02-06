package ru.yandex.tours.calendar.storage

import akka.util.Timeout
import org.joda.time.{DateTime, LocalDate}
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import ru.yandex.tours.calendar.storage.PriceStorage.{HotelPrice, Price}
import ru.yandex.tours.clickhouse.DefaultClickHouseClient
import ru.yandex.tours.model.BaseModel.Pansion
import ru.yandex.tours.model.search.SearchType
import ru.yandex.tours.search.settings.SearchSettingsHolder
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import ru.yandex.tours.util.http.NingHttpClient

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 30.03.16
 */
class PriceStorageIntSpec extends BaseSpec with TestData with ScalaFutures {
  implicit val timeout = Timeout(30.seconds)

  val httpClient = new NingHttpClient(None)
  val clickHouseClient = new DefaultClickHouseClient(httpClient,
    "", "http", "vertis-clickhouse01ht.vs.os.yandex.net", 8123,
    "tours", "ieWooviechai0lool8te", "tours", FiniteDuration(30, SECONDS))(global, timeout)

  val storage = new PriceStorage(clickHouseClient, data.hotelsIndex, SearchType.TOURS,
    data.regionTree, data.geoMapping, SearchSettingsHolder.empty
  )

  "PriceStorage" should {
    "save price" in {
      val res = storage.savePrices(Seq(HotelPrice(
        DateTime.now, 2, 239, LocalDate.now.plusDays(1), 6, Seq(88, 88),
        3271467, 4,
        Seq(Price(Pansion.RO, 18425))
      )))

      res.futureValue(PatienceConfiguration.Timeout(10.seconds))
    }
  }
}
