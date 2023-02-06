package ru.yandex.tours.api

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import spray.routing.RequestContext

import scala.concurrent.duration._

/* @author berkut@yandex-team.ru */

/** Helps to write test probes on different routers
  */
class RequestContextProbe(system: ActorSystem) extends TestProbe(system) {
  def expectHttp(check: RequestContext => Boolean,
                 complete: RequestContext => Unit,
                 timeout: Duration = 3.seconds) = {
    expectMsgPF(timeout) {
      case req: RequestContext if check(req) =>
        complete(req)
        true
    }
  }
}
