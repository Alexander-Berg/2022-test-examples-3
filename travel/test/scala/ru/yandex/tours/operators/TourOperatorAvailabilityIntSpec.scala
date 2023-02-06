package ru.yandex.tours.operators

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.tagobjects.Retryable
import org.scalatest.{Ignore, Inspectors, Retries}
import ru.yandex.tours.model.TourOperator
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import ru.yandex.tours.util.http.NingHttpClient

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 01.06.15
 */
@Ignore
class TourOperatorAvailabilityIntSpec extends BaseSpec with TestData with ScalaFutures with IntegrationPatience with Inspectors with Retries {
  val operators = data.tourOperators
  val httpClient = new NingHttpClient(None)
  val client = new TourOperatorAvailability("csback01ht.vs.yandex.net", 14110, httpClient)

  val operator = operators.getAll.head
  val notExistOperator = TourOperator(999, Map.empty, "not exists", "not_exists", 60d)

  "TourOperatorAvailability" should {
    "mark operation as successful" in {
      client.markSuccess(operator).futureValue
    }
    "mark operation as failed" in {
      client.markFailure(operator).futureValue
    }
    "get operator availability" in {
      client.getAvailability(operator).futureValue
    }
    "get availability if operator do not exists" in {
      val availability = client.getAvailability(notExistOperator).futureValue
      availability.total shouldBe 0
      availability.successes shouldBe 0
      availability.failures shouldBe 0
    }
    "increase counters" taggedAs Retryable in {
      val old = client.getAvailability(operator).futureValue
      client.markSuccess(operator)
      client.markFailure(operator)
      val fresh = client.getAvailability(operator).futureValue
      fresh.successes should be > old.successes
      fresh.failures should be > old.failures
    }
    "get availability for several operators" in {
      val map = client.getAvailability(operators.getAll).futureValue
      forAll(operators.getAll) {
        operator => map should contain key operator
      }
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
