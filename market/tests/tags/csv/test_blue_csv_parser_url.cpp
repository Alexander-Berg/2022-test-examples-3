#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_CSV(R"wrap(shop-sku;price;url
    csv-with-valid-url;1;http://www.test.com/test1
    csv-with-invalid-url;2;httttttp://invalid
    csv-no-url;3;
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-valid-url",
        "Price": 1,
        "Url": "http://www.test.com/test1",
    },
    {
        "OfferId": "csv-with-invalid-url",
        "Price": 2,
        "Url": "httttttp://invalid",
    },
    {
        "OfferId": "csv-no-url",
        "Price": 3,
        "Url": "",
    }
]
)wrap");


TEST(BlueCsvParser, Url) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            if (item->DataCampOffer.content().partner().original().has_url()) {
                result["Url"] = item->DataCampOffer.content().partner().original().url().value();
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
