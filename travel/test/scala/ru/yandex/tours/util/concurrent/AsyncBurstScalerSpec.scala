package ru.yandex.tours.util.concurrent

import org.scalatest.concurrent.ScalaFutures
import ru.yandex.tours.testkit.{TestTicker, BaseSpec}

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 22.12.15
 */
class AsyncBurstScalerSpec extends BaseSpec with ScalaFutures {

  val ticker = new TestTicker()
  val scaler = new AsyncBurstScaler[String](1.second, ticker)

  "AsyncBurstScaler" should {
    "determine if it should work" in {
      val key = "determine if it should work"

      scaler.shouldWork(key, ticker.read()) shouldBe true
      scaler.shouldWork(key, ticker.read()) shouldBe false

      ticker.advance(500.millis)
      scaler.shouldWork(key, ticker.read()) shouldBe false

      ticker.advance(500.millis)
      scaler.shouldWork(key, ticker.read()) shouldBe true
      scaler.shouldWork(key, ticker.read()) shouldBe false
    }

    "throttle work" in {
      var called = 0
      def work(): Future[Unit] = { called += 1; Future.successful(()) }

      val key = "throttle work"

      scaler.throttle(key) { work() }.futureValue
      called shouldBe 1

      scaler.throttle(key) { work() }.futureValue
      called shouldBe 1

      ticker.advance(1.second)
      scaler.throttle(key) { work() }.futureValue
      called shouldBe 2
    }
  }
}
