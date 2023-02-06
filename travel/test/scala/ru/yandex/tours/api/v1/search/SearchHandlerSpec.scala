package ru.yandex.tours.api.v1.search

import akka.actor.ActorSystem
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.tours.api.RouteTestWithConfig
import spray.routing.HttpService

import scala.language.reflectiveCalls

/* @author berkut@yandex-team.ru */

@RunWith(classOf[JUnitRunner])
class SearchHandlerSpec extends Matchers
with WordSpecLike
with RouteTestWithConfig
with HttpService {

//  private val hotel: Hotel = Hotel(123, // id
//    225, //geo id
//    43.5, // longitude
//    32.6, //latitude
//    Iterable((Partner.partners(5), "qazwsx222")), //partner ids
//    Map("ru" -> "Россия, Москва", "en" -> "Russia, Moscow"), //address
//    Map("en" -> "Grand Spa Hotel", "ru" -> "Гранд Спа Отель"), //name
//    None, //url
//    Star.stars(5), //stars
//    Iterable(EnumFeatureValue(Features.name2feature("hotel_type").asInstanceOf[EnumFeature], 1, Some("mini_hotel"))),
//    9.5,
//    456,
//    Iterable(Image("http://t.ru/image")))
//  val hotelSnippet = AggregatedTour(hotel, 10000, 20000, Set(Pansions.AI), 5, 7, 7, new Date(0), new Date(0))
//  val tourInfoResult = SnippetsWithInfo(10, 15, Seq(hotelSnippet))
//  val toursSearchRequest = AggregatedToursSearchRequest(213, 225, 7, new Date(0), Iterable(88))
//
//  private val searchActorProbe = new TestProbe(system) {
//    def respondSessionId(trySessionId: Try[String]) {
//      expectMsgPF(3.seconds) {
//        case req: SearchService.SearchRequest =>
//          sender ! SearchService.SearchResponse(trySessionId, req)
//      }
//    }
//
//    def respondSearchResult(result: Try[SnippetsWithInfo]): Unit = {
//      expectMsgPF(3.seconds) {
//        case req: SearchService.ResultRequest =>
//          assertEquals("123321", req.sessionId)
//          sender ! SearchService.ResultResponse(result, req)
//      }
//    }
//
//    def responseMapRequest(result: Try[Iterable[Hotel]]): Unit = {
//      expectMsgPF(3.seconds) {
//        case req: SearchService.MapInfoRequest =>
//          assertEquals("123321", req.sessionId)
//          sender ! SearchService.MapInfoResponse(result, req)
//      }
//    }
//  }
//
//  private val storageActorProbe = new TestProbe(system) {
//    def respondSearchRequest(sr: Try[Option[AggregatedToursSearchRequest]]): Unit = {
//      expectMsgPF(3.seconds) {
//        case req: ToursStorageActor.GetSearchRequest =>
//          sender ! ToursStorageActor.SearchRequestResponseGot(sr, req)
//      }
//    }
//  }
//
//  private val route = TestActorRef(new SearchHandler(searchActorProbe.ref, storageActorProbe.ref, new Tree(Iterable()))).underlyingActor.route
//
//  "Search Handler" must {
//    "create new session" in {
//      Get("/tours?from_city=213&to_country=225&nights=7&adults=2&start_date=14.12.2014&ages=88") ~> route ~> check {
//        searchActorProbe.respondSessionId(Success("123edc321"))
//        assertEquals(StatusCodes.OK, status)
//        assertTrue(responseAs[String].contains("123edc321"))
//      }
//    }
//
//    "tell about failed session" in {
//      Get("/tours?from_city=213&to_country=225&nights=7&adults=2&start_date=14.12.2014&ages=88") ~> route ~> check {
//        searchActorProbe.respondSessionId(Failure(new Exception("failed")))
//        assertEquals(StatusCodes.InternalServerError, status)
//        assertTrue(responseAs[String].contains("\"errors\":[\"Can not create search session. See logs\"]"))
//      }
//    }
//
//    "tell about search failures" in {
//      Get("/tours/123321/result?lang=ru") ~> route ~> check {
//        searchActorProbe.respondSearchResult(Failure(new Exception("fail")))
//        assertEquals(StatusCodes.InternalServerError, status)
//        assertTrue(responseAs[String].contains("\"errors\":[\"Can not get results by session id!\"]"))
//      }
//    }
//
//    "tell about storage failures in fetching search request" in {
//      Get("/tours/123321/result?lang=ru") ~> route ~> check {
//        searchActorProbe.respondSearchResult(Success(tourInfoResult))
//        storageActorProbe.respondSearchRequest(Failure(new Exception("fail")))
//        assertEquals(StatusCodes.InternalServerError, status)
//        assertTrue(responseAs[String].contains("\"errors\":[\"Can not get search request by sessionId, see logs\"]"))
//      }
//    }
//
//    "tell about unknown session ids" in {
//      Get("/tours/123321/result?lang=ru") ~> route ~> check {
//        searchActorProbe.respondSearchResult(Success(tourInfoResult))
//        storageActorProbe.respondSearchRequest(Success(None))
//        assertEquals(StatusCodes.NotFound, status)
//        assertTrue(responseAs[String].contains("\"errors\":[\"No search request matching such sessionId\"]"))
//      }
//    }
//
//    "tell about unknown session, if no aggregated tours found" in {
//      Get("/tours/123321/result?lang=ru") ~> route ~> check {
//        searchActorProbe.respondSearchResult(Success(SnippetsWithInfo(0, 0, Seq())))
//        storageActorProbe.respondSearchRequest(Success(Some(toursSearchRequest.copy(flexWhen = true, flexNights = true))))
//        assertEquals(StatusCodes.NotFound, status)
//        val response: String = responseAs[String]
//        assertTrue(response.contains("\"errors\":[\"Unknown session id\"]"))
//      }
//    }
//
//    "response with tours info result for known session ids" in {
//      Get("/tours/123321/result?lang=ru") ~> route ~> check {
//        searchActorProbe.respondSearchResult(Success(tourInfoResult))
//        storageActorProbe.respondSearchRequest(Success(Some(toursSearchRequest.copy(flexWhen = true, flexNights = true))))
//        assertEquals(StatusCodes.OK, status)
//        val response = responseAs[String]
//        //check hotel info
//        assertTrue(response.contains("\"errors\":[]"))
//        assertTrue(response.contains("\"id\":123"))
//        assertFalse(response.contains("qazwsx222"))
//        assertTrue(response.contains("\"address\":\"Россия, Москва\""))
//        assertFalse(response.contains("Russia, Moscow"))
//        assertTrue(response.contains("\"name\":\"Гранд Спа Отель\""))
//        assertFalse(response.contains("Grand Spa Hotel"))
//        assertTrue(response.contains("\"hotel_type\":\"mini_hotel\""))
//        assertTrue(response.contains("\"rating\":9.5"))
//        assertTrue(response.contains("\"reviews_count\":456"))
//        assertTrue(response.contains("\"main\":\"http://t.ru/image\""))
//
//        // check tour info
//        assertTrue(response.contains("\"price_from\":10000"))
//        assertTrue(response.contains("\"price_to\":20000"))
//        assertTrue(response.contains("[\"AI\"]"))
//        assertTrue(response.contains("\"offers_count\":5"))
//        assertTrue(response.contains("\"nights_from\":7"))
//        assertTrue(response.contains("\"nights_to\":7"))
//        assertTrue(response.contains("\"start_date_from\":\"01.01.1970\""))
//        assertTrue(response.contains("\"start_date_to\":\"01.01.1970\""))
//
//        // check progress
//        assertTrue(response.contains("\"total\":15"))
//        assertTrue(response.contains("\"done\":10"))
//
//        //check search request
//        assertTrue(response.contains("\"from_city\":213"))
//        assertTrue(response.contains("\"to_country\":225"))
//        assertTrue(response.contains("\"to_city\":2"))
//        assertTrue(response.contains("\"to_country_name\":\"Unknown\""))
//        assertTrue(response.contains("\"from_city_name\":\"Unknown\""))
//        assertTrue(response.contains("\"to_city_name\":\"Unknown\""))
//        assertTrue(response.contains("\"nights\":7"))
//        assertTrue(response.contains("\"start_date\":\"01.01.1970\""))
//        assertTrue(response.contains("\"ages\":[88]"))
//        assertTrue(response.contains("\"stars_from\":1"))
//        assertTrue(response.contains("\"stars_to\":5"))
//        assertTrue(response.contains("\"is_flexible_dates\":true"))
//        assertTrue(response.contains("\"is_flexible_nights\":true"))
//      }
//    }
//
//    "respond to map info requests" in {
//      Get("/tours/123321/map_info?lang=ru&longitude=43.5&latitude=32.6&lon_span=1&lat_span=1") ~> route ~> check {
//        searchActorProbe.responseMapRequest(Success(Iterable(hotel)))
//        assertEquals(StatusCodes.OK, status)
//        val response = responseAs[String]
//        val json = new JSONObject(response)
//        val array = json.getJSONArray("data")
//        assertEquals(1, array.length())
//        assertTrue(response.contains("Гранд Спа Отель"))
//        assertFalse(response.contains("Grand Spa Hotel"))
//      }
//    }
//
//    "tell about map info fails" in {
//      Get("/tours/123321/map_info?lang=ru&longitude=43.5&latitude=32.6&lon_span=1&lat_span=1") ~> route ~> check {
//        searchActorProbe.responseMapRequest(Failure(new Exception("fail")))
//        assertEquals(StatusCodes.InternalServerError, status)
//        assertTrue(responseAs[String].contains("\"errors\":[\"Can not get map info for tours search, see logs\"]"))
//      }
//    }
//  }

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected: Boolean = true
  override implicit def actorRefFactory: ActorSystem = system
}
