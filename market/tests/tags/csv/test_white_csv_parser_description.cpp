#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>
using namespace NMarket;
static const TString INPUT_CSV(R"wrap(shop-sku;price;description
    csv-with-description;7;Купание красного коня
    csv-without-description;77;
)wrap");
static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-description",
        "Price": 7,
        "Description": "Купание красного коня",
    },
    {
        "OfferId": "csv-without-description",
        "Price": 77,
        "Description": "",
    }
]
)wrap");
TEST(BlueCsvParser, Description) {
const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            result["Description"] = item->DataCampOffer.Getcontent().Getpartner().Getoriginal().Getdescription().Getvalue();
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
