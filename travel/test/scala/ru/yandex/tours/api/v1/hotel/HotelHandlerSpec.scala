package ru.yandex.tours.api.v1.hotel

import java.math.RoundingMode
import java.text.DecimalFormat

import akka.testkit.{TestActorRef, TestProbe}
import org.json.JSONObject
import org.junit.Assert._
import ru.yandex.tours.agencies.AgenciesIndex
import ru.yandex.tours.api._
import ru.yandex.tours.avia.AviaAirportRecommendations
import ru.yandex.tours.backend.HotelMinPricePreparer
import ru.yandex.tours.geo.base.region.Tree
import ru.yandex.tours.geo.mapping.GeoMappingHolder
import ru.yandex.tours.hotels.MemoryHotelsIndex
import ru.yandex.tours.model.hotels.Features._
import ru.yandex.tours.model.hotels._
import ru.yandex.tours.resorts.SkiResorts
import ru.yandex.tours.search.settings.SearchSettingsHolder
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import ru.yandex.tours.util.spray.RouteesContext
import ru.yandex.tours.model.Languages._
import spray.http.StatusCodes
import spray.routing.{HttpServiceBase, Route}

/* @author berkut@yandex-team.ru */
class HotelHandlerSpec extends BaseSpec with RouteTestWithConfig with HttpServiceBase with TestData {
  private val hotel = data.randomHotel()

  private val longitudeDiff = 20
  private val otherLongitude =
    if (hotel.longitude < 160) hotel.longitude + longitudeDiff
    else hotel.longitude - longitudeDiff

  val hotelsIndex = new MemoryHotelsIndex(Seq(
    hotel,
    hotel.copy(id = hotel.id + 1, longitude = otherLongitude)
  ), Tree.empty, data.hotelRatings)

  val toursInHotelActor = TestProbe()
  val agenciesIndex = mock[AgenciesIndex]
  val operators = data.tourOperators
  val hotelsVideo = data.hotelsVideo
  val hotelsPanoramas = data.hotelsPanoramas
  val backaPermalinks = data.backaPermalinks

  private val routeesContext = new RouteesContext(data.departures, data.regionTree, None)
  private val geoMapping = mock[GeoMappingHolder]
  private val searchSettings = mock[SearchSettingsHolder]
  private val tree = new Tree(Iterable.empty)
  private val nopRoute: Route = _ => sys.error("nop route")
  private val route = TestActorRef(new HotelHandler(
    routeesContext,
    hotelsIndex,
    tree,
    geoMapping,
    searchSettings,
    null,
    _ => nopRoute,
    _ => nopRoute,
    mock[HotelMinPricePreparer],
    hotelsVideo,
    hotelsPanoramas,
    AviaAirportRecommendations.empty,
    SkiResorts.empty)).underlyingActor.route

  "Hotel Handler" must {

    "return no info about unknown hotel" in {
      Get("/456/info?lang=ru") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        assertTrue(responseAs[String].contains("\"errors\":[\"Unknown hotel\"]"))
      }
    }

    if (hotel.addresses.contains(ru)) checkHotelInfoInLanguage(hotel, ru)
    if (hotel.addresses.contains(en)) checkHotelInfoInLanguage(hotel, en)

    "return near hotels" in {
      Get(s"/${hotel.id}/near_hotels?lang=ru&lon_span=${longitudeDiff * 2}&lat_span=0.1&longitude=${hotel.longitude}&latitude=${hotel.latitude}") ~> route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[String]
        val json = new JSONObject(response)
        val array = json.getJSONArray("data")
        assertEquals(2, array.length())
      }
    }

    "return itself if span is too small" in {
      Get(s"/${hotel.id}/near_hotels?lang=ru&lon_span=0.1&lat_span=0.1&longitude=${hotel.longitude}&latitude=${hotel.latitude}") ~> route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[String]
        val json = new JSONObject(response)
        val array = json.getJSONArray("data")
        assertEquals(1, array.length())
      }
    }

    "return no near hotels for unknown hotel" in {
      Get(s"/${hotel.id + 2}/near_hotels?lang=ru&lon_span=0.1&lat_span=0.1&longitude=${hotel.longitude}&latitude=${(hotel.latitude + 0.3) % 90}") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "return info in needed language in response for near hotels" in {
      Get(s"/${hotel.id}/near_hotels?lang=ru&lon_span=0.1&lat_span=0.1&longitude=${hotel.longitude}&latitude=${hotel.latitude}") ~> route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[String]
        val json = new JSONObject(response)
        val array = json.getJSONArray("data")
        assertEquals(1, array.length())
        val name = array.getJSONObject(0).getString("name")
        assertEquals(hotel.name(ru), name)
      }
    }
  }

  private def checkHotelInfoInLanguage(hotel: Hotel, lang: Lang): Unit = {
    val formatter = new DecimalFormat("0.##")
    formatter.setRoundingMode(RoundingMode.DOWN)

    s"return info in $lang if $lang language specified" in {
      Get(s"/${hotel.id}/info?lang=$lang&from=213") ~> route ~> check {
        val response = responseAs[String]
        status shouldBe StatusCodes.OK
        response should include ("\"errors\":[]")
        response should include (s""""id":${hotel.id}""")
        for (PartnerInfo(partner, hotelName, _, _, _) <- hotel.partnerIds) {
          if (partner != Partners.backa) {
            response shouldNot include (hotelName)
          }
        }
        response should include (s""""country":"${hotel.addresses(lang).country}"""")
        response should include (s""""city":"${hotel.addresses(lang).locality}"""")
        response should include (s""""name":"${hotel.name(lang)}"""")
        hotel.features.foreach {
          case EnumFeatureValue(EnumFeature(n, _), Some(v)) => response should include ( s""""$n":"$v"""")
          case IntFeatureValue(IntFeature(n), Some(v)) => response should include ( s""""$n":$v""")
          case BooleanFeatureValue(BooleanFeature(n), Some(v)) => response should include ( s""""$n":$v""")
          case _ =>
        }
        response should include (s""""rating":${formatter.format(hotel.rating).replace(',', '.')}""")
        response should include (s""""reviews_count":${hotel.reviewsCount}""")
//        for (image <- hotel.images) {
//          response should include (s""""main":"${image.fullSize}"""")
//        }
      }
    }
  }
}
