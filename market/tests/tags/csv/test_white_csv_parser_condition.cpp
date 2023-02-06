#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

// ПРОВЕРЬ, что в документации имя CSV именно такое
static const TString INPUT_CSV(R"wrap(id;price;condition-type;condition-reason
    csv-with-condition-1;7;used;reason
    csv-with-condition-2;7;likenew;reason
    csv-with-condition-bad;7;trash;reason
    csv-without-condition;77;
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {"OfferId":"csv-with-condition-1","Reason":"reason","Type":"USED"},
    {"OfferId":"csv-with-condition-2","Reason":"reason","Type":"LIKENEW"},
    {"OfferId":"csv-with-condition-bad","Reason":"reason"},
    {"OfferId":"csv-without-condition"}
]
)wrap");


TEST(WhiteCsvParser, Condition) {
const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if(item->GetOriginalSpecification().condition().has_type()) {
              result["Type"] = Market::DataCamp::Condition_Type_Name(item->GetOriginalSpecification().condition().type());
            }
            if (item->GetOriginalSpecification().condition().has_reason()) {
              result["Reason"] = item->GetOriginalSpecification().condition().reason();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
