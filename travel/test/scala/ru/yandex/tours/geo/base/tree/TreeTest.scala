package ru.yandex.tours.geo.base.tree

import org.scalatest.Matchers._
import org.scalatest.WordSpecLike
import ru.yandex.tours.geo.base.Region
import ru.yandex.tours.geo.base.region.{Tree, Types}
import ru.yandex.tours.model.{MapRectangle, LocalizedString}
import ru.yandex.tours.model.Languages._

class TreeTest extends WordSpecLike {
  val syns = Set.empty[String]
  val eurasia = Region(1, Types.Continent, 0, LocalizedString(ru -> "Евразия"), "", "", "", "", "", syns, 0, 0, 0, MapRectangle.empty)
  val russia = Region(2, Types.Country, eurasia.id, LocalizedString(ru -> "Россия"), "", "", "", "", "", syns, 0, 0, 0, MapRectangle.empty)
  val cis = Region(3, Types.Region, eurasia.id, LocalizedString(ru -> "СНГ"), "", "", "", "", "", syns, 0, 0, 0, MapRectangle.empty)
  val europe = Region(4, Types.Continent, eurasia.id, LocalizedString(ru -> "Европа"), "", "", "", "", "", syns, 0, 0, 0, MapRectangle.empty)
  val asia = Region(5, Types.Continent, eurasia.id, LocalizedString(ru -> "Азия"), "", "", "", "", "", syns, 0, 0, 0, MapRectangle.empty)

  val center = Region(6, Types.FederalDistrict, russia.id, LocalizedString(ru -> "Центральный"), "", "", "", "", "", syns, 0, 0, 0, MapRectangle.empty)
  val volga = Region(7, Types.FederalDistrict, russia.id, LocalizedString(ru -> "Поволжье"), "", "", "", "", "", syns, 0, 0, 0, MapRectangle.empty)
  val northwest = Region(8, Types.FederalDistrict, russia.id, LocalizedString(ru -> "Северо-Запад"), "", "", "", "", "", syns, 0, 0, 0, MapRectangle.empty)

  val tree = new Tree(Iterable(eurasia, russia, cis, europe, asia, center, volga, northwest))

  "Eurasia" must {
    "have 4 children" in {
      tree.children(eurasia) should have size 4
    }
    "have no parent" in {
      tree.parent(eurasia) shouldBe None
    }
    "have all regions as children" in {
      tree.allChildren(eurasia) shouldBe tree.regions.toSet
    }
    "find all regions as children" in {
      tree.findChildren(eurasia).head shouldBe eurasia
      tree.findChildren(eurasia).toList should contain theSameElementsAs tree.regions
    }
  }

  "Russia" must {
    "have 3 children" in {
      tree.children(russia) should have size 3
    }
    "have Eurasia as parent" in {
      tree.parent(russia) shouldBe Some(eurasia)
      tree.parent(russia.id) shouldBe Some(eurasia)
    }
    "have children" in {
      tree.children(russia) shouldBe Set(center, volga, northwest)
    }
  }

  "Center" must {
    "have Russia as parent" in {
      tree.parent(center) shouldBe Some(russia)
      tree.parent(center.id) shouldBe Some(russia)
    }
    "have Russia as parent of type Country" in {
      tree.parent(center, Types.Country) shouldBe Some(russia)
      tree.parent(center.id, Types.Country) shouldBe Some(russia)
    }
    "have Eurasia as parent of type Continent" in {
      tree.parent(center, Types.Continent) shouldBe Some(eurasia)
      tree.parent(center.id, Types.Continent) shouldBe Some(eurasia)
    }
    "have no children" in {
      tree.children(center) shouldBe empty
    }
  }
}
