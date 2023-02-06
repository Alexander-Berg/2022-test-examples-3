package ru.yandex.tours.util.ip

import org.scalatest.Matchers
import ru.yandex.tours.testkit.BaseSpec

import scala.util.Random

/**
  * Created by asoboll on 27.12.16.
  */
class RandomIpGeneratorSpec extends BaseSpec with Matchers {
  import RandomIpGenerator._

  val subnet1 = Subnet(0xb2400000, 0xfffe0000) // 178.64-65.*.*
  val subnet2 = Subnet(0xb2500000, 0xffff0000) // 178.80.*.*
  val generator = create(subnet1, subnet2)
  val testCount = 10

  "RandomIpGenerator ip" should {
    "generate single ip" in {
      generator.stream.iterator.take(testCount).foreach { ip4 =>
        //println(ip4.getHostAddress)
        ip4.getHostAddress should fullyMatch regex "178\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}".r
      }
    }

    "generate same ip for same seed" in {
      val x = Random.nextInt()
      Seq.fill(testCount)(x).map(generator.fromInt).distinct.size shouldBe 1
    }

    "generate different ip for different seed" in {
      val x = Random.nextInt()
      Seq.range(x, x + testCount).map(generator.fromInt).distinct.size shouldBe testCount
    }
  }
}
