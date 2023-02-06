package ru.yandex.tours.subscriptions.storage

import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import ru.yandex.tours.model.subscriptions.Uid
import ru.yandex.tours.storage.subscriptions.HttpSubscriptionStorage
import ru.yandex.tours.testkit.BaseSpec
import ru.yandex.tours.util.http.NingHttpClient

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 20.10.15
 */
class HttpSubscriptionStorageIntSpec extends BaseSpec with ScalaFutures {
  private implicit val timeout = Timeout(10.seconds)
  val httpClient = new NingHttpClient(Option.empty)
  val storage = new HttpSubscriptionStorage(httpClient, "csbo1ft.yandex.ru", 36134)

  implicit val config = PatienceConfig(10.seconds, 100.millis)

  "HttpSubscriptionStorage" should {
    "load one subscription" in {
      val subscription = storage.getSubscription(Uid(4551191), "ef0062842f23f049c0ac9ec27dc36cf3f36c703b").futureValue
      println(subscription)
    }
    "load user subscriptions" in {
      val subscriptions = storage.getSubscriptions(Uid(4551191)).futureValue
      println(subscriptions.size)
      subscriptions.foreach(println)
      subscriptions should not be empty
    }
    "load all subscriptions" in {
      val subscriptions = storage.getAllSubscriptions.futureValue
      println(subscriptions.size)
      subscriptions.foreach(println)
      subscriptions should not be empty
    }
    "disable subscription" in {
      storage.disableSubscription(Uid(292685114), "ff86cfea5d609814733beb83d3c8e148a7a1a0df").futureValue

    }
  }
}
