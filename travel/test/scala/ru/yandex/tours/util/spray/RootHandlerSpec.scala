package ru.yandex.tours.util.spray

import akka.actor.ActorDSL._
import akka.actor.{ActorSystem, Props}
import akka.routing.RoundRobinPool
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.Matchers._
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import spray.httpx.RequestBuilding.Get


/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 22.04.15
 */
class RootHandlerSpec extends TestKit(ActorSystem("root-handler-spec", ConfigFactory.empty())) with WordSpecLike with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
  }

  private val notStartingActorProps = Props(new Act {
    whenStarting { sys.error("error in preStart") }
  })

  "RootHandler" should {
    "create user specified handler" in {
      var created = false
      val props = Props(new Act {
        whenStarting { created = true }
      })

      val rootProps = RootHandler.props(props)
      system.actorOf(rootProps, "root-handler-1")

      awaitAssert(created shouldBe true)
    }

    "stop if user's handler is failed to start" in {
      val rootProps = RootHandler.props(notStartingActorProps)
      val actor = system.actorOf(rootProps, "root-handler-2")
      watch(actor)
      expectTerminated(actor)
    }

    "stop if user's handler is failed to start (pool version)" in {
      val rootProps = RootHandler.props(notStartingActorProps.withRouter(RoundRobinPool(4)))
      val actor = system.actorOf(rootProps, "root-handler-3")
      watch(actor)
      expectTerminated(actor)
    }

    "not stop if user's handler throws exception in receive" in {
      var created = false
      var received = false
      val failingActorProps = Props(new Act {
        created = true
        become { case _ =>
          received = true
          sys.error("error in receive")
        }
      })

      val rootProps = RootHandler.props(failingActorProps)
      val actor = system.actorOf(rootProps, "root-handler-4")
      watch(actor)

      actor ! Get("/")
      awaitAssert(received shouldBe true)
      expectNoMsg()
    }
  }

}
