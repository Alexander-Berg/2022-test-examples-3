package ru.yandex.tours.db.dao

import com.typesafe.config.ConfigFactory
import ru.yandex.tours.db.DBWrapper
import ru.yandex.tours.db.dao.HotelsDao.OnlyPartner
import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.model.util.Paging
import ru.yandex.tours.testkit.BaseSpec
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 31.03.16
 */
class HotelsDaoIntSpec extends BaseSpec {

  val testDb = new DBWrapper(
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
  val dao = new HotelsDao(testDb)(ExecutionContext.global)

  "HotelsDao" should {
    "load hotels by partner" in {
      val res = dao.get(Paging(), OnlyPartner(Partners.rozahutor)).futureValue
      println(res.size)
    }
  }
}
