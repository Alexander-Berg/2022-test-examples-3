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
        <shop-sku>yml-with-downloadable-true</shop-sku>
        <price>7</price>
        <downloadable>true</downloadable>
      </offer>
      <offer>
        <shop-sku>yml-with-downloadable-false</shop-sku>
        <price>7</price>
        <downloadable>false</downloadable>
      </offer>
      <offer>
        <shop-sku>yml-without-downloadable</shop-sku>
        <price>77</price>
      </offer>
      <offer>
        <shop-sku>yml-with-wrong-downloadable</shop-sku>
        <price>7</price>
        <downloadable>wrong-downloadable</downloadable>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "Downloadable": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } flag: true }",
        "OfferId": "yml-with-downloadable-true",
    },
    {
        "Downloadable": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } flag: false }",
        "OfferId": "yml-with-downloadable-false",
    },
    {
        "Downloadable": "{  }",
        "OfferId": "yml-without-downloadable",
    },
    {
        "Downloadable": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "OfferId": "yml-with-wrong-downloadable",
    }
]
)wrap");


TEST(BlueYmlParser, Downloadable) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Downloadable"] = ToString(item->GetOriginalSpecification().downloadable());
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
