package ru.yandex.tours.supplier

import org.joda.time.DateTime
import org.scalatest.{Matchers, FlatSpec}
import ru.yandex.tours.client.{ShiftDate, RelativeSearchRequest}

/**
 * Created with IntelliJ IDEA.
 * User: Anton Ivanov <antonio@yandex-team.ru>
 * Date: 10.02.15
 * Time: 12:31
 */
class SerialSupplierTest extends FlatSpec with Matchers {
  val requestA = RelativeSearchRequest(0, 0, 0, ShiftDate(1), Seq(88), flexWhen = false, flexNights = false)
  val requestB = RelativeSearchRequest(0, 0, 0, ShiftDate(2), Seq(88), flexWhen = false, flexNights = false)
  "SerialSupplier" should "just work" in {
    val s = new SerialSupplier(Seq(requestA, requestB))
    s.nextRequest(DateTime.now()) should be (requestA)
    s.nextRequest(DateTime.now()) should be (requestB)
    s.nextRequest(DateTime.now()) should be (requestA)
  }
}
