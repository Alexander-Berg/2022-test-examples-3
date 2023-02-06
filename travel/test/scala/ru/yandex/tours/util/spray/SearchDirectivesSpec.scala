package ru.yandex.tours.util.spray

import org.scalatest.matchers.{MatchResult, Matcher}
import ru.yandex.tours.testkit.BaseSpec
import spray.http.HttpRequest
import spray.routing.{Directive1, HttpServiceBase}
import spray.testkit.ScalatestRouteTest

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 16.07.15
 */
class SearchDirectivesSpec extends BaseSpec with HttpServiceBase with SearchDirectives with ScalatestRouteTest {

  def matchDirective[T](directive: Directive1[T]): Matcher[HttpRequest] with Object {def apply(request: HttpRequest): MatchResult} = new Matcher[HttpRequest] {
    override def apply(request: HttpRequest): MatchResult = {
      val route = directive { _ =>
        complete("OK")
      }
      var matched = false
      request ~> route ~> check { matched = handled }
      MatchResult(
        matched,
        s"$request matched directive",
        s"$request did not matched directive"
      )
    }
  }

  "whereToGoRequest" should {
    "match requests" in {
      Get("/?thematic=beach") should matchDirective(whereToGoRequest)
      Get("/?thematic=city") should matchDirective(whereToGoRequest)
      Get("/?thematic=SURFING") should matchDirective(whereToGoRequest)
      Get("/?thematic=SURFING&budget=123") should matchDirective(whereToGoRequest)
      Get("/?thematic=SURFING&month=4") should matchDirective(whereToGoRequest)

      Get("/?thematic=SURFING&budget=") shouldNot matchDirective(whereToGoRequest)
      Get("/?thematic=") shouldNot matchDirective(whereToGoRequest)
      Get("/?thematic=abc") shouldNot matchDirective(whereToGoRequest)
      Get("/") shouldNot matchDirective(whereToGoRequest)
    }
  }

  "userIdentifiers" should {
    "match partial requests" in {
      Get("/") should matchDirective(userIdentifiers)
      Get("/?yuid=123") should matchDirective(userIdentifiers)
      Get("/?uid=234") should matchDirective(userIdentifiers)
      Get("/?login=yaya") should matchDirective(userIdentifiers)
      Get("/?yuid=&uid=234&login=yaya") should matchDirective(userIdentifiers)
      Get("/?yuid=&uid=&login=") should matchDirective(userIdentifiers)
    }
  }
}
