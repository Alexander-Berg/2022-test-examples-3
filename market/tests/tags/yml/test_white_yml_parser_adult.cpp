#include <market/idx/feeds/qparser/tests/test_utils.h>
#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer id="white-yml-with-adult-true">
        <adult>true</adult>
        <price>7</price>
      </offer>
      <offer id="white-yml-with-adult-false">
        <adult>false</adult>
        <price>77</price>
      </offer>
      <offer id="white-yml-without-adult">
        <price>777</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "Adult": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } flag: true }",
        "OfferId": "white-yml-with-adult-true",
    },
    {
        "Adult": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } flag: false }",
        "OfferId": "white-yml-with-adult-false",
    },
    {
        "Adult": "{  }",
        "OfferId": "white-yml-without-adult",
    }
]
)wrap");


TEST(WhiteYmlParser, Adult) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["Adult"] = ToString(item->DataCampOffer.content().partner().original().adult());
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
