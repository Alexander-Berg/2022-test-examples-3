package ru.yandex.tours.wizard.geoaddr

import org.scalatest.Matchers._
import org.scalatest.WordSpecLike
import ru.yandex.tours.geo.base.region

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 30.01.15
 */
class GeoAddrParserSpec extends WordSpecLike {

  val json1 =
    """[{
      |  "geo": "в египет",
      |  "data": {"Variants": [{
      |    "Country": "египет",
      |    "CountryIDs": [1056],
      |    "Weight": 0.927
      |  }]},
      |  "pos": "1",
      |  "length": "2",
      |  "nongeoquery": "туры на майские праздники",
      |  "weight": "0.926831",
      |  "normalizedtext": "египет",
      |  "type": "City"
      |}]
    """.stripMargin

  val geo1 = GeoAddr(
    GeoAddrObject(Seq(
      GeoVariant(Seq(1056), region.Types.Country, 0.927)
    ), 0, "в египет", "египет", position = 1, length = 2)
  )

  val json2 =
  """[{
    |  "geo": "в таиланд",
    |  "data": {"Variants": [{
    |    "Country": "таиланд",
    |    "CountryIDs": [995],
    |    "Weight": 0.933
    |  }]},
    |  "pos": "1",
    |  "length": "2",
    |  "nongeoquery": "туры",
    |  "weight": "0.932762",
    |  "normalizedtext": "таиланд",
    |  "type": "City"
    |}]
  """.stripMargin

  val geo2 = GeoAddr(
    GeoAddrObject(Seq(
      GeoVariant(Seq(995), region.Types.Country, 0.933)
    ), 0, "в таиланд", "таиланд", position = 1, length = 2)
  )

  val json3 =
  """[{
    |  "geo": "петербурга",
    |  "cityidsstr": "2",
    |  "data": {"Variants": [{
    |    "CityIDs": [2],
    |    "City": "санкт-петербург",
    |    "Weight": 0.742
    |  }]},
    |  "pos": "0",
    |  "length": "1",
    |  "nongeoquery": "туры",
    |  "weight": "0.742095",
    |  "normalizedtext": "санкт-петербург",
    |  "type": "City"
    |}]
  """.stripMargin

  val geo3 = GeoAddr(
    GeoAddrObject(Seq(
      GeoVariant(Seq(2), region.Types.City, 0.742)
    ), 0, "петербурга", "санкт-петербург", position = 0, length = 1)
  )

  val json4 =
  """[{
    |  "geo": "в крыму",
    |  "cityidsstr": "977",
    |  "data": {"Variants": [{
    |    "HasLocative": true,
    |    "District1": "крым",
    |    "Weight": 0.952,
    |    "District1IDs": [977]
    |  }]},
    |  "pos": "1",
    |  "length": "2",
    |  "nongeoquery": "отдых",
    |  "weight": "0.952233",
    |  "normalizedtext": "крым",
    |  "type": "City"
    |}]
  """.stripMargin

  val geo4 = GeoAddr(
    GeoAddrObject(Seq(
      GeoVariant(Seq(977), region.Types.Other, 0.952)
    ), 0, "в крыму", "крым", position = 1, length = 2)
  )

  val json5 =
  """[{
    |    "bestgeo": "10419",
    |    "bestinheritedgeo": "10419",
    |    "cityidsstr": "10419,10424,104165",
    |    "data": {
    |        "BestGeo": 10419,
    |        "BestInheritedGeo": 10419,
    |        "Variants": [
    |            {
    |                "City": "ираклион",
    |                "CityIDs": [
    |                    10419,
    |                    10424,
    |                    104165
    |                ],
    |                "InheritedIDs": [
    |                    10424,
    |                    104165
    |                ],
    |                "Weight": 0.972
    |            }
    |        ]
    |    },
    |    "geo": "в ираклион",
    |    "length": "2",
    |    "nongeoquery": "туры",
    |    "normalizedtext": "ираклион",
    |    "pos": "1",
    |    "type": "City",
    |    "weight": "0.971821"
    |}]
  """.stripMargin

  val geo5 = GeoAddr(
    GeoAddrObject(Seq(
      GeoVariant(Seq(10419, 10424, 104165), region.Types.City, 0.972)
    ), 10419, "в ираклион", "ираклион", position = 1, length = 2)
  )

  val base64 =
  """W3siZ2VvIjoi0LIg0YXQtdC70YzRgdC40L3QutC4Iiwibm9ybWFsaXplZHRleHQiOiLRhdC10LvRjNGB0LjQvdC60LgiLCJkYXRhIjp7IlZhcmlhbnR
    |zIjpbeyJIYXNMb2NhdGl2ZSI6dHJ1ZSwiQ2l0eUlEcyI6WzEwNDkzXSwiQ2l0eSI6ItGF0LXQu9GM0YHQuNC90LrQuCIsIldlaWdodCI6MC45OX1dfS
    |wibGVuZ3RoIjoiMiIsIm5vbmdlb3F1ZXJ5Ijoi0YLRg9GAINC90LAg0L/QvtC10LfQtNC1Iiwid2VpZ2h0IjoiMC45OTA0MTgiLCJ0eXBlIjoiQ2l0e
    |SIsInBvcyI6IjEifSx7ImdlbyI6ItC40Lcg0YHQsNC90LrRgiDQv9C10YLQtdGA0LHRg9GA0LPQsCIsIm5vcm1hbGl6ZWR0ZXh0Ijoi0YHQsNC90LrR
    |gi3Qv9C10YLQtdGA0LHRg9GA0LMiLCJkYXRhIjp7IlZhcmlhbnRzIjpbeyJDaXR5SURzIjpbMl0sIkNpdHkiOiLRgdCw0L3QutGCLdC/0LXRgtC10YD
    |QsdGD0YDQsyIsIldlaWdodCI6MC45ODl9XX0sImxlbmd0aCI6IjMiLCJub25nZW9xdWVyeSI6bnVsbCwid2VpZ2h0IjoiMC45ODg5NyIsInR5cGUiOi
    |JDaXR5IiwicG9zIjoiNSJ9XQ==""".stripMargin.replace("\n", "")

  val geo64 = GeoAddr(
    GeoAddrObject(Seq(GeoVariant(Seq(10493), region.Types.City, 0.99)), 0, "в хельсинки", "хельсинки", position = 1, length = 2),
    GeoAddrObject(Seq(GeoVariant(Seq(2), region.Types.City, 0.989)), 0, "из санкт петербурга", "санкт-петербург", position = 5, length = 3)
  )

  val streetJson =
    """[{
      |  "data": {"Variants": [{
      |    "Quarter": "barın",
      |    "Weight": 0.777,
      |    "CommonIDs": [103729]
      |  }]},
      |  "pos": "1",
      |  "length": "2",
      |  "nongeoquery": "туры",
      |  "weight": "0.777078",
      |  "normalizedtext": "barın",
      |  "type": "Street",
      |  "addr": "в bar"
      |}]""".stripMargin

  val streetGeo = GeoAddr(
    GeoAddrObject(Seq(GeoVariant(List(103729), region.Types.Other, 0.777)), 0, "в bar", "barın", 1, 2)
  )


  "GeoAddrParser" should {
    "parse GeoAddr from json #1" in { GeoAddrParser.fromJson(json1) shouldBe geo1 }
    "parse GeoAddr from json #2" in { GeoAddrParser.fromJson(json2) shouldBe geo2 }
    "parse GeoAddr from json #3" in { GeoAddrParser.fromJson(json3) shouldBe geo3 }
    "parse GeoAddr from json #4" in { GeoAddrParser.fromJson(json4) shouldBe geo4 }
    "parse GeoAddr from json #5" in { GeoAddrParser.fromJson(json5) shouldBe geo5 }

    "parse GeoAddr from base64" in {
      GeoAddrParser.fromBase64(base64) shouldBe geo64
    }

    "parse GeoAddr with street" in {
      GeoAddrParser.fromJson(streetJson) shouldBe streetGeo
    }

    "parse empty base64" in {
      GeoAddrParser.fromBase64("") shouldBe GeoAddr.empty
    }
  }
}
