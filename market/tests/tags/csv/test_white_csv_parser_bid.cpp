#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;price;bid
    csv-with-bid;7;123
    csv-without-bid;77;
    csv-with-max-bid;7;100000000
    csv-with-wrong-bid;7;wrong-bid
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-bid",
        "Bid": 123,
    },
    {
        "OfferId": "csv-without-bid",
    },
    {
        "OfferId": "csv-with-max-bid",
        "Bid": 8400,
    },
    {
        "OfferId": "csv-with-wrong-bid",
    },
]
)wrap");


TEST(BlueCsvParser, Bid) {
const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            if (item->DataCampOffer.bids().bid().has_value()) {
                result["Bid"] = item->DataCampOffer.bids().bid().value();
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
