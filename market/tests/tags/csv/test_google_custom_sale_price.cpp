#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/google_custom/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(ID,Item title,Final URL,Price,Sale price
    1,Item1,http://somesite.ru,100.50 RUB,90.50 RUB
    2,Item2,http://somesite.ru,100.50 RUB,90 RUB
    3,Item3,http://somesite.ru,100.50 RUB,50
    4,Item4,http://somesite.ru,100.50 RUB,15.11.22
    5,Item5,http://somesite.ru,100.50 RUB,3 ABC
    6,Item6,http://somesite.ru,100.50 RUB,3 USD
    7,Item7,http://somesite.ru,100.50 RUB,
    8,Item7,http://somesite.ru,,100.50 RUB
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {"id":"1", "currency":"RUR", "oldprice":100.5, "price":90.5},
    {"id":"2", "currency":"RUR", "oldprice":100.5, "price":90},
    {"id":"3", "currency":"RUR", "oldprice":100.5, "price":50},
    {"id":"4", "currency":"RUR", "price":100.5},
    {"id":"5", "currency":"RUR", "oldprice":100.5, "price":3},
    {"id":"6", "currency":"USD", "oldprice":100.5, "price":3},
    {"id":"7", "currency":"RUR", "price":100.5},
    {"id":"8", "currency":"RUR", "price":100.5},
]
)wrap");

TEST(GoogleCustomParser, SalePrice) {
  const auto actual = RunFeedParserWithTrace<NGoogleCustom::TCsvFeedParser>(
      INPUT_CSV,
      [](const TQueueItem &item) {
        NSc::TValue result;
        result["id"] = item->DataCampOffer.identifiers().offer_id();
        if (item->RawPrice) {
          result["price"] = *item->RawPrice;
        }
        if (item->RawOldPrice) {
          result["oldprice"] = *item->RawOldPrice;
        }
        result["currency"] = item->Currency;
        return result;
      },
      GetDefaultGoogleCustomFeedInfo(), "offers-trace.log");
  const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
  ASSERT_EQ(actual, expected);
}
