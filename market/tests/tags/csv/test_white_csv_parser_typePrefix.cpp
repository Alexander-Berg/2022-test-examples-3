#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;

static const TString INPUT_CSV(R"wrap(id;price;typePrefix
    csv-with-typePrefix;7;typePrefix
    csv-without-typePrefix;77;
    csv-with-wrong-typePrefix;7;wrong-typePrefix
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-typePrefix",
        "TypePrefix": "typePrefix",
    },
    {
        "OfferId": "csv-without-typePrefix",
        "TypePrefix": "",
    },
    {
        "OfferId": "csv-with-wrong-typePrefix",
        "TypePrefix": "wrong-typePrefix",
    }
]
)wrap");


TEST(WhiteCsvParser, TypePrefix) {
const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["TypePrefix"] = item->GetOriginalSpecification().type_prefix().value();
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
