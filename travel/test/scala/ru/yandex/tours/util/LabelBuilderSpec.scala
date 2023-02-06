package ru.yandex.tours.util

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.tours.model.BaseModel.Pansion
import ru.yandex.tours.model.search.SearchProducts.{Offer, Source}
import ru.yandex.tours.model.search.SearchResults.{ActualizedOffer, ResultInfo}
import ru.yandex.tours.model.utm.UtmMark
import ru.yandex.tours.personalization.UserIdentifiers

class LabelBuilderSpec extends WordSpec with Matchers {

  "Label Builder" should {
    "build label" in {
      val offer = Offer.newBuilder.setHotelId(76894)
        .setId("123")
        .setExternalId("123321")
        .setPansion(Pansion.AI)
        .setRoomType("")
        .setRawPansion("")
        .setOriginalRoomCode("")
        .setWithTransfer(false)
        .setWithMedicalInsurance(false)
        .setDate(0)
        .setNights(0)
        .setPrice(9944)
        .setSource(Source.newBuilder().setOperatorId(1).setPartnerId(5))
      val actualizedOffer = ActualizedOffer.newBuilder()
        .setCreated(System.currentTimeMillis())
        .setOffer(offer)
        .setResultInfo(ResultInfo.newBuilder().setIsFromLongCache(false))
        .build()
      val labelBuilder = new LabelBuilder("testtesttesttest")
      val query = labelBuilder.encrypt("query")
      val req_id = labelBuilder.encrypt("request-id")
      val label = labelBuilder.build(actualizedOffer, UtmMark("source", "medium", "campaign", "content", "term", query, req_id), UserIdentifiers(Some("1234567891011"), Some("1234356"), Some("test_login")))
      labelBuilder.decrypt(label) shouldBe "source\tmedium\tcampaign\tcontent\tterm\t1234356\t1234567891011\t76894\t1\tquery\trequest-id\t9944"
    }
  }

}
