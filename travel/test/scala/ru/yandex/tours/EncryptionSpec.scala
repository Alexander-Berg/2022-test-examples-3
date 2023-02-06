package ru.yandex.tours

import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.tours.util.Encryption

class EncryptionSpec extends WordSpecLike with Matchers {
  "Hmac sha256" should {
    "get signature of data" in {
      Encryption.sha256hmac("key", "The quick brown fox jumps over the lazy dog") shouldBe "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8"
      val offer = "{\"price\":75400,\"fuelCharge\":5200,\"finalPrice\":80600,\"transferPrice\":16000,\"checkInDate\":\"2015-02-28\",\"nights\":9,\"touristGroup\":{\"adults\":2,\"kidsAges\":[3,5],\"infants\":0},\"meal\":\"ai: все включено\",\"departureCity\":1360,\"resort\":444,\"country\":81,\"hotel\":9012731,\"room\":{\"code\":\"SUPERIOR GARDEN VIEW ROOM / DBL\",\"name\":\"улучшенный номер\"},\"operator\":5,\"bookingUrl\":\"http://ya.ru\",\"flights\":{\"to\":{\"flight_no\":\"U6 3015\",\"origincode\":\"DME\",\"departure\":\"2015-03-25T10:00:00+00:00\",\"destinationcode\":\"SSH\",\"arrival\":\"2015-03-25T13:45:00+00:00\",\"direct\":true,\"aircraft\":\"Airbus-321\",\"airline\":{\"id\":7,\"name\":\"Ural Airlines\"}},\"back\":{\"flight_no\":\"U6 3016\",\"origincode\":\"SSH\",\"departure\":\"2015-04-01T14:45:00+00:00\",\"destinationcode\":\"DME\",\"arrival\":\"2015-04-01T20:40:00+00:00\",\"direct\":true,\"aircraft\":\"Airbus-321\",\"airline\":{\"id\":7,\"name\":\"Ural Airlines\"}}}}"
      Encryption.sha256hmac("hb8qakzcbq3sxdm8rx", offer) shouldBe "61aab235868e3c1105525102b4800a5844ea6da89f2d3ddaea46b8e97032fc1c"
    }
  }

}
