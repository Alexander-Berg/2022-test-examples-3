package ru.yandex.tours.wizard.parser

import org.scalatest.Inspectors
import org.scalatest.matchers.{MatchResult, Matcher}
import ru.yandex.tours.query.parser.ParsingTrie
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import ru.yandex.tours.util.parsing.{IntValue, Tabbed}
import ru.yandex.tours.wizard.domain.ToursWizardRequest
import ru.yandex.tours.wizard.geoaddr.GeoAddrPragmatics
import ru.yandex.tours.wizard.query.{PragmaticsParser, WizardRequest}

import scala.io.Source

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 26.01.15
 */
class WizardRequestParserIntSpec extends BaseSpec with Inspectors with TestData {
  val geoAddrPragmatics = new GeoAddrPragmatics(data.geoMapping)

  val pragmaticParser = new PragmaticsParser(ParsingTrie.fromFile(root / "wizard.trie"))
  val reqAnsParser = new PragmaticsParser(ParsingTrie.fromFile(root / "wizard.reqans.trie"))
  val stopWordsParser = new PragmaticsParser(ParsingTrie.fromFile(root / "stop_words.trie"))

  val pragmaticsParser = new UserRequestParser(
    pragmaticParser,
    reqAnsParser,
    stopWordsParser,
    data.regionTree,
    data.hotelsIndex,
    data.hotelRatings,
    data.geoMapping,
    data.directionsStats
  )
  val departures = data.departures

  val parser = new WizardRequestParser(
    BadQueryReporter.empty,
    geoAddrPragmatics,
    pragmaticsParser,
    departures,
    data.hotelRatings
  )

  private val spb = 2
  private val moscow = 213
  private val turkey = 983
  private val egypt = 1056
  private val thailand = 995
  private val crime = 977

  private val userRegion = moscow

  private def parseOpt(query: String): Option[ToursWizardRequest] = parser.parse(WizardRequest(userRegion, query, ""))
  private def parse(query: String): ToursWizardRequest = parseOpt(query).value

  "WizardRequestParser" should {
    "ignore queries without markers" in {
      parseOpt("яндекс") shouldBe None
      parseOpt("вконтакте") shouldBe None
    }
    "ignore queries with stop words" in {
      parseOpt("тур де франс 2015 год") shouldBe None
      parseOpt("1 тур чемпионата россии по футболу") shouldBe None
      parseOpt("2 тур чемпионата мира по шашкам") shouldBe None
    }
    "parse destination inside request" in {
      parse("Туры в Турцию") should haveTo(turkey)
      parse("Отдых в Египте") should haveTo(egypt)
      parse("Путевка в Таиланд") should haveTo(thailand)
      parse("отдых в крыму") should (haveTo(crime) and haveNoHotel)
    }
    "use user region as departure city" in {
      parse("путевка в Таиланд") should haveFrom(userRegion)
    }
    "parse departure inside request" in {
      parse("путевка в Таиланд из Санкт-Петербурга") should (haveParsedDeparture(spb) and haveFrom(spb))
    }
    "parse departure in request" in {
      parse("туры в крым тула") should (haveParsedDeparture(15) and haveFrom(213))
      parse("туры в египет ярославль") should (haveParsedDeparture(16) and haveFrom(213))
    }
    "parse arrival for hotel requests" in {
      parse("отели санкт-петербурга") should haveTo(spb)
//      TODO
//      parse("гостиницы в россоши воронежской области") should haveTo(10681)
    }
    "parse operator inside request" in {
      parse("таиланд от пегаса") should haveOperator(4)
      parse("в египет от библиоглобуса") should haveOperator(7)
    }

    "parse hotel in request" in {
      forEvery(Seq(
//        "отель москва" -> 1874878, TODO: Fix
        "отель санкт-петербург" -> 1874791,
        "ялта интурист" -> 1013207,
        "seher sun palace resort and spa" -> 1001460,
        "jasmine palace resort spa" -> 1000883,
        "санаторий светлана сочи официальный сайт цены на 2015 год" -> 1014319,
        "continental garden reef resort 5" -> 1000433,
        "пансионат звёздный судак официальный сайт" -> 1013282,
//        "ares hotel 4" -> 1002795,
        "coral beach resort 4" -> 1008332,
        "holiday city hotel" -> 1001499,
        "holiday city hotel 4" -> 1001499
      )) { case (query, hotel) =>
        parse(query) should haveHotel(hotel)
      }


      //      parse("royal oasis naama bay hotel resort") should haveHotel(1000535)
      //      parse("санаторий лермонтова пятигорск официальный сайт цены на 2015 год") should haveHotel(1000381)
      //      parse("gardenia plaza hotels resorts") should haveHotel(1000461)
      //      parse("dream world resort spa") should haveHotel(1001543)
      //      parse("larissa green hill hotel") should haveHotel(1003089)
      //      parse("dream world resort spa 5") should haveHotel(1001543)
    }

    "not parse trash" in {
      parseOpt("helios 40") shouldBe None
      parseOpt("м") shouldBe None
      parseOpt("russie") shouldBe None
      parseOpt("c mount") shouldBe None
//      parseOpt("King of the World") shouldBe None
      parseOpt("FACE") shouldBe None
      parseOpt("xxl") shouldBe None
//      parseOpt("the power of love") shouldBe None
    }

    "parse hotel fast (performance)" in {
      pending
      //warming
      for (_ <- 0 until 200) {
        parse("санаторий светлана сочи официальный сайт цены на 2015 год") should haveHotel(1014319)
      }

      for (_ <- 0 until 1000) {
        val started = System.currentTimeMillis()
        parse("санаторий светлана сочи официальный сайт цены на 2015 год") should haveHotel(1014319)
        val elapsed = System.currentTimeMillis() - started
        println(s"Elapsed: $elapsed ms.")
      }
    }

    "parse hotel in request #2" in {
      val HotelUrl = "https://.*\\.yandex\\.ru/hotel/([0-9]+).*".r
      val hotels = Source.fromInputStream(getClass.getResourceAsStream("/hotels_not_shown.tsv")).getLines().map {
        case Tabbed(request, _, _, _, HotelUrl(IntValue(hotelId))) => request -> hotelId
        case Tabbed(request, HotelUrl(IntValue(hotelId))) => request -> hotelId
        case Tabbed(request, IntValue(hotelId)) => request -> hotelId
      }.toList

      var ok = 0
      var total = 0
      hotels.foreach { case (request, hotelId) =>
        val defined = data.hotelsIndex.getHotelById(hotelId).isDefined
        if (defined) {
          total += 1
        }
        val result = parseOpt(request)
        if (result.flatMap(_.hotel).contains(hotelId)) ok += 1
        else if (defined) println(s"[$request] Expected: $hotelId Got: ${result.flatMap(_.hotel)}")
      }

      println(s"Total: $ok out of $total")
    }
  }

  private def haveTo(expected: Int) = Matcher[ToursWizardRequest](request =>
    MatchResult(
      request.to.contains(expected),
      s"[${request.originalRequest.trim}].to != $expected but == ${request.to}",
      s"[${request.originalRequest.trim}].to == $expected"
    )
  )
  private def haveFrom(expected: Int) = Matcher[ToursWizardRequest](request =>
    MatchResult(
      request.from == expected,
      s"[${request.originalRequest.trim}].from != $expected but == ${request.from}",
      s"[${request.originalRequest.trim}].from == $expected"
    )
  )
  private def haveOperator(expected: Int) = Matcher[ToursWizardRequest](request =>
    MatchResult(
      request.operator.contains(expected),
      s"[${request.originalRequest.trim}].operator != $expected but == ${request.operator}",
      s"[${request.originalRequest.trim}].operator == $expected"
    )
  )
  private def haveHotel(expected: Int) = Matcher[ToursWizardRequest](request =>
    MatchResult(
      request.hotel.contains(expected),
      s"[${request.originalRequest.trim}].hotel != $expected but == ${request.hotel}",
      s"[${request.originalRequest.trim}].hotel == $expected"
    )
  )
  private def haveNoHotel = Matcher[ToursWizardRequest](request =>
    MatchResult(
      request.hotel.isEmpty,
      s"[${request.originalRequest.trim}].hotel is ${request.hotel}",
      s"[${request.originalRequest.trim}].hotel is not defined"
    )
  )
  private def haveParsedDeparture(expected: Int) = Matcher[ToursWizardRequest](request =>
    MatchResult(
      request.regionInRequest.contains(expected),
      s"[${request.originalRequest.trim}].regionInRequest != $expected but == ${request.regionInRequest}",
      s"[${request.originalRequest.trim}].regionInRequest == $expected"
    )
  )
}
