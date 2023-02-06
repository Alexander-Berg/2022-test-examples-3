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
        <shop-sku>yml-with-name</shop-sku>
        <price>1</price>
        <name>Vasya Pupkin</name>
      </offer>
      <offer>
        <shop-sku>yml-without-name</shop-sku>
        <price>2</price>
      </offer>
      <offer>
        <shop-sku>yml-with-empty-name</shop-sku>
        <price>3</price>
        <name></name>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-name",
        "Price": 1,
        "Name": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } value: \"Vasya Pupkin\" }",
    },
    {
        "OfferId": "yml-without-name",
        "Price": 2,
        "Name": "{  }",
    },
    {
        "OfferId": "yml-with-empty-name",
        "Price": 3,
        "Name": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
    },
]
)wrap");


TEST(BlueYmlParser, Name) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            result["Name"] = ToString(item->DataCampOffer.content().partner().original().name());
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
