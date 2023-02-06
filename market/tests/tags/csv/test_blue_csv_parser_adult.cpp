#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;price;adult
    csv-blue-adult-true;100;true
    csv-blue-adult-false;100;false
    csv-blue-without-adult;100;
    csv-blue-adult-wrong;100;qwe
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-blue-adult-true",
        "Adult": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } flag: true }",
        "IsValid": 1,
    },
    {
        "OfferId": "csv-blue-adult-false",
        "Adult": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } flag: false }",
        "IsValid": 1,
    },
    {
        "OfferId": "csv-blue-without-adult",
        "Adult": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "IsValid": 1,
    },
    {
        "OfferId": "csv-blue-adult-wrong",
        "Adult": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "IsValid": 1,
    },
]
)wrap");

TEST(BlueCsvParser, Adult) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["IsValid"] = item->IsValid;
            if (item->IsValid) {
                result["Adult"] = ToString(item->GetOriginalSpecification().adult());
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
