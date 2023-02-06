#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <functional>

using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-03-26 13:10">
  <shop>
    <offers>
      <offer>
         <shop-sku>without-disabled</shop-sku>
         <price>1990.00</price>
      </offer>
      <offer>
         <shop-sku>disabled-True</shop-sku>
         <price>1990.00</price>
         <disabled>True</disabled>
      </offer>
      <offer>
         <shop-sku>disabled-true</shop-sku>
         <price>1990.00</price>
         <disabled>true</disabled>
      </offer>
      <offer>
         <shop-sku>disabled-False</shop-sku>
         <price>1990.00</price>
         <disabled>False</disabled>
      </offer>
      <offer>
         <shop-sku>disabled-false</shop-sku>
         <price>1990.00</price>
         <disabled>false</disabled>
      </offer>
      <offer>
         <shop-sku>disabled-lalala</shop-sku>
         <price>1990.00</price>
         <disabled>lalala</disabled>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "without-disabled",
        "IsDisabled": false,
    },
    {
        "OfferId": "disabled-True",
        "IsDisabled": true,
    },
    {
        "OfferId": "disabled-true",
        "IsDisabled": true,
    },
    {
        "OfferId": "disabled-False",
        "IsDisabled": false,
    },
    {
        "OfferId": "disabled-false",
        "IsDisabled": false,
    },
    {
        "OfferId": "disabled-lalala",
        "IsDisabled": false,
    },
]
)wrap");


TEST(BlueYmlParser, IsDisabled) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["IsDisabled"] = item->IsDisabled;
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
