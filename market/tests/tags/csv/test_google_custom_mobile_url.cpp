#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/google_custom/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(ID,Item title,Final URL,Final mobile URL
    1,Item1,http://somesite.ru,http://m.somesite.ru
    2,Item2,http://somesite.ru,
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {"id":"1", "mobile": "http://m.somesite.ru"},
    {"id":"2"},
]
)wrap");

TEST(GoogleCustomParser, MobileUrl) {
  const auto actual = RunFeedParserWithTrace<NGoogleCustom::TCsvFeedParser>(
      INPUT_CSV,
      [](const TQueueItem &item) {
        NSc::TValue result;
        result["id"] = item->DataCampOffer.identifiers().offer_id();
        if (item->DataCampOffer.content().type_specific_content().has_google_custom()) {
          auto &googleCustom = item->DataCampOffer.content().type_specific_content().google_custom();
          result["mobile"] = googleCustom.mobile_url();
        }
        return result;
      },
      GetDefaultGoogleCustomFeedInfo(), "offers-trace.log");
  const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
  ASSERT_EQ(actual, expected);
}
