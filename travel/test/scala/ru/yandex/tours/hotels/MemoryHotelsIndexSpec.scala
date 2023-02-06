package ru.yandex.tours.hotels

import ru.yandex.tours.geo.base.region.Tree
import ru.yandex.tours.model.hotels._
import ru.yandex.tours.model.{LocalizedString, MapRectangle}
import ru.yandex.tours.testkit.BaseSpec
import ru.yandex.tours.model.Languages._
import shapeless.HNil

/* @author berkut@yandex-team.ru */

class MemoryHotelsIndexSpec extends BaseSpec {

  private val address = Map(ru -> Address(ru, country = "Rosia", locality = "Moskva"))
  private val hotel1 = Hotel(1, 213, 1, 1, List(PartnerInfo(Partners(5), "hotel1", "", 1), PartnerInfo(Partners(0), "1", "", 2)), address, LocalizedString(ru -> "hotel"), Seq.empty, None, Star.stars(1), Iterable.empty, 0, 0, Iterable.empty)
  private val hotel2 = Hotel(2, 213, 1.5, 1.5, List(PartnerInfo(Partners(5), "hotel2", "", 3), PartnerInfo(Partners(0), "2", "", 4)), address, LocalizedString(ru -> "hotel"), Seq.empty, None, Star.stars(1), Iterable.empty, 0, 0, Iterable.empty)
  private val hotel3 = Hotel(3, 213, 2, 2, List(PartnerInfo(Partners(5), "hotel3", "", 5), PartnerInfo(Partners(0), "3", "", 6)), address, LocalizedString(ru -> "hotel"), Seq.empty, None, Star.stars(1), Iterable.empty, 0, 0, Iterable.empty)
  val hotels = List(hotel1, hotel2, hotel3)
  
  val index = new MemoryHotelsIndex(hotels, Tree.empty, HotelRatings.empty)
  "MemoryHotelsIndex" must {
    "return hotels by partner ids" in {
      index.getHotel(Partners(5), "hotel1").value shouldBe hotel1
      index.getHotel(Partners(5), "hotel2").value shouldBe hotel2
      index.getHotel(Partners(5), "hotel3").value shouldBe hotel3
    }

    "return hotel by our id" in {
      index.getHotelById(1).value shouldBe hotel1
      index.getHotelById(2).value shouldBe hotel2
      index.getHotelById(3).value shouldBe hotel3
    }
    "return hotels by id" in {
      index.getHotelsById(Seq(1, 2, 3)) shouldBe Map(
        1 -> hotel1,
        2 -> hotel2,
        3 -> hotel3
      )
    }

    "return index size" in {
      index.size shouldBe 3
    }

    "return hotels in rectangle" in {
      val rectangle = index.inRectangle(MapRectangle.byBoundaries(-1, -1, 1.6, 1.6)).toSet
      rectangle should have size 2
      rectangle.contains(hotel1) shouldBe true
      rectangle.contains(hotel2) shouldBe true
    }

    "check is hotel in rectangle" in {
      val rectangle = MapRectangle.byBoundaries(-1, -1, 1.6, 1.6)
      HotelsIndex.inRectangle(rectangle, hotels) shouldBe Seq(hotel1, hotel2)
    }
  }
}
