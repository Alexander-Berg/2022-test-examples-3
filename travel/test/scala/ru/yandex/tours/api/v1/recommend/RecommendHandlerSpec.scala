package ru.yandex.tours.api.v1.recommend

import akka.actor.ActorSystem
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{ConfigMap, Matchers, WordSpecLike}
import ru.yandex.tours.api.RouteTestWithConfig
import spray.routing.HttpService

import scala.language.reflectiveCalls

/* @author berkut@yandex-team.ru */

@RunWith(classOf[JUnitRunner])
class RecommendHandlerSpec extends Matchers
with WordSpecLike
with RouteTestWithConfig
with HttpService {
//TODO TEST!!!
//  private val aggregatedRequest = HotelSearchRequest(213, 1056, 7, LocalDate.now, Iterable(88))
//  private val request = TourSearchRequest(aggregatedRequest, 123)
//  private val hotel: Hotel = Hotel(123, // id
//    1056, //geo id
//    43.5, // longitude
//    32.6, //latitude
//    Iterable((Partner.partners(5), "qazwsx222")), //partner ids
//    LocalizedString("ru" -> "Россия", "en" -> "Russia"), //country
//    LocalizedString("ru" -> "Москва", "en" -> "Moscow"), //city
//    LocalizedString(), // address
//    LocalizedString("en" -> "Grand Spa Hotel", "ru" -> "Гранд Спа Отель"), //name
//    None, //url
//    Star.stars(5), //stars
//    Iterable(EnumFeatureValue(Features.name2feature("hotel_type").asInstanceOf[EnumFeature], 1, Some("mini_hotel"))),
//    9.5,
//    456,
//    Iterable(Image("http://t.ru/image")))
//  val hotelsIndex = new HotelsIndex(Iterable(hotel))
//  val tree = new Tree(Iterable(Region(1056, region.Types.Country, 1056, LocalizedString("ru" -> "Египет"), Set.empty, 0, 0, 0, 0, 0)))
//  val recommendService = new TestProbe(system) {
//    def reply() {
//      expectMsgPF(3.seconds) {
//        case _ =>
//          sender ! LocalRecommendService.Response(Success(Iterable((request, tour, Iterable.empty))))
//      }
//    }
//  }
//
//  val geoMappingHolder = new GeoMappingHolder(Map.empty, Map.empty)
//
//  private val route = TestActorRef(new RecommendHandler(hotelsIndex, tree, recommendService.ref, geoMappingHolder)).underlyingActor.route
//
//  //  AggregatedToursSearchRequest(213, 225, 7, new Date(System.currentTimeMillis()), Iterable(88, 88))
//  "Recommend handler" must {
//    "recommend tours" in {
//      Get("?from=213&user_id=123&lang=ru") ~> route ~> check {
//        recommendService.reply()
//        assertEquals(StatusCodes.OK, status)
//
//        val rawResponse = responseAs[String]
//        val response = new JSONObject(rawResponse)
//        assertEquals(0, response.getJSONArray("errors").length())
//        val data = response.getJSONArray("data").getJSONObject(0)
//        val tours = data.getJSONArray("tours")
//        assertEquals(1, tours.length())
//        val recommendation: JSONObject = tours.getJSONObject(0)
//        val tour = recommendation.getJSONObject("tour")
//        val hotel = tour.getJSONObject("hotel")
//        assertEquals(123, hotel.getInt("id"))
//        assertEquals("Россия", hotel.getString("country"))
//        assertEquals("Москва", hotel.getString("city"))
//        assertFalse(rawResponse.contains("qazwsx222"))
//        assertEquals("Гранд Спа Отель", hotel.getString("name"))
//        assertFalse(rawResponse.contains("Grand Spa Hotel"))
//        val features = hotel.getJSONObject("features")
//        assertEquals("mini_hotel", features.getString("hotel_type"))
//        assertEquals(9.5, hotel.getDouble("rating"), 0.001)
//        assertEquals(456, hotel.getInt("reviews_count"))
//        val images = hotel.getJSONArray("images")
//        assertEquals(1, images.length())
//        assertEquals("http://t.ru/image", images.getJSONObject(0).getString("main"))
//
//        val searchRequest = recommendation.getJSONObject("search_request")
//        assertEquals(213, searchRequest.getInt("from"))
//        val to = searchRequest.getJSONObject("to")
//        assertEquals(1056, to.getInt("id"))
//        assertEquals(123, to.getInt("hotel_id"))
//        assertEquals("Египет", to.getString("name"))
//        assertEquals(7, searchRequest.getInt("nights"))
//        assertEquals(1, searchRequest.getJSONArray("ages").length())
//        assertFalse(searchRequest.getBoolean("when_flex"))
//        assertFalse(searchRequest.getBoolean("nights_flex"))
//        val filters = recommendation.getJSONObject("filters")
//        assertEquals(0, filters.keySet().size)
//      }
//    }
//  }

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected: Boolean = true
  override implicit def actorRefFactory: ActorSystem = system
}
