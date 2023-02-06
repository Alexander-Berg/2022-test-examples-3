#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/yandex_custom/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(ID;phrases;URL
    123;слово1 слово2;http://somesite.ru
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {"id":"123", "phrases": "слово1 слово2", "url":"http://somesite.ru"},
]
)wrap");

TEST(YandexCustomParser, Phrases) {
  const auto actual = RunFeedParserWithTrace<NYandexCustom::TCsvFeedParser>(
      INPUT_CSV,
      [](const TQueueItem &item) {
        NSc::TValue result;
        result["id"] = item->DataCampOffer.identifiers().offer_id();
        if (item->DataCampOffer.content().type_specific_content().has_yandex_custom()) {
          auto &yandexCustom = item->DataCampOffer.content().type_specific_content().yandex_custom();
          result["id"] = yandexCustom.original_offer_id();
          result["phrases"] = yandexCustom.custom_phrases();
        }
        result["url"] = item->GetOriginalSpecification().url().value();
        return result;
      },
      GetDefaultYandexCustomFeedInfo(), "offers-trace.log");
  const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
  ASSERT_EQ(actual, expected);
}
