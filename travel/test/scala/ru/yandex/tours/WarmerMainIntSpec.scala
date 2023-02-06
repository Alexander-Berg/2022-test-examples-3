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
class WarmerMainIntSpec extends AppTestKit with WordSpecLike {

  "WarmerMain" should {
    "start within 10 seconds" in {
      WarmerMain.start(interactive = false)
      awaitAssert({
        Source.fromURL("http://localhost:36440/ping").mkString shouldBe "0;OK"
      }, max = 10.seconds)
      WarmerMain.stop()
    }
  }
}
