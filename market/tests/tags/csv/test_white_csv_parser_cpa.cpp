#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

// ПРОВЕРЬ, что в документации имя CSV именно такое
static const TString INPUT_CSV(R"wrap(id;price;cpa
    csv-with-cpa;7;1
    csv-without-cpa;77;
    csv-with-wrong-cpa;7;3
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-cpa",
        "cpa": 1,
    },
    {
        "OfferId": "csv-without-cpa"
    },
    {
        "OfferId": "csv-with-wrong-cpa"
    }
]
)wrap");


TEST(WhiteCsvParser, cpa) {
const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->Cpa != ECpaOption::NOTSET) {
                result["cpa"] = static_cast<int>(item->Cpa);
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
