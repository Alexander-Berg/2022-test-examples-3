#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_CSV(R"wrap(shop-sku;price;country_of_origin
    csv-with-country-of-origin;7;Россия, Китай, Буркина Фасо
    csv-without-country-of-origin;77;
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-country-of-origin",
        "Price": 7,
        "CountryOfOrigin": "Россия, Китай, Буркина Фасо",
    },
    {
        "OfferId": "csv-without-country-of-origin",
        "Price": 77,
        "CountryOfOrigin": "",
    }
]
)wrap");


TEST(BlueCsvParser, CountryOfOrigin) {
const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            result["CountryOfOrigin"] = ToString(item->DataCampOffer.Getcontent().Getpartner().Getoriginal().Getcountry_of_origin().Getvalue());
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}


