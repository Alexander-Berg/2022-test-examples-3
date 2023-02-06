package ru.yandex.tours.hotels

import java.io.ByteArrayOutputStream

import ru.yandex.tours.model.BaseModel.{LangToVal, Point, ProtoImage}
import ru.yandex.tours.model.MapRectangle
import ru.yandex.tours.model.filter.hotel.SearchTypeFilter
import ru.yandex.tours.model.hotels.HotelsHolder.{Feature, PartnerInfo, TravelHotel}
import ru.yandex.tours.model.hotels.{Hotel, Partners}
import ru.yandex.tours.model.search.SearchType
import ru.yandex.tours.testkit.{BaseSpec, TestData}

class YoctoHotelsIndexSpec extends BaseSpec with TestData {

  val tree = data.regionTree

  private def hotel(id: Int, partnerId: Int, localId: String, lon: Double, lat: Double, geoId: Int = 0,
                    rating: Double = 9, reviews: Int = 0) = {
    val point = Point.newBuilder().setLatitude(lat).setLongitude(lon)
    val image = ProtoImage.newBuilder().setGroup(1).setHost("").setName("").setProviderId(1).build()
    val name = LangToVal.newBuilder().setLang("en").setValue(s"Hotel #$id")
    val builder = TravelHotel.newBuilder()
      .setId(id)
      .setGeoId(geoId)
      .setPoint(point)
      .addImages(image)
      .setRating(rating)
      .setReviewsCount(reviews)
      .addName(name)
      .addFeatures(Feature.newBuilder().setName("private_beach").setValue("true"))

    builder.addPartnerInfoBuilder().setId(id).setPartner(partnerId).setPartnerId(localId).setPartnerUrl("")
    builder
      .build()
  }

  var index: YoctoHotelsIndex = _

  "Yocto hotels index" should {
    val hotels = Seq(
      hotel(1, 5, "123", 0.1, 0.1, 213, 9),
      hotel(2, 5, "321", -180, -90, 213, 10),
      hotel(3, 2, "123", 1, 0, 2, 8),
      hotel(4, 2, "asd", 0, 1, 2, 7).toBuilder
        .addPartnerInfo(PartnerInfo.newBuilder.setId(6).setPartner(2).setPartnerId("sdf").setPartnerUrl("")).build(),
      hotel(5, 2, "321", 0, 0, 2, 6).toBuilder.clearPoint().build()
    )

    "build index" in {
      val os = new ByteArrayOutputStream()
      YoctoHotelsIndex.build(hotels, tree, HotelRatings.empty, os)
      index = YoctoHotelsIndex.fromBytes(HotelRatings.empty, os.toByteArray)
      index.hotels.size shouldBe 4
    }

    "return index size" in {
      index.size shouldBe 4
    }

    "find hotels by id" in {
      index.getHotelById(1) shouldBe 'defined
      index.getHotelById(2) shouldBe 'defined
      index.getHotelById(3) shouldBe 'defined
      index.getHotelById(4) shouldBe 'defined
      index.getHotelById(5) shouldBe 'empty
      index.getHotelById(100) shouldBe 'empty
    }

    "find hotels by ids" in {
      index.getHotelsById(Seq(1, 2, 3)) shouldBe Map(
        1 -> Hotel(hotels.head),
        2 -> Hotel(hotels(1)),
        3 -> Hotel(hotels(2))
      ).mapValues(_.copy(rating = 0d, reviewsCount = 0))
    }

    "find hotel by children id" in {
      val master = index.getHotelById(6)
      master shouldBe 'defined
      master.get.id shouldBe 4
    }

    "find hotels by partner id" in {
      val yaHotel = index.getHotel(Partners.lt, "123")
      yaHotel shouldBe 'defined
      yaHotel.get.partnerIds.forall(t => t.partner == Partners.lt && t.id == "123") shouldBe true
      index.getHotel(Partners.lt, "321") shouldBe 'defined

      val bookingHotel = index.getHotel(Partners.booking, "123")
      bookingHotel shouldBe 'defined
      bookingHotel.get.partnerIds.forall(t => t.partner == Partners.booking && t.id == "123") shouldBe true
      index.getHotel(Partners.booking, "asd") shouldBe 'defined

      index.getHotel(Partners.yandex, "321") shouldBe 'empty
      index.getHotel(Partners.booking, "111") shouldBe 'empty
      index.getHotel(Partners.booking, "321") shouldBe 'empty
    }

    "find hotels in rectangle" in {
      index.inRectangle(MapRectangle.byBoundaries(-180, -90, 180, 90), 3) should have size 3
      index.inRectangle(MapRectangle.byBoundaries(-1, -90, -2, 90), 10) should have size 4
      index.inRectangle(MapRectangle.byBoundaries(-0.5, -0.5, 0.5, 0.5), 10) should have size 1
      index.inRectangle(MapRectangle.byBoundaries(-2, -2, -1, -1), 10) should have size 0
    }

    "find top hotels by region" in {
      val moscowTop = index.topInRegion(213, 3).toArray
      moscowTop should have size 2
      moscowTop.head.id shouldBe 2
      moscowTop(1).id shouldBe 1
      val spbTop = index.topInRegion(2, 2).toArray
      spbTop should have size 2
      spbTop.head.id shouldBe 3
      spbTop(1).id shouldBe 4
      val russiaTop = index.topInRegion(225, 3).toArray
      russiaTop should have size 3
      russiaTop(0).id shouldBe 2
      russiaTop(1).id shouldBe 1
      russiaTop(2).id shouldBe 3
      index.topInRegion(225, 100, SearchTypeFilter(SearchType.TOURS)) should have size 2
      index.topInRegion(225, 100, SearchTypeFilter(SearchType.ROOMS)) should have size 2
    }
  }
}
