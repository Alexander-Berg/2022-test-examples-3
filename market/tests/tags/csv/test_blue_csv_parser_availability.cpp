#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

// ПРОВЕРЬ, что в документации имя CSV именно такое
static const TString INPUT_CSV(R"wrap(shop-sku;price;availability
    csv-with-availability;7;INACTIVE
    csv-with-availability2;7; activE
    csv-with-availability3;7; Архивный
    csv-without-availability;77;
    csv-with-wrong-availability;7;wrong-availability
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "Id": "csv-with-availability",
        "Value": "WONT_SUPPLY",
    },
    {
        "Id": "csv-with-availability2",
        "Value": "WILL_SUPPLY",
    },
    {
        "Id": "csv-with-availability3",
        "Value": "ARCHIVE",
    },
    {
        "Id": "csv-without-availability"
    },
    {
        "Id": "csv-with-wrong-availability"
    }
]
)wrap");


TEST(BlueCsvParser, Availability) {
const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["Id"] = item->DataCampOffer.identifiers().offer_id();
            auto supplyPlan = item->GetOriginalTerms().supply_plan();
            if(supplyPlan.has_value()) {
                result["Value"] = Market::DataCamp::SupplyPlan_Variation_Name(supplyPlan.value());
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
