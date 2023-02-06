package ru.yandex.tours.supplier

import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.tours.client.{RelativeSearchRequest, ShiftDate}

/**
 * Created with IntelliJ IDEA.
 * User: Anton Ivanov <antonio@yandex-team.ru>
 * Date: 10.02.15
 * Time: 14:03
 */
class CompositeSupplierTest extends FlatSpec with Matchers {
  "CompositeSupplier" should "correct work" in {
    val expected = RelativeSearchRequest(213, 1056, 8, ShiftDate(14), Seq(88, 88), flexWhen = false, flexNights = false)
    val simpleSupplier = new RequestSupplier {
      override def nextRequest(current: DateTime): RelativeSearchRequest = expected
    }
    val supplier = new CompositeSupplier(Vector(simpleSupplier))
    supplier.nextRequest(DateTime.parse("2015-02-10")) shouldBe expected
  }
}
