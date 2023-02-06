package ru.yandex.tours.tanker

import org.scalatest.LoneElement
import ru.yandex.tours.model.Languages
import ru.yandex.tours.model.hotels.Features
import ru.yandex.tours.model.hotels.Features.{IntFeature, IntFeatureValue}
import ru.yandex.tours.testkit.{BaseSpec, TestData}

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 17.08.16
 */
class TranslationsSpec extends BaseSpec with TestData with LoneElement {

  val tr = Translations.fromFile(root / "tanker.json")
  val lang = Languages.ru

  "Translation" should {
    "translate numeric features" in {
      val floors = Features.feature[IntFeature]("floors")
      tr.translate(IntFeatureValue(floors, Some(1)), lang).loneElement shouldBe "1 этаж"
      tr.translate(IntFeatureValue(floors, Some(2)), lang).loneElement shouldBe "2 этажа"
      tr.translate(IntFeatureValue(floors, Some(7)), lang).loneElement shouldBe "7 этажей"

      val buildYear = Features.feature[IntFeature]("build_year")
      tr.translate(IntFeatureValue(buildYear, Some(1990)), lang).loneElement shouldBe "построен в 1990 году"
      tr.translate(IntFeatureValue(buildYear, Some(2002)), lang).loneElement shouldBe "построен в 2002 году"
      tr.translate(IntFeatureValue(buildYear, Some(7095)), lang).loneElement shouldBe "построен в 7095 году"

      val numberRestaurant = Features.feature[IntFeature]("number_restaurant")
      tr.translate(IntFeatureValue(numberRestaurant, Some(0)), lang).loneElement shouldBe "0 ресторанов"
      tr.translate(IntFeatureValue(numberRestaurant, Some(1)), lang).loneElement shouldBe "Ресторан"
      tr.translate(IntFeatureValue(numberRestaurant, Some(2)), lang).loneElement shouldBe "2 ресторана"
      tr.translate(IntFeatureValue(numberRestaurant, Some(7)), lang).loneElement shouldBe "7 ресторанов"
    }
  }
}
