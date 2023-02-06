#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;

// ПРОВЕРЬ, что в документации имя CSV именно такое
static const TString INPUT_CSV(R"wrap(shop-sku;price;downloadable
    csv-with-downloadable-true;7;true
    csv-with-downloadable-false;7;false
    csv-without-downloadable;77;
    csv-with-wrong-downloadable;7;wrong-downloadable
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "Downloadable": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } flag: true }",
        "OfferId": "csv-with-downloadable-true",
    },
    {
        "Downloadable": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } flag: false }",
        "OfferId": "csv-with-downloadable-false",
    },
    {
        "Downloadable": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "OfferId": "csv-without-downloadable",
    },
    {
        "Downloadable": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "OfferId": "csv-with-wrong-downloadable",
    },
]
)wrap");


TEST(BlueCsvParser, Downloadable) {
const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Downloadable"] = ToString(item->GetOriginalSpecification().downloadable());
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
