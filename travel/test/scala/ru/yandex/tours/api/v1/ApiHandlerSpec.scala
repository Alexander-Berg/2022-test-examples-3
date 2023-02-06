package ru.yandex.tours.api.v1

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.tours.api.{RequestContextProbe, RouteTestWithConfig}
import ru.yandex.tours.testkit.BaseSpec
import spray.routing.HttpService

import scala.language.reflectiveCalls

/* @author berkut@yandex-team.ru */

@RunWith(classOf[JUnitRunner])
class ApiHandlerSpec extends BaseSpec with RouteTestWithConfig with HttpService {

  private val probe = new RequestContextProbe(system)

  /** Route of handler under test */
  private val route = TestActorRef(new ApiHandler(probe.ref, probe.ref, probe.ref, probe.ref, probe.ref, probe.ref,
    probe.ref, probe.ref, probe.ref)).underlyingActor.route

  simpleCheck("search")
  simpleCheck("recommend")
  simpleCheck("statistic")
  simpleCheck("hotel")
  simpleCheck("agencies")
  simpleCheck("reference")
  simpleCheck("subscriptions")
  simpleCheck("resorts")
  simpleCheck("users")

  def simpleCheck(routeName: String): Unit = {
    s"/$routeName" should {
      s"route to $routeName handler" in {
        Get(s"/api/1.x/$routeName") ~> route ~> check {
          probe.expectHttp(
            req => req.unmatchedPath.isEmpty,
            req => req.complete("OK"))
          responseAs[String] should be("OK")
        }
      }
    }
  }

  implicit def actorRefFactory: ActorSystem = system
}
