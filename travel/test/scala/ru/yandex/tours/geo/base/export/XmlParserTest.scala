package ru.yandex.tours.geo.base.export

import org.scalatest.Matchers._
import org.scalatest.WordSpecLike
import ru.yandex.tours.geo.base.region.RegionBoundaries

class XmlParserTest extends WordSpecLike {

  private val regionsInTestExport = 34

  "Xml parser" must {
    "parse geobase" in {
      val is = getClass.getResourceAsStream("/geobase.xml")
      val tree = XmlParser.parse(is, RegionBoundaries.empty).get
//      val tree = parse.success.value
      tree should have size regionsInTestExport
    }
  }
}
