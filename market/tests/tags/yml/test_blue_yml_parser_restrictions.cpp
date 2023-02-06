#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog>
  <shop>
    <offers>
      <offer>
        <shop-sku>ignore-restrictions</shop-sku>
        <price>1</price>
        <restrictions>
          <clients>
            <b2c>true</b2c>
            <b2b>false</b2b>
          </clients>
          <trading>
            <retail>true</retail>
            <wholesale>true</wholesale>
          </trading>
        </restrictions>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "ignore-restrictions",
        "Price": 1
    }
]
)wrap");


TEST(BlueYmlParser, Restrictions) {
    const auto [actual, checkResult] = RunBlueYmlFeedParserWithCheckFeed<NBlue::TYmlFeedParser>(
            INPUT_XML,
            [](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                if (item->RawPrice) {
                    result["Price"] = *item->RawPrice;
                }
                return result;
            },
            GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
    ASSERT_EQ(checkResult.log_message().size(), 0);
}

