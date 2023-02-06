package ru.yandex.tours.indexer.metro

import java.util.zip.GZIPInputStream

import org.scalatest.Inspectors
import ru.yandex.tours.testkit.BaseSpec
import ru.yandex.tours.util.IO

import scala.collection.JavaConversions._

class MetroJsonParserSpec extends BaseSpec with Inspectors {
  "Metro json parser" should {
    "parse export" in {

      val source = IO.readLines(new GZIPInputStream(getClass.getResourceAsStream("/metro.json.gz"))).mkString
      val result = MetroJsonParser.parseFromJson(source)
      result shouldBe 'success
      val metros = result.get
      metros should have size 435
      val someMetros = metros.filter(t => t.getId == 20449 && t.getGeoId == 213)

      someMetros should have size 3
      forEvery(someMetros) { metro ⇒
        metro.getPoint.getLatitude shouldBe 55.743887
        metro.getPoint.getLongitude shouldBe 37.567285
        Set("#0099cc", "#003399", "#7f0000") should contain (metro.getColor)
        metro.getGeoId shouldBe 213
        metro.getNameCount shouldBe 5

        val stationName = metro.getNameList.find(_.getLang == "ru").get.getValue

        metro.getColor match {
          case "#003399" ⇒ stationName shouldBe "Киевская (Арбатско-Покровская линия)"
          case "#7f0000" ⇒ stationName shouldBe "Киевская (Кольцевая линия)"
          case "#0099cc" ⇒ stationName shouldBe "Киевская (Филёвская линия)"
        }
      }
    }
  }
}
