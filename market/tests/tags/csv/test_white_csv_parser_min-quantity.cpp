#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(id;price;min-quantity
    csv-with-min-quantity;7;22
    csv-without-min-quantity;77;
    csv-with-wrong-min-quantity;7;wrong-min-quantity
)wrap");

static const TString INPUT_CSV_1(R"wrap(id;price;min_quantity
    csv-with-min-quantity;7;22
    csv-without-min-quantity;77;
    csv-with-wrong-min-quantity;7;wrong-min-quantity
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-min-quantity",
        "MinQuantity": 22,
        "MinQuantityHasValue": 1
    },
    {
        "OfferId": "csv-without-min-quantity",
        "MinQuantityHasValue": 0
    },
    {
        "OfferId": "csv-with-wrong-min-quantity",
        "MinQuantityHasValue": 0
    }
]
)wrap");

namespace {
    void RunParserAndAssert(const TString& inputCsv, const TString& expectedJson) {
        const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
                inputCsv,
                [](const TQueueItem& item) {
                    NSc::TValue result;
                    result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                    if (item->GetOriginalTerms().has_quantity() && item->GetOriginalTerms().quantity().has_min()) {
                        result["MinQuantity"] = item->GetOriginalTerms().quantity().min();
                        result["MinQuantityHasValue"] = true;
                    } else {
                        result["MinQuantityHasValue"] = false;
                    }
                    return result;
                },
                GetDefaultWhiteFeedInfo(EFeedType::CSV),
                "offers-trace.log"
        );
        const auto expected = NSc::TValue::FromJson(expectedJson);
        ASSERT_EQ(actual, expected);
    }
}

TEST(WhiteCsvParser, MinQuantity) {
    RunParserAndAssert(INPUT_CSV, EXPECTED_JSON);
}

TEST(WhiteCsvParser, MinQuantity_0) {
    RunParserAndAssert(INPUT_CSV_1, EXPECTED_JSON);
}
