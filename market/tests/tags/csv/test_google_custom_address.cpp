#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/google_custom/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(ID,Item title,Final URL,Item address
    1,Item1,http://somesite.ru,"Королев, Московская обл., Россия"
    2,Item2,http://somesite.ru,"ул. Богомолова, д. 123, г. Королев, Московская обл., 141070"
    3,Item3,http://somesite.ru,"55.9112, 37.8167"
    4,Item4,http://somesite.ru,
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {"id":"1", "address": "Королев, Московская обл., Россия"},
    {"id":"2", "address": "ул. Богомолова, д. 123, г. Королев, Московская обл., 141070"},
    {"id":"3", "address": "55.9112, 37.8167"},
    {"id":"4", "address": ""},
]
)wrap");

TEST(GoogleCustomParser, Address) {
  const auto actual = RunFeedParserWithTrace<NGoogleCustom::TCsvFeedParser>(
      INPUT_CSV,
      [](const TQueueItem &item) {
        NSc::TValue result;
        result["id"] = item->DataCampOffer.identifiers().offer_id();
        if (item->DataCampOffer.content().type_specific_content().has_google_custom()) {
          auto &googleCustom = item->DataCampOffer.content().type_specific_content().google_custom();
          result["address"] = googleCustom.address();
        }
        return result;
      },
      GetDefaultGoogleCustomFeedInfo(), "offers-trace.log");
  const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
  ASSERT_EQ(actual, expected);
}
