package ru.yandex.tours.testkit

import com.typesafe.config.ConfigFactory

object TestConfig {
  val config = ConfigFactory.load(
    ConfigFactory.parseResources("application.conf").withFallback(
      ConfigFactory.parseString(
        """
          |tours.s3.url="localhost/travel-indexer"
          |tours.s3.key=""
          |tours.s3.secretkey=""
          |tours.billing.url="localhost:80"
          |""".stripMargin)
    )
  )
}
