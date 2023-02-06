package ru.yandex.tours.api

import com.typesafe.config.ConfigFactory
import org.scalatest.Suite
import spray.testkit.ScalatestRouteTest

/* @author berkut@yandex-team.ru */

trait RouteTestWithConfig extends ScalatestRouteTest {
  this: Suite =>
  override def testConfig = ConfigFactory.parseResources("spray.conf")
}

