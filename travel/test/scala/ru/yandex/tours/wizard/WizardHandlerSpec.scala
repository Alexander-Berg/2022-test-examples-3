package ru.yandex.tours.wizard

import akka.testkit.TestActorRef
import com.codahale.metrics.MetricRegistry
import com.typesafe.config.{ConfigFactory, Config}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.Matchers._
import org.scalatest.WordSpecLike
import org.scalatest.mock.MockitoSugar
import ru.yandex.tours.wizard.experiment.ExperimentControl
import ru.yandex.tours.wizard.parser.WizardRequestParser
import ru.yandex.tours.wizard.search.{WizardThrottler, WizardToursSearcher}
import ru.yandex.tours.wizard.serialize.WizardResponseSerializer
import spray.http.{StatusCodes, Uri}
import spray.testkit.ScalatestRouteTest

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 02.02.15
 */
class WizardHandlerSpec extends WordSpecLike with ScalatestRouteTest with MockitoSugar {

  override def testConfig: Config = ConfigFactory.empty()

  private val metrics = new MetricRegistry
  private val parser = mock[WizardRequestParser]
  when(parser.parse(anyObject())).thenReturn(None)

  private val throttler = mock[WizardThrottler]
  when(throttler.shouldAnswer(anyString(), anyString())).thenReturn(true)

  private val experimentControl = mock[ExperimentControl]

  private val searcher = mock[WizardToursSearcher]
  private val serializer = mock[WizardResponseSerializer]

  private val actor = TestActorRef(new WizardHandler(metrics, parser, throttler, experimentControl, searcher, serializer))
  private val route = actor.underlyingActor.route
  private val sealRoute = actor.underlyingActor.sealRoute(route)

  private val uri = Uri("/wizard")

  "GET /wizard" should {
    "require user_request, geoaddr, tld and region_id params" in {
      Get(uri) ~> sealRoute ~> check {
        status shouldBe StatusCodes.NotFound
      }
      Get(uri.withQuery("geoaddr" -> "")) ~> sealRoute ~> check {
        status shouldBe StatusCodes.NotFound
      }
      Get(uri.withQuery("region_id" -> "")) ~> sealRoute ~> check {
        status shouldBe StatusCodes.NotFound
      }
      Get(uri.withQuery("user_request" -> "")) ~> sealRoute ~> check {
        status shouldBe StatusCodes.NotFound
      }
      Get(uri.withQuery("geoaddr" -> "", "region_id" -> "213", "user_request" -> "abba", "tld" -> "ru")) ~> route ~> check {
        status should not be StatusCodes.NotFound
      }
    }

    "require user_request or text parameter" in {
      Get(uri.withQuery("geoaddr" -> "", "region_id" -> "213", "user_request" -> "abba", "tld" -> "ru")) ~> route ~> check {
        status should not be StatusCodes.NotFound
      }
      Get(uri.withQuery("geoaddr" -> "", "region_id" -> "213", "text" -> "abba", "tld" -> "ru")) ~> route ~> check {
        status should not be StatusCodes.NotFound
      }
    }

    "respond with 2xx status" in {
      Get(uri.withQuery("geoaddr" -> "", "region_id" -> "213", "user_request" -> "", "tld" -> "ru")) ~> route ~> check {
        status.intValue should (be >= 200 and be <= 299)
      }
    }
  }
}
