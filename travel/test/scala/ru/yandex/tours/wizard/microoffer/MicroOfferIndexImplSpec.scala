package ru.yandex.tours.wizard.microoffer

import java.nio.ByteBuffer

import org.apache.commons.io.output.ByteArrayOutputStream
import ru.yandex.tours.index.WizardIndexing.MicroOffers
import ru.yandex.tours.model.wizard.MicroOffer.HotelMicroOffersProto
import ru.yandex.tours.testkit.BaseSpec

/**
  * Created by asoboll on 17.02.17.
  */
class MicroOfferIndexImplSpec extends BaseSpec with MicroOfferTestData {

  val item1 = hotelOffers(11, 10000, 11000, 12999, 8999)
  val item2 = hotelOffers(21, 6000)
  val item3 = hotelOffers(31, 8500, 11000)
  val item4 = hotelOffers(41, 30000)
  val item5 = hotelOffers(51, 2000, 100000)
  val item6 = hotelOffers(61, 4999)

  val data = Seq(item3, item2, item6, item4, item1, item5).sorted(MicroOfferIndex.idOrdering)
  val freshness = 12341234123L

  def buffered(dataInt: Seq[HotelMicroOffersProto]) = {
    val os = new ByteArrayOutputStream()
    val writer = MicroOfferWriter(os)
    writer.writeHeader(MicroOffers.formatVersion, freshness, dataInt.size)
    if (dataInt.nonEmpty) {
      writer.writeItem(dataInt.head)
      writer.writeItems(dataInt.tail)
    }
    writer.close()
    val bb = ByteBuffer.wrap(os.toByteArray)
    MicroOfferIndexImpl.fromBuffer(bb)
  }

  val bufferedIndex = buffered(data)

  val plainIndex = MicroOfferIndexImpl.fromIterator(freshness, data.iterator)

  "MicroOfferIndexImpl" should {
    "contain correct data" in {
      plainIndex.size shouldBe data.size
      bufferedIndex.size shouldBe data.size

      plainIndex.freshness shouldBe freshness
      bufferedIndex.freshness shouldBe freshness

      plainIndex.iterator.toSeq shouldEqual data
      bufferedIndex.iterator.toSeq shouldEqual data
    }

    "find items by hotelId" in {
      plainIndex.get(11) shouldBe Some(item1)
      bufferedIndex.get(11) shouldBe Some(item1)

      plainIndex.get(51) shouldBe Some(item5)
      bufferedIndex.get(51) shouldBe Some(item5)
    }

    "find nothing" in {
      plainIndex.get(1) shouldBe None
      bufferedIndex.get(1) shouldBe None

      plainIndex.get(30) shouldBe None
      bufferedIndex.get(30) shouldBe None

      plainIndex.get(1000) shouldBe None
      bufferedIndex.get(1000) shouldBe None
    }

    "work when empty" in {
      val index = MicroOfferIndexImpl.fromIterator(freshness, Iterator.empty)

      index.size shouldBe 0
      index.iterator.hasNext shouldBe false
      index.get(1) shouldBe None
    }

    "work buffered when empty" in {
      val index = buffered(Seq.empty)

      index.size shouldBe 0
      index.iterator.hasNext shouldBe false
      index.get(1) shouldBe None
    }
  }
}
