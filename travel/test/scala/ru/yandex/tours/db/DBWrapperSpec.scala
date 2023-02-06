package ru.yandex.tours.db

import ru.yandex.tours.testkit.BaseSpec
import slick.driver.MySQLDriver.api._
import slick.lifted.ProvenShape

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 25.11.15
 */
class DBWrapperSpec extends BaseSpec {

  class TestTable(tag: Tag) extends Table[Int](tag, "test_table") {
    val id = column[Int]("id")
    override def * : ProvenShape[Int] = id
  }
  val testTable = TableQuery[TestTable]

  "DBWrapper" should {
    "detect readonly action" in {
      val db = mock[Database]
      val wrapper = new DBWrapper(db)

      wrapper.isReadOnly(testTable.result) shouldBe true
    }

    "detect write action" in {
      val db = mock[Database]
      val wrapper = new DBWrapper(db)

      wrapper.isReadOnly(sql"select 1".as[Int]) shouldBe false //fallback for interpolation
      wrapper.isReadOnly(sqlu"update 1") shouldBe false
      wrapper.isReadOnly(sqlu"update 1") shouldBe false
      wrapper.isReadOnly(testTable += 1) shouldBe false
      wrapper.isReadOnly(testTable.schema.create) shouldBe false
      wrapper.isReadOnly(DBIO.seq(testTable += 1, testTable.result)) shouldBe false
    }
  }
}
