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
         <shop-sku>without-oldprice</shop-sku>
         <price>1990.00</price>
      </offer>
      <offer>
         <shop-sku>oldprice-8990</shop-sku>
         <price>1990.00</price>
         <oldprice>8990</oldprice>
      </offer>
      <offer>
         <shop-sku>oldprice-8990-01</shop-sku>
         <price>1990.00</price>
         <oldprice>8990.01</oldprice>
      </offer>
      <offer>
         <shop-sku>oldprice-8999-99</shop-sku>
         <price>1990.00</price>
         <oldprice>   8999,99   </oldprice>
      </offer>
      <offer>
         <shop-sku>oldprice-lalala</shop-sku>
         <price>1990.00</price>
         <oldprice>lalala</oldprice>
      </offer>
      <offer>
         <shop-sku>oldprice--9001</shop-sku>
         <price>100</price>
         <oldprice>-9001</oldprice>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "without-oldprice",
        "OldPrice": "(empty maybe)",
    },
    {
        "OfferId": "oldprice-8990",
        "OldPrice": "8990",
    },
    {
        "OfferId": "oldprice-8990-01",
        "OldPrice": "8990.01",
    },
    {
        "OfferId": "oldprice-8999-99",
        "OldPrice": "8999.99",
    },
    {
        "OfferId": "oldprice-lalala",
        "OldPrice": "(empty maybe)",
    },
    {
        "OfferId": "oldprice--9001",
        "OldPrice": "(empty maybe)",
    },
]
)wrap");


TEST(BlueYmlParser, OldPrice) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["OldPrice"] = ToString(item->RawOldPrice);
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
