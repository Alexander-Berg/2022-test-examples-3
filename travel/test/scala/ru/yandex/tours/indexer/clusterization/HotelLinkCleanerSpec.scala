package ru.yandex.tours.indexer.clusterization

import ru.yandex.tours.clustering.Clustering.{Link, LinkWithConfidence}
import ru.yandex.tours.db.tables.Clusterization.LinkWithInfo
import ru.yandex.tours.db.tables.LinkType
import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.testkit.BaseSpec

class HotelLinkCleanerSpec extends BaseSpec {
  val hotel2partner = Map(
    1 -> Partners.ostrovok,
    2 -> Partners.ostrovok,
    3 -> Partners.ostrovok,
    4 -> Partners.booking,
    5 -> Partners.hotels101
  )

  def newCleaner: HotelLinkCleaner = new HotelLinkCleaner(
    Seq(
      LinkWithInfo(1, 5, 0.7d, LinkType.MERGE, 123, 0),
      LinkWithInfo(4, 5, 0.7d, LinkType.UNMERGE, 228, 121)
    ),
    hotel2partner
  )

  "Hotel link cleaner" should {

    "Not add deleted links" in {
      newCleaner.removeExcessLinksAndSetConfidence(Seq(LinkWithConfidence(5, 4, 0.7))) shouldBe 'empty
    }

    "Connect hotels of same partner with low confidence" in {
      val links = Seq(LinkWithConfidence(2, 1, 0.7), LinkWithConfidence(3, 2, 0.7))
      val result = newCleaner.removeExcessLinksAndSetConfidence(links)
      result shouldBe Seq(
        LinkWithConfidence(2, 1, 0.7),
        LinkWithConfidence(3, 2, 0.35)
      )
    }

    "Do not add excess links" in {
      val links = Seq(LinkWithConfidence(3, 1, 0.7), LinkWithConfidence(5, 3, 0.7))
      val result = newCleaner.removeExcessLinksAndSetConfidence(links)
      result shouldBe Seq(LinkWithConfidence(3, 1, 0.7))
    }
    "do not add banned links" in {
      newCleaner.removeExcessLinksAndSetConfidence(Seq(LinkWithConfidence(4, 5, 0.7))) shouldBe Seq.empty
      newCleaner.removeExcessLinksAndSetConfidence(Seq(LinkWithConfidence(5, 4, 0.7))) shouldBe Seq.empty
      newCleaner.removeExcessLinksAndSetConfidence(Seq(
        LinkWithConfidence(5, 4, 0.7)
      )) shouldBe Seq.empty
    }

    "Combine all 3 logics" in {
      val links = Seq(
        LinkWithConfidence(5, 4, 0.7),
        LinkWithConfidence(2, 1, 0.7),
        LinkWithConfidence(3, 2, 0.7),
        LinkWithConfidence(5, 2, 0.7),
        LinkWithConfidence(5, 3, 0.7)
      )
      val result = newCleaner.removeExcessLinksAndSetConfidence(links)
      result shouldBe Seq(
        LinkWithConfidence(2, 1, 0.7),
        LinkWithConfidence(3, 2, 0.35)
      )
    }
  }
}
