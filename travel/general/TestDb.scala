package ru.yandex.tours.tools

import com.typesafe.config.ConfigFactory
import ru.yandex.tours.db.DBWrapper
import slick.driver.MySQLDriver.api._

trait TestDb {
  lazy val testDb = new DBWrapper(
    Database.forConfig(
      "tours.mysql",
      ConfigFactory.parseString(
        """tours.mysql {
          |  driver = "com.mysql.jdbc.Driver"
          |  url = "jdbc:mysql://vertis-master-nonrt-testing.mysql.yandex.net:3306/tours"
          |  user = "tours"
          |  password = "hie2Eg1w"
          |  connectionTestQuery = "/* ping */ select 1"
          |  idleTimeout = 1m
          |  maxLifetime = 10m
          |  connectionTimeout = 10s
          |  properties = {
          |    connectTimeout=3000
          |    socketTimeout=5000
          |    rewriteBatchedStatements=true
          |    useUnicode=true
          |    characterEncoding=utf8
          |  }
          |}
        """.stripMargin)
    )
  )
  lazy val prodDb = new DBWrapper(
    Database.forConfig(
      "tours.mysql",
      ConfigFactory.parseString(
        """tours.mysql {
          |  driver = "com.mysql.jdbc.Driver"
          |  url = "jdbc:mysql://localhost:3307/tours"
          |  user = "tours"
          |  password = "]JsFTlv9j_SvxUaE$G(q"
          |  connectionTestQuery = "/* ping */ select 1"
          |  idleTimeout = 1m
          |  maxLifetime = 10m
          |  connectionTimeout = 10s
          |  properties = {
          |    connectTimeout=3000
          |    socketTimeout=5000
          |    rewriteBatchedStatements=true
          |    useUnicode=true
          |    characterEncoding=utf8
          |  }
          |}
        """.stripMargin)
    )
  )
}
