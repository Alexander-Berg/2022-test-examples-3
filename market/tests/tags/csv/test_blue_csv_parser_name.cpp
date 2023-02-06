#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_CSV(R"wrap(shop-sku;price;name
    csv-with-name;1;Vasya Pupkin
    csv-without-name;2;
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-name",
        "Price": 1,
        "Name": "Vasya Pupkin",
        "RawTitle": "Vasya Pupkin",
    },
    {
        "OfferId": "csv-without-name",
        "Price": 2,
        "Name": "",
        "RawTitle": "",
    }
]
)wrap");


TEST(BlueCsvParser, Name) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            result["Name"] = item->DataCampOffer.content().partner().original().name().value();
            result["RawTitle"] = item->DataCampOffer.content().partner().original().name().value();
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
