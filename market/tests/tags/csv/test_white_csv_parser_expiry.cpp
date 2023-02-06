#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;price;expiry
    csv-with-expiry;7;P1Y2M10DT2H30M
    csv-with-expiry-date;7;2021-10-30T12:00 +0300
    csv-without-expiry;77;
    csv-with-wrong-expiry;7;P1Y2M10DT2H30111nM
)wrap");

// 1635584400 is epoch timestamp for 2021-10-30T12:00 +0300 (local time)

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-expiry",
        "Expiry": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } validity_period { years: 1 months: 2 days: 10 hours: 2 minutes: 30 } }",
    },
    {
        "OfferId": "csv-with-expiry-date",
        "Expiry": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
    },
    {
        "OfferId": "csv-without-expiry",
        "Expiry": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
    },
    {
        "OfferId": "csv-with-wrong-expiry",
        "Expiry": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
    }
]
)wrap");


TEST(BlueCsvParser, Expiry) {
const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Expiry"] = ToString(item->GetOriginalSpecification().expiry());

            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
