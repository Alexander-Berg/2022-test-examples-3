package ru.yandex.tours.util.spray

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import ru.yandex.tours.testkit.BaseSpec
import shapeless._
import spray.http.Uri
import spray.routing.Directives._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 07.09.15
 */
class spraySpec extends TestKit(ActorSystem(classOf[spraySpec].getSimpleName)) with BaseSpec with BeforeAndAfterAll with SearchDirectives {

  "extract" should {
    "extract value from query" in {
      val directive = parameter("a")
      extract(Uri.Query("a=7"), directive) shouldBe "7"
      extract(Uri.Query("a=8"), directive) shouldBe "8"
    }
    "extract hlist from query" in {
      val directive = parameters("a", "b")
      hextract(Uri.Query("a=7&b=9"), directive) shouldBe ("7" :: "9" :: HNil)
      hextract(Uri.Query("b=29&a=17"), directive) shouldBe ("17" :: "29" :: HNil)
    }
    "fail if query not matched" in {
      val directive = parameter("a")
      val exception = the[RuntimeException] thrownBy {
        extract(Uri.Query("b=8"), directive)
      }
      exception.getMessage should include ("b=8 not matched directive")
    }
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
  }
}
