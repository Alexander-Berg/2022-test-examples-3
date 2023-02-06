package ru.yandex.tours.extdata

import ru.yandex.tours.extdata.UtilsSpec._
import ru.yandex.tours.operators.TourOperators
import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 12.05.15
 */
class UtilsSpec extends BaseSpec {

  "Utils" should {
    "create proxy" in {
      val target = new Target
      val (proxy, _) = Utils.createProxy(target, lazyLoad = false)
      proxy shouldNot be theSameInstanceAs target
      target.called shouldBe false
      proxy.call()
      target.called shouldBe true
    }
    "create lazy proxy" in {
      val (proxy, replacer) = Utils.createProxy[Target](sys.error("not defined"), lazyLoad = true)
      a[Exception] shouldBe thrownBy { proxy.call() }

      val target = new Target
      replacer(target)
      proxy.call()
      target.called shouldBe true
    }

    "create proxy for class with constructor args" in {
      val target = new TargetWithConstructor(1)
      val (proxy, _) = Utils.createProxy(target, lazyLoad = false)
      proxy shouldNot be theSameInstanceAs target
      target.called shouldBe false
      proxy.call()
      target.called shouldBe true
    }
  }
}
object UtilsSpec {

  class Target {
    var called = false
    def call(): Unit = called = true
  }

  class TargetWithConstructor(i: Int) {
    var called = false
    def call(): Unit = called = true
  }
}
