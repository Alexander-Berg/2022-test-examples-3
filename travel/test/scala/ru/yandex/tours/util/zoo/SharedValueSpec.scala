package ru.yandex.tours.util.zoo

import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.Retries
import org.scalatest.concurrent.AsyncAssertions.Waiter
import org.scalatest.concurrent.{IntegrationPatience, Eventually}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.tagobjects.Retryable
import ru.yandex.tours.testkit.{BaseSpec, ZooKeeperAware}

import scala.concurrent.duration._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 27.06.15
 */
class SharedValueSpec extends BaseSpec with ZooKeeperAware with Eventually with IntegrationPatience with Retries {

  "Shared value" should {
    "share same value" in {
      val value1 = new SharedValue(curatorBase, "/tours-ut/shared-value", 0, IntSerializer)
      val value2 = new SharedValue(curatorBase, "/tours-ut/shared-value", 0, IntSerializer)

      eventually(Timeout(30.second)) { value1.set(0) }

      value1.set(1)
      eventually { value1.get shouldBe 1 }
      eventually { value2.get shouldBe 1 }

      value1.set(2)
      eventually { value1.get shouldBe 2 }
      eventually { value2.get shouldBe 2 }

      value2.set(3)
      eventually { value2.get shouldBe 3 }
      eventually { value1.get shouldBe 3 }
    }
    "subscribe for changes" taggedAs Retryable in {
      pending
      val value1 = new SharedValue(curatorBase, "/tours-ut/shared-value2", 0, IntSerializer)
      eventually(Timeout(15.second)) { value1.set(1) }
      eventually(Timeout(15.second)) { value1.get shouldBe 1 }

      val newValue = 99
      val got = new AtomicInteger(-1)
      value1.onUpdate { i =>
        got.set(i)
      }
      value1.set(newValue)
      eventually(Timeout(10.seconds)) { got.get shouldBe newValue }
    }
  }

  override def withFixture(test: NoArgTest) = {
    if (isRetryable(test))
      withRetry {
        super.withFixture(test)
      }
    else
      super.withFixture(test)
  }
}
