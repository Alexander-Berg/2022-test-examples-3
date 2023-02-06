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
      <offer available="false">
        <shop-sku>yml-with-available</shop-sku>
        <price>7</price>
      </offer>
      <offer>
        <shop-sku>yml-without-available</shop-sku>
        <price>77</price>
      </offer>
      <offer available="">
        <shop-sku>yml-with-empty-available</shop-sku>
        <price>77</price>
      </offer>
      <offer id="invalid-available" available="something">
        <shop-sku>yml-with-invalid-available</shop-sku>
        <price>78</price>
      </offer>
      <offer available="unknown">
        <shop-sku>yml-with-unknown-available</shop-sku>
        <price>42</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-available",
        "Available": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } flag: false }",
        "Deadline": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "IsValid": 1
    },
    {
        "OfferId": "yml-without-available",
        "Available": "{  }",
        "Deadline": "{  }",
        "IsValid": 1
    },
    {
        "OfferId": "yml-with-empty-available",
        "Available": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "Deadline": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "IsValid": 1
    },
    {
        "OfferId": "yml-with-invalid-available",
        "Available": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "Deadline": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "IsValid": 1
    },
    {
        "OfferId": "yml-with-unknown-available",
        "Available": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "Deadline": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } value: UNKNOWN }",
        "IsValid": 1
    },
]
)wrap");


TEST(BlueYmlParser, Available) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Available"] = ToString(item->GetOriginalPartnerDelivery().available());
            result["Deadline"] = ToString(item->GetOriginalPartnerDelivery().deadline());
            result["IsValid"] = item->IsValid;
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
