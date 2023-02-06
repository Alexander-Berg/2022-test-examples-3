
#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(id;price;step-quantity
    csv-with-step-quantity;7;22
    csv-without-step-quantity;77;
    csv-with-wrong-step-quantity;7;wrong-step-quantity
)wrap");

static const TString INPUT_CSV_1(R"wrap(id;price;step_quantity
    csv-with-step-quantity;7;22
    csv-without-step-quantity;77;
    csv-with-wrong-step-quantity;7;wrong-step-quantity
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-step-quantity",
        "StepQuantity": 22,
        "StepQuantityHasValue": 1
    },
    {
        "OfferId": "csv-without-step-quantity",
        "StepQuantityHasValue": 0
    },
    {
        "OfferId": "csv-with-wrong-step-quantity",
        "StepQuantityHasValue": 0
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
                    if (item->GetOriginalTerms().has_quantity() && item->GetOriginalTerms().quantity().has_step()) {
                        result["StepQuantity"] = item->GetOriginalTerms().quantity().step();
                        result["StepQuantityHasValue"] = true;
                    } else {
                        result["StepQuantityHasValue"] = false;
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

TEST(WhiteCsvParser, StepQuantity) {
    RunParserAndAssert(INPUT_CSV, EXPECTED_JSON);
}

TEST(WhiteCsvParser, StepQuantity_0) {
    RunParserAndAssert(INPUT_CSV_1, EXPECTED_JSON);
}
