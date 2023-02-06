package ru.yandex.tours.api

import org.json.JSONArray
import org.scalatest.WordSpecLike
import ru.yandex.tours.model.util.Paging
import ru.yandex.tours.testkit.BaseSpec

/* @author berkut@yandex-team.ru */

class JsonSerializationTest extends BaseSpec with JsonSerialization {
  "paging to json" should {
    "contain valid number of pages" in {
      def pager(size: Int, pageSize: Int = 20) = toJson(new JSONArray, Paging(0, pageSize), size).getJSONObject("pager")
      pager(size = 1).getInt("available_page_count") shouldBe 1
      pager(size = 21).getInt("available_page_count") shouldBe 2
      pager(size = 20).getInt("available_page_count") shouldBe 1

      pager(size = 20, pageSize = 18).getInt("available_page_count") shouldBe 2
      pager(size = 17, pageSize = 18).getInt("available_page_count") shouldBe 1
      pager(size = 18, pageSize = 18).getInt("available_page_count") shouldBe 1
    }
  }
}
