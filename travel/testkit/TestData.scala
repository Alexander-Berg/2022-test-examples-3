package ru.yandex.tours.testkit

import scala.io.Source

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 20.08.15
 */
trait TestData {
  lazy val data = TestData.instance

  def getTestData(name: String, clazz: Class[_]): String = {
    Source.fromInputStream(clazz.getResourceAsStream(name)).mkString
  }
}

object TestData {
  private val instance = new TestDataProvider()
}