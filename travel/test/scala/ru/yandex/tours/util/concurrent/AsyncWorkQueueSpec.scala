package ru.yandex.tours.util.concurrent

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, EitherValues}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import ru.yandex.tours.testkit.BaseSpec

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 11.11.15
 */
class AsyncWorkQueueSpec extends TestKit(ActorSystem("async-work-queue-spec", ConfigFactory.empty())) with BaseSpec with BeforeAndAfterAll with ScalaFutures with EitherValues {

  import system.dispatcher

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  def future() = Future { val started = System.currentTimeMillis(); Thread.sleep(1000); started }

  "AsyncWorkQueue" should {
    "submit future" in {
      val queue = new AsyncWorkQueue(4)(system, system.dispatcher)

      queue.submit(Future.successful(1)).futureValue shouldBe 1
      queue.submit(Future { 2 }).futureValue shouldBe 2
      queue.submit(Future { 4 * 4 }).futureValue shouldBe 16
    }
    "limit number of executed futures" in {
      val queue = new AsyncWorkQueue(1)(system, system.dispatcher)

      val f1 = queue.submit(future())
      val delayed = queue.submit(future())

      val f1Started = f1.futureValue(Timeout(1500.millis))

      delayed.futureValue(Timeout(1.second)) shouldBe (f1Started + 1000) +- 100
    }
    "cancel futures by deadline" in {
      val queue = new AsyncWorkQueue(1)(system, system.dispatcher)

      val f1 = queue.submit(future())
      val delayed = queue.submit(future(), deadline = 500.millis)

      f1.futureValue(Timeout(1500.millis))

      a[DeadlineReachedException] shouldBe thrownBy {
        Await.result(delayed, 1.second)
      }
    }
  }
}
