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
         <shop-sku>without-price</shop-sku>
      </offer>
      <offer>
         <shop-sku>with-zero-price</shop-sku>
         <price>0</price>
      </offer>
      <offer>
         <shop-sku>price-8990</shop-sku>
         <price>8990</price>
      </offer>
      <offer>
         <shop-sku>price-8990-01</shop-sku>
         <price>8990.01</price>
      </offer>
      <offer>
         <shop-sku>price-8999-99</shop-sku>
         <price>   8999,99   </price>
      </offer>
      <offer>
         <shop-sku>price-lalala</shop-sku>
         <price> lalala </price>
      </offer>
      <offer>
         <shop-sku>price--9001</shop-sku>
         <price>-9001</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "without-price",
        "RawPrice": "(empty maybe)",
        "IsValid": 1,
    },
    {
        "OfferId": "with-zero-price",
        "RawPrice": "0",
        "IsValid": 1,
    },
    {
        "OfferId": "price-8990",
        "Price": 8990,
        "RawPrice": "8990",
        "IsValid": 1,
    },
    {
        "OfferId": "price-8990-01",
        "Price": 8990.01,
        "RawPrice": "8990.01",
        "IsValid": 1,
    },
    {
        "OfferId": "price-8999-99",
        "Price": 8999.99,
        "RawPrice": "8999,99",
        "IsValid": 1,
    },
    {
        "OfferId": "price-lalala",
        "RawPrice": "lalala",
        "IsValid": 1,
    },
    {
        "OfferId": "price--9001",
        "RawPrice": "-9001",
        "IsValid": 1,
    },
]
)wrap");


TEST(BlueYmlParser, Price) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            result["RawPrice"] = ToString(item->RawOriginalPrice);
            result["IsValid"] = item->IsValid;
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
