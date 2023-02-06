package ru.yandex.tours.partners.booking

import org.joda.time.LocalDate
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.tours.geo.base.region.Tree
import ru.yandex.tours.hotels.MemoryHotelsIndex
import ru.yandex.tours.model.BaseModel.Pansion
import ru.yandex.tours.model.hotels.PartnerInfo
import ru.yandex.tours.model.search.SearchProducts.Offer
import ru.yandex.tours.model.search.{BaseRequest, HotelSearchRequest, OfferSearchRequest}
import ru.yandex.tours.model.util.proto
import ru.yandex.tours.parsing.PansionUnifier
import ru.yandex.tours.testkit.TestData

class BookingResponseParserSpec extends WordSpec with Matchers with TestData {
  val hotelProviders = data.hotelProviders
  private val hotel = data.hotelsIndex.hotels.next
  val oneHotel = hotel.copy(id = hotel.id + 1, partnerIds = Iterable(PartnerInfo(partner, "1101556", "http://booking.com", 1)))
  val otherHotel = hotel.copy(id = hotel.id + 2, partnerIds = Iterable(PartnerInfo(partner, "743569", "http://booking.com", 2)))
  val hotelsIndex = new MemoryHotelsIndex(Iterable(oneHotel, otherHotel), Tree.empty, data.hotelRatings)
  val provider = hotelProviders.getAll.head

  "Booking response parser" should {
    val parser = new BookingResponseParser(hotelsIndex, PansionUnifier.empty)

    def checkOffer(offer: Offer,
                   url: String,
                   id: String,
                   request: BaseRequest,
                   hotelId: Int,
                   roomName: String,
                   rawRoomName: String,
                   pansion: Pansion,
                   rawPansion: String,
                   price: Int) {

      offer.getAgentBookingUrl shouldBe url
      offer.getExternalId shouldBe id
      proto.toLocalDate(offer.getDate) shouldBe request.hotelRequest.when
      offer.getHotelId shouldBe hotelId
      offer.getNights shouldBe request.hotelRequest.nights

      offer.getLinkCount shouldBe 1
      val link = offer.getLink(0)
      link.getBillingId shouldBe provider.code
      link.getLink shouldBe url
      link.getPartnerName shouldBe provider.name

      offer.getOriginalRoomCode shouldBe rawRoomName
      offer.getRoomType shouldBe roomName
      offer.getPansion shouldBe pansion
      offer.getRawPansion shouldBe rawPansion
      offer.getPrice shouldBe price
      offer.getSource.getPartnerId shouldBe partner.id
      offer.getWithTransfer shouldBe false
      offer.getWithMedicalInsurance shouldBe false
      offer.getWithFlight shouldBe false
    }

    "parse hotel snippets" in {
      val searchRequest = HotelSearchRequest(213, 1056, 8, new LocalDate("2015-12-27"), Seq(88, 88))
      val trySnippets = parser.parseHotelSnippets(searchRequest.extend(), provider)(rawHotelsJson)
      trySnippets shouldBe 'Success
      val ChunkedSnippets(chunks, snippets) = trySnippets.get
      chunks shouldBe 1
      snippets should have size 2
      val snippet = snippets.head
      proto.toLocalDate(snippet.getDateMax) shouldBe searchRequest.when
      proto.toLocalDate(snippet.getDateMin) shouldBe searchRequest.when
      snippet.getHotelId shouldBe oneHotel.id
      snippet.getNightsMax shouldBe searchRequest.nights
      snippet.getNightsMin shouldBe searchRequest.nights
      snippet.getOfferCount shouldBe 4
      snippet.getPansionsCount shouldBe 1
      snippet.getPansions(0).getPansion shouldBe Pansion.RO
      snippet.getPansions(0).getPrice shouldBe 2500
      snippet.getPriceMin shouldBe 2500
      snippet.getPriceMax shouldBe 4000
      snippet.hasWithFlight shouldBe true
      snippet.getWithFlight shouldBe false
      snippet.getSourceCount shouldBe 1
      snippet.getSource(0).getOperatorId shouldBe provider.id
      snippet.hasSample shouldBe true
      val sample = snippet.getSample
      sample.hasFreeCancellation shouldBe false
      checkOffer(sample,
        "http://booking.com?aid=350687&checkin=2015-12-27&interval=8&lang=ru&selected_currency=RUB&group_adults=2&label=yandextravel",
        "110155603",
        searchRequest,
        oneHotel.id,
        "Economy triple room",
        "Economy Triple Room",
        Pansion.RO,
        "No meal is included in this room rate.",
        2500)
    }

    "parse chunked hotel snippets" in {
      val searchRequest = HotelSearchRequest(213, 1056, 8, new LocalDate("2015-12-27"), Seq(88, 88))
      val trySnippets = parser.parseHotelSnippets(searchRequest.extend(), provider)("{\"chunks\":3,\"result\":" + rawHotelsJson + "}")
      trySnippets shouldBe 'Success
      val ChunkedSnippets(chunks, snippets) = trySnippets.get
      chunks shouldBe 3
      snippets should have size 2
    }

    "not fail with bad hotel_id snippets" in {
      val searchRequest = HotelSearchRequest(213, 1056, 8, new LocalDate("2015-12-27"), Seq(88, 88))
      val trySnippets = parser.parseHotelSnippets(searchRequest.extend(), provider)(badHotelIdJson)
      trySnippets shouldBe 'Success
      val ChunkedSnippets(chunks, snippets) = trySnippets.get
      snippets should have size 0
    }

    "parse offers" in {
      val request = OfferSearchRequest(HotelSearchRequest(213, 1056, 15, new LocalDate("2015-12-21"), Seq(88, 88)), oneHotel.id)
      val tryOffers = parser.parseOffers(request.extend(), provider)(rawOfferJson)
      tryOffers shouldBe 'Success
      val offers = tryOffers.get
      offers should have size 1
      val offer = offers.head
      offer.getFreeCancellation shouldBe true
      checkOffer(offer,
        //"https://secure.booking.com/book.html?aid=350687&hotel_id=1101556&stage=1&checkin=2015-12-21&interval=15&lang=ru&label=yandextravel&nr_rooms_148267001_87636138_6_0=1",
        "http://booking.com?aid=350687&checkin=2015-12-21&interval=15&lang=ru&selected_currency=RUB&group_adults=2&label=yandextravel&show_room=148267001_87636138_6_0&no_rooms=1#RD148267001",
        "148267001_87636138_6_0",
        request,
        oneHotel.id,
        "Two-bedroom apartment",
        "Two-Bedroom Apartment",
        Pansion.HB,
        Pansion.HB.toString,
        33000
      )
    }
  }

  val rawOfferJson =
    """
      |[
      |
      |{
      |
      |    "hotel_text": "<![CDATA[\n<div class=\"policies\">\n  <div class=\"POLICY_CANCELLATION\">If cancelled or modified up to 7 days before date of arrival,  no fee will be charged. If cancelled or modified later or in case of no-show, 100 percent of the first night will be charged.\n</div>\n  <div class=\"POLICY_CHILDREN\">All children are welcome. There is no capacity for extra beds in the room.\n</div>\n  <div class=\"POLICY_HOTEL_INTERNET\">WiFi is available in all areas and is free of charge. </div>\n  <div class=\"POLICY_HOTEL_PARKING\">Free public parking is possible on site (reservation is not needed). </div>\n  <div class=\"POLICY_HOTEL_PETS\">Pets are not allowed. </div>\n  <div class=\"POLICY_PREPAY\">No deposit will be charged. </div>\n  <div class=\"POLICY_PREAUTHORIZE\">The hotel reserves the right to pre-authorise credit cards prior to arrival.</div>\n  <div class=\"POLICY_HOTEL_EXTRACHARGES\">18 % VAT is included.<items>18 % VAT is included.</items>\n  </div>\n</div>\n]]>",
      |    "arrival_date": "2015-10-22",
      |    "hotel_id": "1101556",
      |    "departure_date": "2015-10-25",
      |    "block":
      |
      |[
      |
      |{
      |
      |    "block_text": "<![CDATA[\n<div class=\"room_text\"></div>\n<div class=\"facilities\"></div>\n<div class=\"policies\">\n  <div class=\"POLICY_SUMMARY_INCLUDED\">18 % VAT</div>\n  <div class=\"POLICY_SUMMARY_EXCLUDED\"></div>\n  <div class=\"POLICY_TITLE\">General</div>\n</div>\n\n]]>",
      |    "photos":
      |
      |[
      |
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53008711.jpg",
      |    "photo_id": "53008711",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53008711.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53008711.jpg"
      |
      |},
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53008697.jpg",
      |    "photo_id": "53008697",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53008697.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53008697.jpg"
      |
      |},
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53008689.jpg",
      |    "photo_id": "53008689",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53008689.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53008689.jpg"
      |
      |},
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53008681.jpg",
      |    "photo_id": "53008681",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53008681.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53008681.jpg"
      |
      |},
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53008685.jpg",
      |    "photo_id": "53008685",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53008685.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53008685.jpg"
      |
      |},
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53010578.jpg",
      |    "photo_id": "53010578",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53010578.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53010578.jpg"
      |
      |},
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53008655.jpg",
      |    "photo_id": "53008655",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53008655.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53008655.jpg"
      |
      |},
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53009737.jpg",
      |    "photo_id": "53009737",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53009737.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53009737.jpg"
      |
      |},
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53008718.jpg",
      |    "photo_id": "53008718",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53008718.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53008718.jpg"
      |
      |},
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53008663.jpg",
      |    "photo_id": "53008663",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53008663.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53008663.jpg"
      |
      |},
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53008659.jpg",
      |    "photo_id": "53008659",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53008659.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53008659.jpg"
      |
      |},
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53008670.jpg",
      |    "photo_id": "53008670",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53008670.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53008670.jpg"
      |
      |},
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53008668.jpg",
      |    "photo_id": "53008668",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53008668.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53008668.jpg"
      |
      |},
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53009431.jpg",
      |    "photo_id": "53009431",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53009431.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53009431.jpg"
      |
      |},
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53009674.jpg",
      |    "photo_id": "53009674",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53009674.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53009674.jpg"
      |
      |},
      |{
      |
      |    "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53009681.jpg",
      |    "photo_id": "53009681",
      |    "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53009681.jpg",
      |    "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53009681.jpg"
      |
      |},
      |
      |    {
      |        "url_original": "http://aff.bstatic.com/images/hotel/max500/530/53009992.jpg",
      |        "photo_id": "53009992",
      |        "url_max300": "http://aff.bstatic.com/images/hotel/max300/530/53009992.jpg",
      |        "url_square60": "http://aff.bstatic.com/images/hotel/square60/530/53009992.jpg"
      |    }
      |
      |],
      |"max_occupancy": "6",
      |"full_board": 0,
      |"refundable_until": "2015-10-14 23:59:59 +0300",
      |"breakfast_included": "0",
      |"name": "Two-Bedroom Apartment",
      |"half_board": 1,
      |"min_price":
      |{
      |
      |    "currency": "RUB",
      |    "price": "33000.00",
      |    "other_currency":
      |
      |    {
      |        "currency": "USD",
      |        "price": "493.84"
      |    }
      |
      |},
      |"deposit_required": 0,
      |"block_id": "148267001_87636138_6_0",
      |"all_inclusive": 0,
      |"incremental_price":
      |[
      |
      |{
      |
      |    "currency": "RUB",
      |    "price": "33000.00",
      |    "other_currency":
      |
      |        {
      |            "currency": "USD",
      |            "price": "493.84"
      |        }
      |    }
      |
      |],
      |"rack_rate":
      |{
      |
      |    "currency": "RUB",
      |    "price": "0.00",
      |    "other_currency":
      |
      |                    {
      |                        "currency": "USD",
      |                        "price": "0.00"
      |                    }
      |                },
      |                "refundable": 1
      |            }
      |        ]
      |    }
      |
      |]
    """.stripMargin

  val rawHotelsJson =
    """[
      |{
      |
      |    "min_price_room_name": "Economy Triple Room",
      |    "max_total_price_room_id": "110155604",
      |    "min_price_mealplan": "No meal is included in this room rate.",
      |    "max_total_price": "4000.68",
      |    "min_price": "2500.26",
      |    "hotel_id": 1101556,
      |    "rack_rate":
      |    {
      |        "currency": "RUB",
      |        "price": "0.00"
      |    },
      |    "max_total_price_room_name": "Family Room",
      |    "min_price_room_id": "110155603",
      |    "min_total_price_room_name": "Economy Triple Room",
      |    "ranking": 2,
      |    "min_total_price_mealplan": "No meal is included in this room rate.",
      |    "currency_code": "RUB",
      |    "max_price": "4000.68",
      |    "min_total_price": "2500.26",
      |    "min_total_price_room_occupancy": "3",
      |    "max_price_room_id": "110155604",
      |    "max_total_price_mealplan": "No meal is included in this room rate.",
      |    "max_price_mealplan": "No meal is included in this room rate.",
      |    "max_price_room_name": "Family Room",
      |    "max_total_price_room_occupancy": "6",
      |    "available_rooms": 4,
      |    "min_total_price_room_id": "110155603"
      |
      |},
      |{
      |    "min_price_room_name": "Apartment - Priutskiy pereulok 3",
      |    "max_total_price_room_id": "74356901",
      |    "min_price_mealplan": "No meal is included in this room rate.",
      |    "max_total_price": "5800.00",
      |    "min_price": "5800.00",
      |    "hotel_id": "743569",
      |    "rack_rate":
      |
      |        {
      |            "currency": "RUB",
      |            "price": "0.00"
      |        },
      |        "max_total_price_room_name": "Apartment - Priutskiy pereulok 3",
      |        "min_price_room_id": "74356901",
      |        "min_total_price_room_name": "Apartment - Priutskiy pereulok 3",
      |        "ranking": 1,
      |        "min_total_price_mealplan": "No meal is included in this room rate.",
      |        "currency_code": "RUB",
      |        "min_total_price": "5800.00",
      |        "max_price": "5800.00",
      |        "max_price_room_id": "74356901",
      |        "min_total_price_room_occupancy": "4",
      |        "max_total_price_mealplan": "No meal is included in this room rate.",
      |        "max_price_room_name": "Apartment - Priutskiy pereulok 3",
      |        "max_price_mealplan": "No meal is included in this room rate.",
      |        "max_total_price_room_occupancy": "4",
      |        "available_rooms": 1,
      |        "min_total_price_room_id": "74356901"
      |    }
      |]
    """.stripMargin

  val badHotelIdJson =
    """
      |[
      |{
      |    "hotel_id": ["743569"]
      |    }
      |]
    """.stripMargin
}
