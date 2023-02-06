package ru.yandex.tours.db.geomapping

import ru.yandex.tours.db.geomapping.GeoMappingRecordProcessor._
import ru.yandex.tours.geo.mapping.GeoMappingShort
import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.testkit.BaseSpec

/**
  * Created by asoboll on 10.12.15.
  */
class GeoMappingRecordProcessorSpec extends BaseSpec{
  object geoRecord {
    var data: Seq[GeoMappingRecordExt] = Seq()

    def add(rec: (GeoMappingRecord, Long, Boolean)): Seq[GeoMappingRecordExt] = {
      data :+= GeoMappingRecordExt.tupled(rec); data
    }

    def add(command: Command, geoId:Int, partner: Int, partnerId: String, isManual: Boolean): Seq[GeoMappingRecordExt] =
      add((GeoMappingRecord(0, command, Partners(partner), geoId, partnerId, 0), geoRecord.data.size.toLong + 1, isManual))
  }

  "GeoMappingRecordProcessor" should {
    "detect automatic transactions" in {
      getMapping(Seq()) shouldBe List()
      getMapping(geoRecord.add(Add, 5, 3, "123", false)) shouldBe List(GeoMappingShort(Partners(3), 5, "123"))
    }

    "respect record order" in {
      getMapping(geoRecord.add(Drop, 5, 3, "123", false)) shouldBe List()
      getMapping(geoRecord.add(Drop, 5, 3, "123", true)) shouldBe List()
      getMapping(geoRecord.add(Add, 5, 3, "123", true)) shouldBe List(GeoMappingShort(Partners(3), 5, "123"))
    }

    "distinguish fullPartnerId" in {
      geoRecord.add(Add, 2, 1, "123", true)
      getMapping(geoRecord.add(Drop, 5, 3, "123", true)) shouldBe List(GeoMappingShort(Partners(1), 2, "123"))
      geoRecord.add(Add, 3, 1, "12", true)
      getMapping(geoRecord.add(Drop, 2, 1, "123", true)) shouldBe List(GeoMappingShort(Partners(1), 3, "12"))
    }

    "detect manual transactions" in {
      getMapping(geoRecord.add(Drop, 3, 1, "12", false)) shouldBe List(GeoMappingShort(Partners(1), 3, "12"))
      getMapping(geoRecord.add(Add, 3, 1, "15", false)) shouldBe List(GeoMappingShort(Partners(1), 3, "15"), GeoMappingShort(Partners(1), 3, "12"))
      getMapping(geoRecord.add(Drop, 3, 1, "12", true)) shouldBe List(GeoMappingShort(Partners(1), 3, "15"))
    }

  }
}