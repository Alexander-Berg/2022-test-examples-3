package ru.yandex.tours.testkit.gen

import org.joda.time.LocalDate
import org.scalacheck.Gen

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 27.02.15
 */
object LocalDateGenerator {

  def inFutureGen = {
    val now = LocalDate.now()
    for (dateOffset <- Gen.chooseNum(0, 365 * 4))
    yield now.plusDays(dateOffset)
  }

  def anyDateGen = {
    val now = LocalDate.now()
    for (dateOffset <- Gen.chooseNum(-365 * 10, 365 * 20))
    yield now.plusDays(dateOffset)
  }
}
