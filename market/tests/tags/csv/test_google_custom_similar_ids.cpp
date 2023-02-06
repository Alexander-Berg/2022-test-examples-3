#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/google_custom/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(ID,Item title,Final URL,Similar IDs
    1,Item1,http://somesite.ru,"2,3"
    2,Item2,http://somesite.ru,"1,3"
    3,Item3,http://somesite.ru,"1,2"
    4,Item4,http://somesite.ru,
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {"id":"1", "similar_ids": ["2", "3"]},
    {"id":"2", "similar_ids": ["1", "3"]},
    {"id":"3", "similar_ids": ["1", "2"]},
    {"id":"4", "similar_ids": []},
]
)wrap");

TEST(GoogleCustomParser, SimilarIds) {
  const auto actual = RunFeedParserWithTrace<NGoogleCustom::TCsvFeedParser>(
      INPUT_CSV,
      [](const TQueueItem &item) {
        NSc::TValue result;
        result["id"] = item->DataCampOffer.identifiers().offer_id();
        if (item->DataCampOffer.content().type_specific_content().has_google_custom()) {
          auto &googleCustom = item->DataCampOffer.content().type_specific_content().google_custom();
          result["similar_ids"].SetArray();
          for (auto similar_id : googleCustom.similar_ids()) {
            result["similar_ids"].Push(similar_id);
          }
        }
        return result;
      },
      GetDefaultGoogleCustomFeedInfo(), "offers-trace.log");
  const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
  ASSERT_EQ(actual, expected);
}
