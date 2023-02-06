#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;

static const TString INPUT_CSV(R"wrap(id;price;available
    csv-with-available;7;false
    csv-without-available;77;
    csv-with-wrong-available;7;wrong-available
    csv-with-unknown-available;42;unknown
)wrap");
// Bad test in old FP: https://a.yandex-team.ru/arc/trunk/arcadia/market/idx/feeds/feedparser/test/feeds/retcode/bad_available_attribute.xml?rev=7526871#L19


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-available",
        "Available": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } flag: false }",
        "Deadline": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "IsValid": 1
    },
    {
        "OfferId": "csv-without-available",
        "Available": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "Deadline": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "IsValid": 1
    },
    {
        "OfferId": "csv-with-wrong-available",
        "Available": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "Deadline": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "IsValid": 1
    },
]
)wrap");


TEST(WhiteCsvParser, Available) {
const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Available"] = ToString(item->GetOriginalPartnerDelivery().available());
            result["Deadline"] = ToString(item->GetOriginalPartnerDelivery().deadline());
            result["IsValid"] = item->IsValid;
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
