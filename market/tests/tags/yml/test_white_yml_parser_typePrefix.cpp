#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer id="yml-with-typePrefix">
        <price>7</price>
        <typePrefix>typePrefix</typePrefix>
      </offer>
      <offer id="yml-without-typePrefix">
        <price>77</price>
      </offer>
      <offer id="yml-with-empty-typePrefix">
        <price>77</price>
        <typePrefix></typePrefix>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-typePrefix",
        "TypePrefix": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } value: \"typePrefix\" }",
    },
    {
        "OfferId": "yml-without-typePrefix",
        "TypePrefix": "{  }",
    },
    {
        "OfferId": "yml-with-empty-typePrefix",
        "TypePrefix": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
    },
]
)wrap");


TEST(WhiteYmlParser, TypePrefix) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["TypePrefix"] = ToString(item->GetOriginalSpecification().type_prefix());
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
