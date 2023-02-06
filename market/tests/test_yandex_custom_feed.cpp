#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/yandex_custom/csv/feed_parser.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>
#include <util/generic/maybe.h>
#include <util/stream/file.h>

#include <google/protobuf/text_format.h>

using namespace NMarket;

namespace {

const auto fname =
    JoinFsPaths(ArcadiaSourceRoot(),
                "market/idx/feeds/qparser/tests/data/yandex_custom_feed.csv");
auto fStream = TUnbufferedFileInput(fname);
const auto input_csv = fStream.ReadAll();

} // namespace

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "currency": "RUB",
        "description": "Цифровой модуль ввода/вывод",
        "id": "2665",
        "id2": "0224261001",
        "image": "https://wdml.ru/",
        "oldprice": "3000",
        "price": "2638.24",
        "title": "0224261001 RS F20 LP2N 5/20",
        "url": "https://wdml.ru/cat/0224261001"
    },
    {
        "currency": "RUB",
        "description": "Измерительная клемма с размыкателем",
        "id": "24747",
        "id2": "1000010001",
        "image": "https://wdml.ru/upload/shop_3/2/4/7/item_24747/item_image24747.jpg",
        "oldprice": "350.34",
        "price": "303.6",
        "title": "1000010001 WTR 4 SL RT",
        "url": "https://wdml.ru/cat/1000010001"
    },
    {
        "currency": "RUB",
        "description": "Скоба экрана",
        "id": "24749",
        "id2": "1000950001",
        "image": "https://wdml.ru/upload/shop_3/2/4/7/item_24749/item_image24749.jpg",
        "oldprice": "2300.64",
        "price": "2114.64",
        "title": "1000950001 SHIELD LEVER 8 MOD 35mm",
        "url": "https://wdml.ru/cat/1000950001"
    }
]
)wrap");

TEST(YandexCustomFeedParser, ExampleFeed) {

  const auto result = RunFeedParserWithTrace<NYandexCustom::TCsvFeedParser>(
      input_csv,
      [](const TQueueItem &item) {
        NSc::TValue result;
        result["url"] = item->GetOriginalSpecification().url().value();
        result["image"] =
            item->DataCampOffer.pictures().partner().original().source(0).url();
        result["title"] = item->GetOriginalSpecification().name().value();
        result["description"] =
            item->GetOriginalSpecification().description().value();
        result["currency"] = item->Currency;

        const auto &price = item->RawPrice;
        if (price) {
          result["price"] = ToString(*price);
        }

        const auto &oldprice = item->RawOldPrice;
        if (oldprice) {
          result["oldprice"] = ToString(*oldprice);
        }
        ASSERT_TRUE(item->DataCampOffer.content().type_specific_content().has_yandex_custom());
        auto &yandexCustom = item->DataCampOffer.content().type_specific_content().yandex_custom();
        result["id"] = yandexCustom.original_offer_id();
        result["id2"] = yandexCustom.id2();
        return result;
      },
      GetDefaultYandexCustomFeedInfo(), "offers-trace.log");
  const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
  ASSERT_EQ(result, expected);
}
