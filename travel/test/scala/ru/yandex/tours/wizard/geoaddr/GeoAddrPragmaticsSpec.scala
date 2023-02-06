package ru.yandex.tours.wizard.geoaddr

import org.scalatest.Matchers._
import org.scalatest.OptionValues._
import org.scalatest.WordSpecLike
import ru.yandex.tours.geo.base.region
import ru.yandex.tours.geo.mapping.GeoMappingHolder
import ru.yandex.tours.query.SomeRegion
import ru.yandex.tours.util.text.StringNormalizer
import ru.yandex.tours.wizard.query.ParsedUserQuery.QueryPart

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 05.02.15
 */
class GeoAddrPragmaticsSpec extends WordSpecLike {

  val getMappingHolder = GeoMappingHolder(
    Map(
      (0, 995) -> "Таиланд",
      (0, 1056) -> "Египет",
      (0, 104168) -> "Крит" //not country actually
    ),
    Map(
      (0, 10419) -> "Ираклион",
      (0, 10493) -> "Хельсинки",
      (0, 213) -> "Москва",
      (0, 2) -> "Санкт-Петербург"
    ),
    Map(
      (0, 213) -> "Москва",
      (0, 2) -> "Санкт-Петербург"
    ),
    Map.empty
  )

  val toEgypt = GeoAddrObject(Seq(
    GeoVariant(Seq(1056), region.Types.Country, 0.927)
  ), 1056, "в египет", "египет", position = 1, length = 2)

  val toThailand = GeoAddrObject(Seq(
    GeoVariant(Seq(995), region.Types.Country, 0.933)
  ), 995, "в таиланд", "таиланд", position = 1, length = 2)

  val toCrete = GeoAddrObject(Seq(
    GeoVariant(Seq(104168), region.Types.FederalSubject, 0.969)
  ), 104168, "на крит", "крит", position = 1, length = 2)

  val toHeraklion = GeoAddrObject(Seq(
    GeoVariant(Seq(10419, 10424, 104165), region.Types.City, 0.969)
  ), 10419, "в ираклион", "ираклион", position = 1, length = 2)

  val fromMoscow = GeoAddrObject(Seq(
    GeoVariant(Seq(213), region.Types.City, 0.933)
  ), 213, "из москвы", "москва", position = 0, length = 2)

  val fromSpb = GeoAddrObject(Seq(
    GeoVariant(Seq(2), region.Types.City, 0.933)
  ), 2, "из санкт-петербурга", "санкт-петербург", position = 0, length = 3)

  val helsinki = GeoAddrObject(Seq(
    GeoVariant(Seq(10493), region.Types.City, 0.933)
  ), 10493, "хельсинки", "хельсинки", position = 0, length = 1)

  val unknown = GeoAddrObject(Seq(
    GeoVariant(Seq(9999), region.Types.City, 0.933)
  ), 0, "яндекс", "яндекс", position = 0, length = 1)

  val gap = new GeoAddrPragmatics(getMappingHolder)

  "GeoAddrPragmatics" should {
    "translate GeoAddrObject to pragmatic if region is known" in {
      gap.translateObject(toEgypt).value.regionId shouldBe 1056
      gap.translateObject(toThailand).value.regionId shouldBe 995
      gap.translateObject(toCrete).value.regionId shouldBe 104168
      gap.translateObject(toHeraklion).value.regionId shouldBe 10419
      gap.translateObject(fromMoscow).value.regionId shouldBe 213
      gap.translateObject(fromSpb).value.regionId shouldBe 2
      gap.translateObject(helsinki).value.regionId shouldBe 10493
    }
    "not translate GeoAddrObject if region is unknown" in {
      gap.translateObject(unknown) shouldBe None
    }
    "use best geo if present (>0)" in {
      val withBestGeo = unknown.copy(bestGeo = 9999)
      gap.translateObject(withBestGeo).value.regionId shouldBe 9999
    }
    "translate GeoAddrObject to SomeRegion" in {
      gap.translateObject(helsinki).value shouldBe SomeRegion(10493)
    }
    "translate GeoObject to QueryParts" in {
      val query = "туры из Санкт-Петербурга в Ираклион"
      val normalized = StringNormalizer.normalizeString(query)
      val geoAddr = GeoAddr(fromSpb, toHeraklion)
      val expected = Seq(
        QueryPart(normalized, 5, 25, SomeRegion(2)),
        QueryPart(normalized, 25, 36, SomeRegion(10419))
      )
      gap.translate(query, geoAddr) shouldBe expected
    }
    "ignore separators" in {
      val query = "туры из санкт - петербурга. в ираклион"
      val normalized = StringNormalizer.normalizeString(query)
      val geoAddr = GeoAddr(fromSpb, toHeraklion)
      val expected = Seq(
        QueryPart(normalized, 5, 25, SomeRegion(2)),
        QueryPart(normalized, 25, 36, SomeRegion(10419))
      )
      gap.translate(query, geoAddr) shouldBe expected
    }
  }
}
