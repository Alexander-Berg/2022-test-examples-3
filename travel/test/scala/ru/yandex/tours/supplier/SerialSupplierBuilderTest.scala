package ru.yandex.tours.supplier

import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.tours.client.{ShiftDate, RelativeSearchRequest}

/**
 * Created with IntelliJ IDEA.
 * User: Anton Ivanov <antonio@yandex-team.ru>
 * Date: 10.02.15
 * Time: 12:46
 */
class SerialSupplierBuilderTest extends FlatSpec with Matchers {


  "SerialSupplierBuilder" should "correct load supplier by day of the week" in {
    val s = SerialSupplierBuilder
    (1 to 7).foreach(i => {
      val expected = RelativeSearchRequest(213, 1056, 6 + i, ShiftDate(14), Seq(88, 88), flexWhen = false, flexNights = false)
      s.parseFileForDayOfWeek(i).nextRequest(DateTime.now()) shouldBe expected
    })
  }
}
