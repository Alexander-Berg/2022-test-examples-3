package ru.yandex.tours.testkit

import org.junit.rules.TemporaryFolder
import org.scalatest.{Suite, BeforeAndAfterAll}

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 12.01.16
 */
trait TemporaryDirectory extends BeforeAndAfterAll { this: Suite =>
  val tempDir = new TemporaryFolder

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    tempDir.create()
  }

  override protected def afterAll(): Unit = {
    tempDir.delete()
    super.afterAll()
  }
}
