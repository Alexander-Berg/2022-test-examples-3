package ru.yandex.tours.index.composite

import ru.yandex.tours.index.{WizardIndexItem, WizardIndexIterator}
import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 10.06.15
 */
class PairIndexIteratorSpec extends BaseSpec {

  def pair(left: Iterator[WizardIndexItem], right: Iterator[WizardIndexItem]) = {
    new PairIndexIterator(WizardIndexIterator.fromIterator(left), WizardIndexIterator.fromIterator(right))
  }

  "PairIndexIterator" should {
    "throw exception on `next` if exhausted" in {
      val i = new PairIndexIterator(WizardIndexIterator.empty, WizardIndexIterator.empty)
      i.hasNext shouldBe false
      an[RuntimeException] shouldBe thrownBy { i.next() }
    }
    "choose first item if equals" in {
      val i1 = WizardIndexItem(0, 2, 3, 123131, 7, 8, 19, 1)
      val i2 = WizardIndexItem(0, 2, 3, 123131, 7, 8, 10, 1)
      assume(i1.compareTo(i2) == 0)

      val i = pair(Iterator(i1), Iterator(i2))
      i.toList shouldBe List(i1)
    }
  }
}
