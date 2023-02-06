package ru.yandex.tours.partners.ostrovokv3

import org.apache.commons.io.IOUtils
import ru.yandex.tours.geo.base.region.Tree
import ru.yandex.tours.hotels.MemoryHotelsIndex
import ru.yandex.tours.model.BaseModel.Pansion
import ru.yandex.tours.model.hotels.{PartnerInfo, Partners}
import ru.yandex.tours.model.util.proto
import ru.yandex.tours.parsing.PansionUnifier
import ru.yandex.tours.testkit.{BaseSpec, TestData}

class OstrovokV3ResponseParserSpec extends BaseSpec with TestData {
  private val hotel = data.hotelsIndex.hotels.next().copy(partnerIds = Iterable(PartnerInfo(Partners.ostrovokv3, "7848169", "", 0)))
  private val hotelsIndex = new MemoryHotelsIndex(Iterable(hotel), Tree.empty, data.hotelRatings)
  private val provider = data.hotelProviders.getAll.head

  "OstrovokV3 response parser" should {
    "parse response" in {
      val parser = new OstrovokV3ResponseParser(hotelsIndex, new PansionUnifier(Map(PansionUnifier.normalize("Завтрак включен") -> Pansion.BB)))
      val request = data.randomRequest
      val tryResult = parser.parse(request.extend(), response, provider)
      tryResult should be a 'Success
      val result = tryResult.get
      result.offers should have size 3
      val offer = result.offers.head
      offer.getAgentBookingUrl shouldBe "https://ostrovok.ru/go/rooms/rodeway_inn_stevenson/?cur=RUB&dates=02.12.2017-03.12.2017&from=rodeway_inn_stevenson.5475.RUB.debcb0f8.h-1ca7b6cc-4cec-4b0a-b2bb-2c38b5b11e29&guests=2&lang=ru&partner_data=eVg8QJzHplLaAXvsgo1pwlBnvSWucWVrNbI5dWDeFBOjDsUmpYmnzfEmZIQ%3D&partner_slug=yandex.affiliate.7877&request_id=3f9e586955385d8c407fd55ec28352ba&room=s-77a52448-6f7c-5292-b4ec-5447f0fe4b82&scroll=prices&session=144%3A0&utm_campaign=ru-ru&utm_distil=ru-ru&utm_medium=cpa-metasearch&utm_source=yandex_metasearch"
//      offer.getId shouldBe "p-58b059ae6c110c05a1669a5cf069db48"
      proto.toLocalDate(offer.getDate) shouldBe request.when
      offer.getFreeCancellation shouldBe true
      offer.getHotelId shouldBe hotel.id
      offer.getNights shouldBe request.nights

      offer.getLinkCount shouldBe 1
      val link = offer.getLink(0)
      link.getBillingId shouldBe provider.code
      link.getLink shouldBe "https://ostrovok.ru/go/rooms/rodeway_inn_stevenson/?cur=RUB&dates=02.12.2017-03.12.2017&from=rodeway_inn_stevenson.5475.RUB.debcb0f8.h-1ca7b6cc-4cec-4b0a-b2bb-2c38b5b11e29&guests=2&lang=ru&partner_data=eVg8QJzHplLaAXvsgo1pwlBnvSWucWVrNbI5dWDeFBOjDsUmpYmnzfEmZIQ%3D&partner_slug=yandex.affiliate.7877&request_id=3f9e586955385d8c407fd55ec28352ba&room=s-77a52448-6f7c-5292-b4ec-5447f0fe4b82&scroll=prices&session=144%3A0&utm_campaign=ru-ru&utm_distil=ru-ru&utm_medium=cpa-metasearch&utm_source=yandex_metasearch"

      link.getPartnerName shouldBe provider.name

      offer.getOriginalRoomCode shouldBe "Стандартный двухместный номер (Двуспальная кровать)"
      offer.getRoomType shouldBe "Стандартный двухместный номер (двуспальная кровать)"
      offer.getPansion shouldBe Pansion.BB
      offer.getRawPansion shouldBe "Завтрак включен"
      offer.getPrice shouldBe 5475
      offer.getSource.getPartnerId shouldBe Partners.ostrovokv3.id
      offer.getWithTransfer shouldBe false
      offer.getWithMedicalInsurance shouldBe false
      offer.getWithFlight shouldBe false
    }
  }

  private val response = IOUtils.toString(getClass.getResourceAsStream("/ostrovokv3_response.json"))
}
