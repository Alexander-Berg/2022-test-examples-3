#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/google_custom/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(ID,Item title,Final URL,Formatted price
    1,Item1,http://somesite.ru,От 100 рублей
    2,Item2,http://somesite.ru,По низкой цене
    3,Item3,http://somesite.ru,
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {"id":"1", "formatted_price": "От 100 рублей"},
    {"id":"2", "formatted_price": "По низкой цене"},
    {"id":"3", "formatted_price": ""},
]
)wrap");

TEST(GoogleCustomParser, FormattedPrice) {
  const auto actual = RunFeedParserWithTrace<NGoogleCustom::TCsvFeedParser>(
      INPUT_CSV,
      [](const TQueueItem &item) {
        NSc::TValue result;
        result["id"] = item->DataCampOffer.identifiers().offer_id();
        if (item->DataCampOffer.content().type_specific_content().has_google_custom()) {
          auto &googleCustom = item->DataCampOffer.content().type_specific_content().google_custom();
          result["formatted_price"] = googleCustom.formatted_price();
        }
        return result;
      },
      GetDefaultGoogleCustomFeedInfo(), "offers-trace.log");
  const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
  ASSERT_EQ(actual, expected);
}
