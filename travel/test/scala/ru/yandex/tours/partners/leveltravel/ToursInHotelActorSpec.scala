package ru.yandex.tours.partners.leveltravel

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import ru.yandex.tours.testkit.TestConfig

import scala.language.reflectiveCalls

/* @author berkut@yandex-team.ru */

@RunWith(classOf[JUnitRunner])
class ToursInHotelActorSpec extends TestKit(ActorSystem("tours-in-hotel-spec", TestConfig.config)) with ImplicitSender
with WordSpecLike with Matchers with BeforeAndAfterAll {

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected: Boolean = true
  override protected def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

//  val storage = new TestProbe(system) {
//    def saveToursInHotel(result: Try[Unit]) {
//      expectMsgPF() {
//        case req: ToursStorageActor.SaveToursInHotelResponse =>
//          val response = req.response
//          assertEquals(sessionId, response.sessionId)
//          assertEquals(123, response.hotelId)
//          assertEquals(Partner(5, "lt"), response.partner)
//          assertEquals(partnerRequestId, response.requestId)
//          assertEquals(0, response.tours.size)
//          assertEquals(RequestStatus.PERFORMING, response.status)
//          sender ! ToursStorageActor.ToursInHotelResponseSaved(result, req)
//      }
//    }
//
//    def respondToGetAggregatedTours(result: Try[Iterable[AggregatedToursResponse]]): Unit = {
//      expectMsgPF() {
//        case req@ToursStorageActor.GetAggregatedToursResponses("321", _) =>
//          sender() ! ToursStorageActor.AggregatedToursResponsesGot(result, req)
//      }
//    }
//
//    def saveToursInHotel(test: ToursInHotelResponse => Unit): Unit = {
//      expectMsgPF() {
//        case req@ToursStorageActor.SaveToursInHotelResponse(response, ctx) =>
//          test(response)
//          sender() ! ToursStorageActor.ToursInHotelResponseSaved(Success(), req)
//      }
//    }
//  }
//
//  val http = new TestProbe(system) {
//    def respondToGetToursInHotel(result: Try[(Int, String)]) = {
//      expectMsgPF() {
//        case req: AsyncHttpClient.Request =>
//          sender ! AsyncHttpClient.Response(result, req)
//      }
//    }
//  }
//
//  val searchService = new TestProbe(system)
//
//  val toursInHotelActor = TestActorRef(Props(new ToursInHotelActor(storage.ref, http.ref, new ToursParser, searchService.ref)))
//
//
//  val sessionId = "321"
//  val hotel = Hotel(123, 1, 1, 1, Iterable((Partner.partners(5), "40429")), Map.empty, Map.empty, None, Star.stars(1), Iterable.empty, 0, 0, Iterable.empty)
//  val otherHotel = hotel.copy(id = 111)
//  val tour = AggregatedTour(hotel, 1000, 2000, Set(), 1, 1, 2, new Date(0), new Date(0))
//  val otherTour = tour.copy(hotel = otherHotel)
//  private val partnerRequestId = "333"
//  val aggregatedTour = AggregatedToursResponse(sessionId, Partner.partners(5), "11", Some(7), Some(new Date(0)), partnerRequestId, RequestStatus.COMPLETED, Some(ToursSearchResult(List(tour, otherTour))))
//  val partnerResponse = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/tours_in_hotel.json")).getLines().mkString("")
//
//  "Tours in hotel actor" must {
//    "perform search of tours in hotel" in {
//      toursInHotelActor ! ToursInHotelActor.InitSearchBySessionIdRequest(sessionId, 123, "context")
//      storage.respondToGetAggregatedTours(Try(Iterable(aggregatedTour)))
//      storage.saveToursInHotel(Success())
//      storage.expectNoMsg(100.millis)
//      expectMsgPF() {
//        case ToursInHotelActor.InitSearchBySessionIdResponse(result, initReq) =>
//          assertTrue(result.isSuccess)
//          assertEquals("context", initReq.ctx)
//      }
//      http.respondToGetToursInHotel(Success(200, partnerResponse))
//      http.expectNoMsg(100.millis)
//      storage.saveToursInHotel(toursInHotel => {
//        assertEquals(123, toursInHotel.hotelId)
//        assertEquals(Partner(5, "lt"), toursInHotel.partner)
//        assertEquals(partnerRequestId, toursInHotel.requestId)
//        assertEquals(sessionId, toursInHotel.sessionId)
//        assertEquals(RequestStatus.COMPLETED, toursInHotel.status)
//        assertEquals(3, toursInHotel.tours.size)
//        val tour = toursInHotel.tours.head
//        assertEquals("06-6fdc9920-00ab-47f3-bf23-c3c35dd4e50a", tour.tourId)
//        assertEquals(6, tour.operator.id)
//        assertEquals(7, tour.nights)
//        assertEquals(Pansions.HB, tour.pansion)
//        assertEquals("Стандартный номер", tour.roomType)
//        assertEquals(1418500800000l, tour.startDate.getTime)
//      })
//    }
//
//    "return fail if no partner responses found" in {
//      toursInHotelActor ! ToursInHotelActor.InitSearchBySessionIdRequest(sessionId, 123, "context")
//      storage.respondToGetAggregatedTours(Failure(new Exception("Can not get responses")))
//      expectMsgPF() {
//        case ToursInHotelActor.InitSearchBySessionIdResponse(result, _) =>
//          assertTrue(result.isFailure)
//      }
//      storage.expectNoMsg(100.millis)
//      http.expectNoMsg(100.millis)
//      expectNoMsg(100.millis)
//    }
//
//    "return fail, if no 'tours in hotel' response was saved" in {
//      toursInHotelActor ! ToursInHotelActor.InitSearchBySessionIdRequest(sessionId, 123)
//      storage.respondToGetAggregatedTours(Try(Iterable(aggregatedTour)))
//      storage.saveToursInHotel(Failure(new Exception("failed")))
//      expectMsgPF() {
//        case ToursInHotelActor.InitSearchBySessionIdResponse(result, _) =>
//          assertTrue(result.isFailure)
//      }
//      storage.expectNoMsg(100.millis)
//      http.expectNoMsg(100.millis)
//      expectNoMsg(100.millis)
//    }
//
//    "handle http errors" in {
//      toursInHotelActor ! ToursInHotelActor.InitSearchBySessionIdRequest(sessionId, 123, "context")
//      storage.respondToGetAggregatedTours(Try(Iterable(aggregatedTour)))
//      storage.saveToursInHotel(Success())
//      storage.expectNoMsg(100.millis)
//      expectMsgPF() {
//        case ToursInHotelActor.InitSearchBySessionIdResponse(result, initReq) =>
//          assertTrue(result.isSuccess)
//          assertEquals("context", initReq.ctx)
//      }
//      http.respondToGetToursInHotel(Success(404, partnerResponse))
//      http.expectNoMsg(100.millis)
//      storage.saveToursInHotel(toursInHotel => {
//        assertEquals(RequestStatus.FAILED, toursInHotel.status)
//        assertEquals(0, toursInHotel.tours.size)
//      })
//      storage.expectNoMsg(100.millis)
//      http.expectNoMsg(100.millis)
//      expectNoMsg(100.millis)
//    }
//
//    "handle bad json" in {
//      toursInHotelActor ! ToursInHotelActor.InitSearchBySessionIdRequest(sessionId, 123, "context")
//      storage.respondToGetAggregatedTours(Try(Iterable(aggregatedTour)))
//      storage.saveToursInHotel(Success())
//      storage.expectNoMsg(100.millis)
//      expectMsgPF() {
//        case ToursInHotelActor.InitSearchBySessionIdResponse(result, initReq) =>
//          assertTrue(result.isSuccess)
//          assertEquals("context", initReq.ctx)
//      }
//      http.respondToGetToursInHotel(Success(200, ""))
//      http.expectNoMsg(100.millis)
//      storage.saveToursInHotel(toursInHotel => {
//        assertEquals(RequestStatus.FAILED, toursInHotel.status)
//        assertEquals(0, toursInHotel.tours.size)
//      })
//      storage.expectNoMsg(100.millis)
//      http.expectNoMsg(100.millis)
//      expectNoMsg(100.millis)
//    }
//
//    "return result" in {
//      toursInHotelActor ! ToursInHotelActor.GetResultRequest(sessionId, 123)
//      val tours = new ToursParser().parse(partnerResponse).get
//      storage.expectMsgPF() {
//        case req@ToursStorageActor.GetToursInHotelResponses(otherSessionId, hotelId, ctx) =>
//          assertEquals(sessionId, otherSessionId)
//          val toursResponse: ToursInHotelResponse = ToursInHotelResponse(sessionId, hotelId, Partner(5, "lt"), partnerRequestId, RequestStatus.COMPLETED, tours)
//          toursInHotelActor ! ToursStorageActor.ToursInHotelResponseGot(Success(Iterable(toursResponse, toursResponse)), req)
//      }
//      expectMsgPF() {
//        case ToursInHotelActor.GetResultResponse(result, _) =>
//          assertTrue(result.isSuccess)
//          val toursResponse = result.get
//          assertTrue(toursResponse.finished)
//          assertEquals(6, toursResponse.tours.size)
//      }
//    }
//  }

}
