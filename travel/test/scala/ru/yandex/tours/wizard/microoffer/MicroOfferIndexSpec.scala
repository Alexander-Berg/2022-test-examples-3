package ru.yandex.tours.wizard.microoffer

import ru.yandex.tours.testkit.BaseSpec

import scala.collection.JavaConverters._

/**
  * Created by asoboll on 17.02.17.
  */
class MicroOfferIndexSpec extends BaseSpec with MicroOfferTestData {
  import MicroOfferIndex._

  val hotel1 = 1357
  val hotel2 = 2468
  val hotel3 = 5000
  val item1 = hotelOffers(hotel1, 10000, 11000, 12999, 8999)
  val item2 = hotelOffers(hotel1, 6000)
  val item3 = hotelOffers(hotel1, 8500, 11000)
  val item4 = hotelOffers(hotel2, 30000)
  val item5 = hotelOffers(hotel3, 2000, 100000)
  val item6 = hotelOffers(hotel3, 4999)

  "MicroOfferIndex" should {
    "sort offers by price" in {
      item1.getOffersList.asScala.map(_.getPrice) shouldEqual List(8999, 10000, 11000, 12999)
    }
    "sort hotels by price" in {
      Seq(item1, item2, item3).sorted(priceOrdering) shouldEqual Seq(item2, item3, item1)
    }
    "sort hotels by offer count" in {
      Seq(item1, item2, item3).sorted(countOrdering) shouldEqual Seq(item1, item3, item2)
    }
    "sort hotels by default order" in {
      Seq(item1, item2, item3).sorted(defaultOrdering) shouldEqual Seq(item3, item1, item2)
    }
    "sort hotels by id" in {
      Seq(item4, item5, item3).sorted(idOrdering) shouldEqual Seq(item3, item4, item5)
    }

    "choose distinct hotels" in {
      val distinct = distinctHotels(Seq(item1, item2, item3, item4, item5, item6)).toSeq
      distinct shouldEqual Seq(item3, item4, item5)
    }
  }
}
