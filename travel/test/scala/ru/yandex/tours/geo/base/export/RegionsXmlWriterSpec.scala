package ru.yandex.tours.geo.base.export

import ru.yandex.tours.geo.base.region.RegionBoundaries
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import ru.yandex.tours.util.IO
import ru.yandex.tours.util.Randoms._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 26.11.15
 */
class RegionsXmlWriterSpec extends BaseSpec with TestData {
  "RegionsXmlWriter" should {
    "write valid xml for all regions" in {
      data.regionTree.regions.sample(50).foreach { region =>
        val is = IO.printStream { w =>
          val writer = new RegionsXmlWriter(w)
          writer.write(region)
          writer.close()
        }
        XmlParser.getRegions(is, RegionBoundaries.empty) shouldBe Seq(region)
      }
    }
    "write all regions in valid xml" in {
      val regions = data.regionTree.regions.sample(50)
      val is = IO.printStream { w =>
        val writer = new RegionsXmlWriter(w)
        regions.foreach { region =>
          writer.write(region)
        }
        writer.close()
      }
      XmlParser.getRegions(is, RegionBoundaries.empty) shouldBe regions
    }
  }
}
