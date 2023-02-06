#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

// test https://a.yandex-team.ru/arc/trunk/arcadia/market/idx/feeds/feedparser/test/yatf/positive/test_expiry.py?rev=7541529
// https://a.yandex-team.ru/arc/trunk/arcadia/market/idx/feeds/feedparser/test/yatf/negative/test_expiry_negative.py?rev=7541529

// 1635595200 is epoch timestamp for 2021-10-30T12:00 GMT

static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer>
        <shop-sku>yml-with-expiry</shop-sku>
        <price>7</price>
        <expiry>P2Y2M10DT2H30M</expiry>
      </offer>
      <offer>
        <shop-sku>yml-with-expiry-date</shop-sku>
        <price>77</price>
        <expiry>2021-10-30T12:00 +0000</expiry>
      </offer>
      <offer>
        <shop-sku>yml-without-expiry</shop-sku>
        <price>77</price>
      </offer>
      <offer>
        <shop-sku>yml-with-wrong-expiry</shop-sku>
        <price>77</price>
        <expiry>2021.10.30.12.00</expiry>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-expiry",
        "Expiry": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } validity_period { years: 2 months: 2 days: 10 hours: 2 minutes: 30 } }",
    },
    {
        "OfferId": "yml-with-expiry-date",
        "Expiry": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
    },
    {
        "OfferId": "yml-without-expiry",
        "Expiry": "{  }",
    },
    {
        "OfferId": "yml-with-wrong-expiry",
        "Expiry": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
    }
]
)wrap");


TEST(BlueYmlParser, Expiry) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Expiry"] = ToString(item->GetOriginalSpecification().expiry());
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
