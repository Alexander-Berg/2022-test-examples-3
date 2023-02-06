package ru.yandex.tours.direction

import java.io.ByteArrayInputStream
import java.time.Month

import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.input.ClosedInputStream
import org.apache.commons.io.output.ByteArrayOutputStream
import ru.yandex.tours.extdata.DataTypes
import ru.yandex.tours.geo.base.Region
import ru.yandex.tours.model.direction.{ThematicInfo, Thematics}
import ru.yandex.tours.model.image.ImageProviders
import ru.yandex.tours.model.{AllYear, Image, MonthInterval, UnknownSeasonality}
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import shapeless._

import scala.util.Random

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 11.06.15
 */
class DirectionsSpec extends BaseSpec with TestData {

  val moscow = data.regionTree.region(213).value
  val egypt = data.regionTree.region(1056).value
  val turkey = data.regionTree.region(983).value
  val china = data.regionTree.region(134).value
  val cuba = data.regionTree.region(10017).value
  val kipr = data.regionTree.region(20574).value
  val alaniya = data.regionTree.region(11502).value
  val kemer = data.regionTree.region(21091).value
  val crime = data.regionTree.region(977).value

  val regions = Vector(moscow, egypt, turkey, china, cuba, kipr, alaniya, kemer, crime)
  val countries = Vector(egypt, turkey, china, cuba, kipr)

  def toDirection(region: Region) = Direction(region, Seq.empty, Seq.empty, Random.nextBoolean, noVisa = false, Seq.empty, Seq.empty)

  val rawDirections = regions.map(toDirection)
  val directions = new Directions(rawDirections)

  "Directions" should {
    "contain all directions" in {
      directions.all shouldBe rawDirections
    }
    "contain directions as map" in {
      directions.asMap shouldBe rawDirections.map(d => d.region.id -> d).toMap
    }
    "contain promoted directions" in {
      directions.promoted shouldBe rawDirections.filter(_.isPromoted)
    }

    "parse tsv and keep original order of images" in {
      val image1 = Image("a", 1, ImageProviders.lt, "a", None, size = None, pHash = None, nnFeatures = None)
      val image2 = Image("b", 2, ImageProviders.lt, "b", None, size = None, pHash = None, nnFeatures = None)

      val encoded = {
        val os = new ByteArrayOutputStream()
        image1.toProto.writeDelimitedTo(os)
        image2.toProto.writeDelimitedTo(os)
        Base64.encodeBase64String(os.toByteArray)
      }
      val region = egypt
      val priority = 13
      val data = s"${region.id}\t$encoded\t\t$priority\ttrue\tBeach:3:all_year:;Surfing:4:4-5:5-5"
      val directions = Directions.parse(new ByteArrayInputStream(data.getBytes), this.data.regionTree :: this.data.geoMapping :: this.data.skiResorts :: HNil).all
      directions should have size 1
      directions.head.region shouldBe region
      directions.head.isPromoted shouldBe true
      directions.head.images shouldBe Seq(image1, image2)
      directions.head.squareImages shouldBe empty
      directions.head.noVisa shouldBe true
      directions.head.thematics shouldBe Seq(
        ThematicInfo(Thematics.Beach, 3d, AllYear, UnknownSeasonality),
        ThematicInfo(Thematics.Surfing, 4d, MonthInterval(Month.APRIL, Month.MAY), MonthInterval(Month.MAY, Month.MAY))
      )
    }

    "parse from real data" in {
      val directions = Directions.fromFile(root / "directions",
        data.regionTree :: data.geoMapping :: data.skiResorts :: HNil)
      directions.all should not be empty
      directions.promoted should not be empty
      directions.asMap should not be empty
    }
    "throw Error if nothing parsed" in {
      a[Error] shouldBe thrownBy {
        Directions.parse(new ClosedInputStream, data.regionTree :: data.geoMapping :: data.skiResorts :: HNil)
      }
    }
    "have correct data type" in {
      Directions.dataType shouldBe DataTypes.directions
    }
    "depend on region Tree" in {
      Directions.dependsOn should contain (DataTypes.regions)
    }
  }
}
