#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/google_travel/csv/feed_parser.h>

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
                        "market/idx/feeds/qparser/tests/data/google_travel_feed.csv");
    auto fStream = TUnbufferedFileInput(fname);
    const auto input_csv = fStream.ReadAll();

} // namespace

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "offer_id": "adca10a34a0bc9044ff996ca3b617eba",
        "destination_id": "1412651",
        "destination_name": "Волгоград",
        "origin_id": "1447874",
        "origin_name": "Москва",
        "title": "Автобус Москва Волгоград",
        "url": "https://bus.tutu.ru/bilety_na_avtobus/Moskva/Volgograd/?date=tomorrow&from=1447874&to=1412651&amount=1",
        "image": "https://bus.tutu.ru/csv_target_schedule.png",
        "price": "1703"
    },
    {
        "offer_id": "345db0b1248d342caea9df030096387f",
        "destination_id": "1457861",
        "destination_name": "Одесса",
        "origin_name": "Москва",
        "title": "Автобус Москва Одесса",
        "url": "https://bus.tutu.ru/bilety_na_avtobus/Moskva/Odessa/?date=tomorrow&from=1447874&to=1457861&amount=1",
        "image": "https://bus.tutu.ru/csv_target_schedule.png",
        "price": "4540"
    },
    {
        "offer_id": "a46c85b8921d09d2c85cd2129caee93b",
        "destination_id": "570",
        "destination_name": "Карелия",
        "title": "Все водопады Карелии (Зима)",
        "url": "https://kareliagid.ru/tours/in-karelia/vse-vodopady-karelii.html",
        "image": "https://kareliagid.ru/assets/images/Vodopady/432134.jpg",
        "price": "4935",
        "old_price": "6490"
    },
    {
        "offer_id": "020f1dae1d9264c80f33941c00edd836",
        "destination_id": "568",
        "destination_name": "Карелия",
        "origin_id": "300",
        "origin_name": "Рязань",
        "title": "Все водопады Карелии (Зима)",
        "url": "https://kareliagid.ru",
        "image": "https://kareliagid.ru/assets/images/Vodopady/432134.jpg",
        "price": "4935"
    }
]
)wrap");

TEST(GoogleTravelFeedParser, ExampleFeed) {

    const auto result = RunFeedParserWithTrace<NGoogleTravel::TCsvFeedParser>(
        input_csv,
        [](const TQueueItem &item) {
            NSc::TValue result;

            // Common data
            ASSERT_TRUE(item->DataCampOffer.identifiers().has_offer_id());
            result["offer_id"] = item->DataCampOffer.identifiers().offer_id();

            ASSERT_TRUE(item->GetOriginalSpecification().url().has_value());
            result["url"] = item->GetOriginalSpecification().url().value();

            result["image"] = item->DataCampOffer.pictures().partner().original().source(0).url();
            result["title"] = item->GetOriginalSpecification().name().value();

            const auto &price = item->RawPrice;
            if (price) {
                result["price"] = ToString(*price);
            }
            const auto &old_price = item->RawOldPrice;
            if (old_price) {
                result["old_price"] = ToString(*old_price);
            }

            // Special data
            ASSERT_TRUE(item->DataCampOffer.content().type_specific_content().has_travel());
            auto &travel = item->DataCampOffer.content().type_specific_content().travel();

            ASSERT_TRUE(travel.has_destination_id());
            result["destination_id"] = travel.destination_id();

            if (travel.has_destination_name()) {
                result["destination_name"] = travel.destination_name();
            }
            if (travel.has_origin_id()) {
                result["origin_id"] = travel.origin_id();
            }
            if (travel.has_origin_name()) {
                result["origin_name"] = travel.origin_name();
            }

            return result;
        },
        GetDefaultGoogleTravelFeedInfo(),
        "offers-trace.log");
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);

    ASSERT_EQ(result.ArraySize(), expected.ArraySize());
    ASSERT_EQ(result, expected);
}