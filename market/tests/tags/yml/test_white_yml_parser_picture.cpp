#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>
#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;

static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer id="yml-with-picture">
        <price>7</price>
        <picture>http://test.picture.yandex.ru</picture>
      </offer>
      <offer id="yml-without-picture">
        <price>77</price>
      </offer>
      <offer id="yml-with-many-picture">
        <price>7</price>
        <picture>http://test.picture.yandex.ru/1</picture>
        <picture>https://test.picture.yandex.ru/2</picture>
      </offer>
      <offer id="yml-with-commas">
        <price>7</price>
        <picture>   , https://test.picture.yandex.ru/1 , http://test.picture.yandex.ru/1,255,255,255,https://test.picture.yandex.ru/3        </picture>
      </offer>
      <offer id="yml-with-wrong-picture">
        <price>7</price>
        <picture>file://wrong-picture</picture>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-picture",
        "Picture": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } source { url: \"test.picture.yandex.ru/\" source: DIRECT_LINK } }",
    },
    {
        "OfferId": "yml-without-picture",
        "Picture": "{  }",
    },
    {
        "OfferId": "yml-with-many-picture",
        "Picture": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } source { url: \"test.picture.yandex.ru/1\" source: DIRECT_LINK } source { url: \"https://test.picture.yandex.ru/2\" source: DIRECT_LINK } }"
    },
    {
        "OfferId": "yml-with-commas",
        "Picture": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } source { url: \"https://test.picture.yandex.ru/1\" source: DIRECT_LINK } source { url: \"test.picture.yandex.ru/1,255,255,255\" source: DIRECT_LINK } source { url: \"https://test.picture.yandex.ru/3\" source: DIRECT_LINK } }"
    },
    {
        "OfferId": "yml-with-wrong-picture",
        "Picture": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
    }

]
)wrap");


TEST(WhiteYmlParser, Picture) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Picture"] = ToString(item->DataCampOffer.pictures().partner().original());
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
