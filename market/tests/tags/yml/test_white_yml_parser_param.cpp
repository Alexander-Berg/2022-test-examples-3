#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer>
        <shop-sku>yml-with-param</shop-sku>
        <price>7</price>
        <param name="Чухонец">убогий</param>
        <param name="Рост Дюймовочки" unit="дюйм">1</param>
      </offer>
      <offer>
        <shop-sku>yml-without-param</shop-sku>
        <price>77</price>
      </offer>
      <offer>
        <shop-sku>yml-with-named-empty-param</shop-sku>
        <price>8</price>
        <param name="Цвет"></param>
        <param name="Тип"></param>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-param",
        "Param": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } param { name: \"Чухонец\" unit: \"\" value: \"убогий\" } param { name: \"Рост Дюймовочки\" unit: \"дюйм\" value: \"1\" } }",
    },
    {
        "OfferId": "yml-without-param",
        "Param": "{  }",
    },
    {
        "OfferId": "yml-with-named-empty-param",
        "Param": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
    }
]
)wrap");


TEST(BlueYmlParser, Param) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Param"] = ToString(item->GetOriginalSpecification().offer_params());
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
