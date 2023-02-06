package ru.yandex.tours.util.naming

import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 30.09.15
 */
class HotelNameIdSpec extends BaseSpec {
  "HotelNameId" should {
    "store hotel id and name index" in {
      val id = HotelNameId(10001, 4)

      id.hotelId shouldBe 10001
      id.nameIndex shouldBe 4
    }

    "work with any hotel id and name index" in {
      HotelNameId(Int.MaxValue, 4).hotelId shouldBe Int.MaxValue
      HotelNameId(Int.MaxValue, 4).nameIndex shouldBe 4
      HotelNameId(Int.MaxValue, 0).nameIndex shouldBe 0

      HotelNameId(500, Int.MaxValue).hotelId shouldBe 500
      HotelNameId(500, Int.MaxValue).nameIndex shouldBe Int.MaxValue
      HotelNameId(0, Int.MaxValue).nameIndex shouldBe Int.MaxValue
    }
  }
}
