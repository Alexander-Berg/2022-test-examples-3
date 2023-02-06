package ru.yandex.tours.partners.leveltravel

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import ru.yandex.tours.testkit.TestConfig

/* @author berkut@yandex-team.ru */

@RunWith(classOf[JUnitRunner])
class CheckStatusActorSpec extends TestKit(ActorSystem("check-status-spec", TestConfig.config)) with ImplicitSender
with WordSpecLike with Matchers with BeforeAndAfterAll {

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected: Boolean = true
  override protected def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

//  val request = HotelSearchRequest(213, 1056, 7, LocalDate.now, Iterable(88, 88))
//
//  "Check status actor" must {
//    "Make requests to http actor and partners fetch actor" in {
//      val checkStatusActor = TestActorRef(Props(new HotelDownloader(testActor, testActor, 50.millisecond)))
//      checkStatusActor ! HotelDownloader.ScheduleRequest(request, Some(7), None, "111", Set("1", "2"))
//      expectMsgPF(300.millisecond) {
//        case req@AsyncHttpClient.Request(url, _, _, _) =>
//          checkStatusActor ! AsyncHttpClient.Response(Success((200, response2)), req)
//      }
//      expectMsgPF(300.millisecond) {
//        case req@AsyncHttpClient.Request(url, _, _, _) =>
//          checkStatusActor ! AsyncHttpClient.Response(Success((200, response1)), req)
//      }
//      expectMsgPF(300.millisecond) {
//        case SnippetFetcherActor.FetchResult(_, _, _, _, operatorId, _) =>
//          assertEquals("2", operatorId)
//      }
//      expectMsgPF(300.millisecond) {
//        case req@AsyncHttpClient.Request(url, _, _, _) =>
//          checkStatusActor ! AsyncHttpClient.Response(Success((200, response0)), req)
//      }
//      expectMsgPF(300.millisecond) {
//        case SnippetFetcherActor.FetchResult(_, _, _, _, operatorId, _) =>
//          assertEquals("1", operatorId)
//      }
//      expectNoMsg(500.millisecond)
//    }
//
//    "Should fail after some retries if partner fails" in {
//      val checkStatusActor = TestActorRef(Props(new HotelDownloader(testActor, testActor, 100.millisecond)))
//      val retries = 5
//      checkStatusActor ! HotelDownloader.ScheduleRequest(request, Some(7), None, "112", Set("1"), retries - 1)
//      for (i <- 0 until retries) {
//        expectMsgPF(200.millisecond) {
//          case req@AsyncHttpClient.Request(_, _, _, _) =>
//            checkStatusActor ! AsyncHttpClient.Response(Success((404, "Not found")), req)
//        }
//      }
//      expectNoMsg(1.second)
//    }
//
//    "Should fail after some retries if partner returnes bad response" in {
//      val checkStatusActor = TestActorRef(Props(new HotelDownloader(testActor, testActor, 100.millisecond)))
//      val retries = 5
//      checkStatusActor ! HotelDownloader.ScheduleRequest(request, Some(7), None, "113", Set("1"), retries - 1)
//      for (i <- 0 until retries) {
//        expectMsgPF(200.millisecond) {
//          case req@AsyncHttpClient.Request(_, _, _, _) =>
//            checkStatusActor ! AsyncHttpClient.Response(Success((200, "Invalid json")), req)
//        }
//      }
//      expectNoMsg(1.second)
//    }
//  }
//
//  val response2 = """{
//                    |  "partner"       : "ya",
//                    |  "request_id"    : "111",
//                    |  "status"        : {
//                    |    1: "pending",
//                    |    2: "performing",
//                    |  },
//                    |}""".stripMargin
//
//  val response1 = """{
//                    |  "partner"       : "ya",
//                    |  "request_id"    : "111",
//                    |  "status"        : {
//                    |    1: "performing",
//                    |    2: "completed",
//                    |  },
//                    |}""".stripMargin
//
//  val response0 = """{
//                    |  "partner"       : "ya",
//                    |  "request_id"    : "111",
//                    |  "status"        : {
//                    |    1: "no_results",
//                    |    2: "completed",
//                    |  },
//                    |}""".stripMargin
}
