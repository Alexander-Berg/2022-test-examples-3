#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/google_hotel/csv/feed_parser.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>
#include <util/generic/maybe.h>
#include <util/stream/file.h>

#include <google/protobuf/text_format.h>

using namespace NMarket;

namespace {

    const auto fname =
            JoinFsPaths(ArcadiaSourceRoot(),
                        "market/idx/feeds/qparser/tests/data/google_hotel_feed.csv");
    auto fStream = TUnbufferedFileInput(fname);
    const auto input_csv = fStream.ReadAll();

} // namespace

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "offer_id": "3992e674046a10a81f4f068f1f35639e",
        "property_id": "88171580382",
        "property_name": "Hawaii Riviera Aqua Park Resort",
        "destination_name": "Египет",
        "url": "https://travel.yandex.ru/hotels/top/cairo/hawaii-riviera-aqua-park-resort",
        "image": "https://avatars.mds.yandex.net/get-altay/5308697/2a0000017b411ec066c85d6dc13a2af99d79/travel-marketing-square",
        "price": "4069",
        "star_rating": 3,
        "score": 8.6,
        "max_score": 10,
        "facilities": ["гостевая парковка", "бассейн"],
        "address": "test data city",
        "category": "hotel"
    },
    {
        "offer_id": "0d81389bcfc15bb5a08c39148dd5b0fc",
        "property_id": "88171832577",
        "property_name": "Вояж",
        "destination_name": "Малореченское",
        "url": "https://travel.yandex.ru/hotels/top/republic-of-crimea/voiazh",
        "image": "https://avatars.mds.yandex.net/get-altay/225456/2a0000016036a21b9b1e346ac7a32253f643/travel-marketing-square",
        "price": "3282",
        "star_rating": 4,
        "score": 8,
        "max_score": 15,
        "facilities": ["площадка для пикника", "оплата картой"],
        "address": "test data city",
        "category": "hostel"
    },
    {
        "offer_id": "0e3e89f60a10092cbfc49908995ce857",
        "property_id": "89514888151",
        "property_name": "Bellini Hotel",
        "destination_name": "Айя-Напа",
        "url": "https://travel.yandex.ru/hotels/top/ayia-napa/bellini-hotel",
        "image": "https://avatars.mds.yandex.net/get-altay/4388821/2a000001792f547881498842858a04338ef1/travel-marketing-square",
        "price": "5810.5",
        "star_rating": 5,
        "score": 9,
        "max_score": 15,
        "facilities": ["бассейн", "wi-fi"],
        "address": "data test place",
        "category": "hotel"
    }
]
)wrap");

TEST(GoogleHotelFeedParser, ExampleFeed) {

    const auto result = RunFeedParserWithTrace<NGoogleHotel::TCsvFeedParser>(
        input_csv,
        [](const TQueueItem &item) {
            NSc::TValue result;

            // Common data
            result["offer_id"] = item->DataCampOffer.identifiers().offer_id();
            result["url"] = item->GetOriginalSpecification().url().value();
            result["image"] = item->DataCampOffer.pictures().partner().original().source(0).url();

            const auto &price = item->RawPrice;
            if (price) {
                result["price"] = ToString(*price);
            }

            // Special data
            ASSERT_TRUE(item->DataCampOffer.content().type_specific_content().has_hotel());
            auto &hotel = item->DataCampOffer.content().type_specific_content().hotel();

            ASSERT_TRUE(hotel.has_property_id());
            result["property_id"] = hotel.property_id();
            ASSERT_TRUE(hotel.has_property_name());
            result["property_name"] = hotel.property_name();
            ASSERT_TRUE(hotel.has_destination_name());
            result["destination_name"] = hotel.destination_name();

            if (hotel.has_star_rating()) {
                result["star_rating"] = hotel.star_rating();
            }
            if (hotel.has_score()) {
                result["score"] = hotel.score();
            }
            if (hotel.has_max_score()) {
                result["max_score"] = hotel.max_score();
            }
            if (hotel.has_address()) {
                result["address"] = hotel.address();
            }
            if (hotel.has_category()) {
                result["category"] = hotel.category();
            }

            result["facilities"].SetArray();
            result["facilities"].AppendAll(hotel.facilities().begin(), hotel.facilities().end());

            return result;
        },
        GetDefaultGoogleHotelFeedInfo(),
        "offers-trace.log");
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);

    ASSERT_EQ(result.ArraySize(), expected.ArraySize());
    ASSERT_EQ(result, expected);
}