package ru.yandex.tours.util.lang

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import ru.yandex.tours.testkit.BaseSpec

import scala.concurrent.duration.FiniteDuration

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 08.07.15
 */
class RichConfigSpec extends BaseSpec {
  "RichConfig.getFiniteDuration" should {
    "provide FiniteDuration from config" in {
      val config = ConfigFactory.parseString(
        """a1 = 3 seconds
          |a2=1h
          |a3=500 millis
          |a4=50 ms
          |a5=2s
          |a6=10m
        """.stripMargin)
      config.getFiniteDuration("a1") shouldBe FiniteDuration(3, TimeUnit.SECONDS)
      config.getFiniteDuration("a2") shouldBe FiniteDuration(1, TimeUnit.HOURS)
      config.getFiniteDuration("a3") shouldBe FiniteDuration(500, TimeUnit.MILLISECONDS)
      config.getFiniteDuration("a4") shouldBe FiniteDuration(50, TimeUnit.MILLISECONDS)
      config.getFiniteDuration("a5") shouldBe FiniteDuration(2, TimeUnit.SECONDS)
      config.getFiniteDuration("a6") shouldBe FiniteDuration(10, TimeUnit.MINUTES)
    }
    "throw exception if duration is infinite" in {
      val config = ConfigFactory.parseString(
        """a=inf
          |b=infinite
        """.stripMargin)
      an[RuntimeException] shouldBe thrownBy { config.getFiniteDuration("a") }
      an[RuntimeException] shouldBe thrownBy { config.getFiniteDuration("b") }
    }
  }
}
