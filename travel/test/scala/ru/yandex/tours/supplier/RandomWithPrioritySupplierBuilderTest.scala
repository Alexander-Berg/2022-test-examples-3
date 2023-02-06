package ru.yandex.tours.supplier

import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}

/**
 * Created with IntelliJ IDEA.
 * User: Anton Ivanov <antonio@yandex-team.ru>
 * Date: 10.02.15
 * Time: 13:08
 */
class RandomWithPrioritySupplierBuilderTest extends FlatSpec with Matchers {
  val b = RandomWithPrioritySupplierBuilder
  "RandomWithPrioritySupplierBuilder" should "correct load all data from files" in {
   val s = b.parseFileForDayOfWeek(3)
    s.total should be (150)
    println(s"el=${s.elements}")
    s.elements.size should be (2)
    s.elements.head._1 should be (0)
    s.elements.last._1 should be (77)
    s.nextRequest(DateTime.parse("2012-01-01"))
  }

  it should "throw exception on load invalid date" in {
    an [NullPointerException] should be thrownBy b.parseFileForDayOfWeek(10)
  }
}
