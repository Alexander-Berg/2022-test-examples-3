package ru.yandex.tours.geo

import org.scalatest.LoneElement
import ru.yandex.tours.avia.AviaAirportRecommendations
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import shapeless.HNil

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 16.09.15
 */
class AviaAirportRecommendationsSpec extends BaseSpec with TestData with LoneElement {

  val recommendations = data.airportRecommendations

  "Avia airport recommendations" should {
    "parse tsv file" in {
      val recommendations = AviaAirportRecommendations.fromFile(
        root / "airport_recommendations.tsv",
        data.regionTree :: data.aviaCities :: HNil
      )
      println(recommendations.size)
      println(recommendations.recommend(213))
      println(recommendations.recommendMany(2, 213))
    }

    "recommend airport" in {
      recommendations.recommend(21536).head.city.geoId shouldBe Some(21537)
    }
    "not recommend airports if departure close to destination" in {
      recommendations.recommend(from = 2, geoId = 10174) shouldBe None
      recommendations.recommend(from = 213, geoId = 21624) shouldBe None
    }
  }
}
