#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/google_flight/csv/feed_parser.h>

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
                        "market/idx/feeds/qparser/tests/data/google_flight_feed.csv");
    auto fStream = TUnbufferedFileInput(fname);
    const auto input_csv = fStream.ReadAll();

} // namespace

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "offer_id": "145fd22f62f491f326cf91005acb68ee",
        "destination_id": "SIP",
        "destination_name": "Симферополь",
        "origin_id": "LED",
        "origin_name": "Санкт-Петербург",
        "url": "http://vacationtravel.ru/avia/led/sip",
        "image": "https://photo.hotellook.com/static/cities/1200x1200/SIP.jpg",
        "price": "5600"
    },
    {
        "offer_id": "78c90cef9b726f1c5eb3abd6bc8ddcd9",
        "destination_id": "MOW",
        "destination_name": "Москва",
        "origin_name": "Сочи (Адлер)",
        "url": "https://www.kupibilet.ru/routes/AER/MOW",
        "image": "https://photo.hotellook.com/static/cities/1200x1200/SIP.jpg",
        "price": "5600"
    },
    {
        "offer_id": "4472602bffabb01e8eba6ed2675a1e1c",
        "destination_id": "MOW",
        "destination_name": "Москва",
        "url": "http://vacationtravel.by/avia/bqt/ayt",
        "image": "https://photo.hotellook.com/static/cities/1200x1200/SIP.jpg",
        "price": "6490"
    }
]
)wrap");

TEST(GoogleFlightFeedParser, ExampleFeed) {

    const auto result = RunFeedParserWithTrace<NGoogleFlight::TCsvFeedParser>(
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
            ASSERT_TRUE(item->DataCampOffer.content().type_specific_content().has_flight());
            auto &flight = item->DataCampOffer.content().type_specific_content().flight();

            ASSERT_TRUE(flight.has_destination_id());
            result["destination_id"] = flight.destination_id();

            ASSERT_TRUE(flight.has_destination_name());
            result["destination_name"] = flight.destination_name();

            if (flight.has_origin_id()) {
                result["origin_id"] = flight.origin_id();
            }
            if (flight.has_origin_name()) {
                result["origin_name"] = flight.origin_name();
            }

            return result;
        },
        GetDefaultGoogleFlightFeedInfo(),
        "offers-trace.log");
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);

    ASSERT_EQ(result.ArraySize(), expected.ArraySize());
    ASSERT_EQ(result, expected);
}