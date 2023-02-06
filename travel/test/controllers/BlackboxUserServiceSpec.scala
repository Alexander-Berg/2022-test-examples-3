package controllers

import akka.util.Timeout
import org.mockito.Mockito._
import org.mockito.Matchers._
import models.User
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import ru.yandex.tours.testkit.BaseSpec
import ru.yandex.tours.util.http.AsyncHttpClient
import spray.http.StatusCodes

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 01.03.16
 */
class BlackboxUserServiceSpec extends BaseSpec with ScalaFutures with IntegrationPatience {

  private def newService: (BlackboxUserService, AsyncHttpClient) = {
    val httpClient = mock[AsyncHttpClient]
    val service = new BlackboxUserService(httpClient, "http://blackbox.yandex-team.ru/blackbox")(global)
    (service, httpClient)
  }

  private implicit val timeout = Timeout(1.second)
  private val userIp = "127.0.0.1"

  "BlackboxUserService" should {
    "return empty user list on empty uid list" in {
      val (service, _) = newService
      service.getUsers(Nil, userIp).futureValue shouldBe Seq()
    }
    "get info about single user" in {
      val (service, client) = newService

      when(client.get(anyObject(), anyObject())(anyObject()))
        .thenReturn(Future.successful(StatusCodes.OK ->
          """<?xml version="1.0" encoding="UTF-8"?>
            |<doc>
            |<uid hosted="0">123</uid>
            |<login>some_login</login>
            |<have_password>1</have_password>
            |<have_hint>0</have_hint>
            |<karma confirmed="0">0</karma>
            |<karma_status>0</karma_status>
            |</doc>
          """.stripMargin))

      service.getUser(123L, userIp).futureValue shouldBe Some(User(123L, "some_login"))
    }
    "get info about many user" in {
      val (service, client) = newService

      when(client.get(anyObject(), anyObject())(anyObject()))
        .thenReturn(Future.successful(StatusCodes.OK ->
          """<?xml version="1.0" encoding="UTF-8"?>
            |<doc>
            |<user id="123">
            |<uid hosted="0">123</uid>
            |<login>some_login</login>
            |<have_password>1</have_password>
            |<have_hint>0</have_hint>
            |<karma confirmed="0">0</karma>
            |<karma_status>0</karma_status>
            |</user>
            |<user id="223">
            |<uid hosted="0">223</uid>
            |<login>another_login</login>
            |<have_password>1</have_password>
            |<have_hint>0</have_hint>
            |<karma confirmed="0">0</karma>
            |<karma_status>0</karma_status>
            |</user>
            |</doc>
          """.stripMargin))

      service.getUsers(Seq(123L, 223L), userIp).futureValue shouldBe Seq(
        User(123L, "some_login"),
        User(223L, "another_login")
      )
    }
  }
}
