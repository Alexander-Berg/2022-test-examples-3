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
        <shop-sku>blue-yml-with-adult-true</shop-sku>
        <adult>true</adult>
        <price>7</price>
      </offer>
      <offer>
        <shop-sku>blue-yml-with-adult-false</shop-sku>
        <adult>false</adult>
        <price>7</price>
      </offer>
      <offer>
        <shop-sku>blue-yml-without-adult</shop-sku>
        <price>7</price>
      </offer>
      <offer>
        <shop-sku>blue-yml-empty-adult</shop-sku>
        <price>7</price>
        <adult></adult>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "Adult": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } flag: true }",
        "OfferId": "blue-yml-with-adult-true",
    },
    {
        "Adult": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } flag: false }",
        "OfferId": "blue-yml-with-adult-false",
    },
    {
        "Adult": "{  }",
        "OfferId": "blue-yml-without-adult",
    },
    {
        "Adult": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "OfferId": "blue-yml-empty-adult",
    }
]
)wrap");


TEST(BlueYmlParser, Adult) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Adult"] = ToString(item->DataCampOffer.content().partner().original().adult());
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
