package ru.yandex.tours

import org.scalatest.Matchers._
import org.scalatest.{Ignore, WordSpecLike}
import ru.yandex.tours.testkit.AppTestKit

import scala.concurrent.duration._
import scala.io.Source

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 24.02.15
 */
@Ignore
class SearcherMainIntSpec extends AppTestKit with WordSpecLike {

  "SearcherMain" should {
    "start within 10 seconds" in {
      SearcherMain.start(interactive = false)
      awaitAssert({
        Source.fromURL("http://localhost:36420/ping").mkString shouldBe "0;OK"
      }, max = 10.seconds)
      SearcherMain.stop()
    }
  }
}
