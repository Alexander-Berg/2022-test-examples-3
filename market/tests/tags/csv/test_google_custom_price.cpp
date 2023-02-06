#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/google_custom/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(ID,Item title,Final URL,Price
    1,Item1,http://somesite.ru,100.35 RUB
    2,Item2,http://somesite.ru,85 RUB
    3,Item3,http://somesite.ru,33
    4,Item4,http://somesite.ru,10 USD
    5,Item5,http://somesite.ru,15.11.20
    6,Item6,http://somesite.ru,1 ABC
    7,Item7,http://somesite.ru,1 RUR
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {"id":"1", "price": 100.35, "currency": "RUR"},
    {"id":"2", "price": 85, "currency": "RUR"},
    {"id":"3", "price": 33, "currency": "RUR"},
    {"id":"4", "price": 10, "currency": "USD"},
    {"id":"5", "currency": "RUR"},
    {"id":"6", "price": 1, "currency": "RUR"},
    {"id":"7", "price": 1, "currency": "RUR"},
]
)wrap");

TEST(GoogleCustomParser, Price) {
  const auto actual = RunFeedParserWithTrace<NGoogleCustom::TCsvFeedParser>(
      INPUT_CSV,
      [](const TQueueItem &item) {
        NSc::TValue result;
        result["id"] = item->DataCampOffer.identifiers().offer_id();
        if (item->RawPrice) {
          result["price"] = *item->RawPrice;
        }
        result["currency"] = item->Currency;
        return result;
      },
      GetDefaultGoogleCustomFeedInfo(), "offers-trace.log");
  const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
  ASSERT_EQ(actual, expected);
}
