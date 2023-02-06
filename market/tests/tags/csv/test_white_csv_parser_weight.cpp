#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_CSV(R"wrap(id;price;weight
    csv-white-with-weight;1;123.45
    csv-white-without-weight;2;
    csv-white-with-invalid-weight;3;-0.1
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-white-with-weight",
        "Price": 1,
        "Weight": 123450000,
    },
    {
        "OfferId": "csv-white-without-weight",
        "Price": 2,
        "Weight": 0,
    },
    {
        "OfferId": "csv-white-with-invalid-weight",
        "Price": 3,
        "Weight": 0,
    },
]
)wrap");


TEST(WhiteCsvParser, Weight) {

    const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            result["Weight"] = item->DataCampOffer.content().partner().original().weight().value_mg();
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
