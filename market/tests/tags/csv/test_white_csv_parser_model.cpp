#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_CSV(R"wrap(id;price;model
    csv-with-model;1;IPhone 5
    csv-without-model;2;
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-model",
        "Price": 1,
        "Model": "IPhone 5",
    },
    {
        "OfferId": "csv-without-model",
        "Price": 2,
        "Model": "",
    }
]
)wrap");


TEST(WhiteCsvParser, Model) {
const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
    INPUT_CSV,
    [](const TQueueItem& item) {
      NSc::TValue result;
      result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
      if (item->RawPrice) {
          result["Price"] = *item->RawPrice;
      }
      result["Model"] = item->DataCampOffer.content().partner().original().model().value();
      return result;
    },
    GetDefaultWhiteFeedInfo(EFeedType::CSV),
    "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
