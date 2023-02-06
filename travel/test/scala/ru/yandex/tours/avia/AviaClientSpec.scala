package ru.yandex.tours.avia

import play.api.libs.json.Json
import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 10.02.16
 */
class AviaClientSpec extends BaseSpec {
  import AviaClient._

  val response =
    """{
      |  "status": "success",
      |  "data": {
      |    "min_prices": [
      |      {
      |        "date": "2016-02-11",
      |        "indirect": {
      |          "variants": [{
      |              "forward": [
      |                "5682b084cf79cb89c56530e1",
      |                "5682b085cf79cb89c56530e2"
      |              ]
      |            }],
      |          "tariff": {
      |            "currency": "RUR",
      |            "price": 13715.00
      |          }
      |        },
      |        "direct": {
      |          "variants": [{
      |              "backward": [
      |                "5682b085cf79cb89c56530e3"
      |              ]
      |            }],
      |          "tariff": {
      |            "currency": "RUR",
      |            "price": 9969.00
      |          }
      |        }
      |      }
      |    ],
      |    "flights": [],
      |    "companies": []
      |  }
      |}
    """.stripMargin

  "AviaClient" should {
    "parse response" in {
      Json.parse(response).as[Response] shouldBe Response(
        MinPrices(
          Some(MinPrice(
            Seq(FlightVariant(Seq("5682b084cf79cb89c56530e1", "5682b085cf79cb89c56530e2"), Seq.empty)),
            Tariff("RUR", 13715)
          )),
          Some(MinPrice(
            Seq(FlightVariant(Seq.empty, Seq("5682b085cf79cb89c56530e3"))),
            Tariff("RUR", 9969)
          )),
          Seq.empty,
          Seq.empty
        ),
        None
      )
    }
  }
}
