package ru.yandex.tours.hotels

import ru.yandex.tours.testkit.{BaseSpec, TestData}

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 27.07.15
 */
class HotelRatingsSpec extends BaseSpec with TestData {

  "HotelRatings" should {
    "return relevance" in {
      val hotelsIndex = data.hotelsIndex
      val ratings = HotelRatings(hotelsIndex)

      for (hotel <- hotelsIndex.hotels) {
        ratings.getRelevance(hotel.id) shouldBe hotel.relevance
      }
    }
    "return rating" in {
      val hotelsIndex = data.hotelsIndex
      val ratings = HotelRatings(hotelsIndex)

      for (hotel <- hotelsIndex.hotels) {
        ratings.getRating(hotel.id) shouldBe hotel.rating
      }
    }

    "work fast" in {
      pending
      val hotelsIndex = data.hotelsIndex
      val ratings = HotelRatings(hotelsIndex)
      for (_ <- 0 until 40) {
        System.gc()
        val start = System.currentTimeMillis()
        for (_ <- 0 until 1000) {
          for (hotel <- hotelsIndex.hotels) {
            ratings.getRelevance(hotel.id)
          }
        }
        println(s"Done in ${System.currentTimeMillis() - start} ms.")
      }
    }
  }
}
