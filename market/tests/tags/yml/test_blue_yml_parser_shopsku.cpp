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
        <price>29490.00</price>
      </offer>
      <offer id="tyui">
        <price>19490.00</price>
      </offer>
      <offer>
        <shop-sku>qwer</shop-sku>
        <price>39490.00</price>
      </offer>
      <offer>
        <shop-sku>qwer1</shop-sku>
        <shop-sku>qwer2</shop-sku>
        <price>49490.00</price>
      </offer>
      <offer>
        <shop_sku>asdf1</shop_sku>
        <shop_sku>asdf2</shop_sku>
        <price>59490.00</price>
      </offer>
      <offer>
        <shop-sku>fgh</shop-sku>
        <shop_sku>hgf</shop_sku>
        <price>69490.00</price>
      </offer>
      <offer id="aba">
        <shop_sku>baba</shop_sku>
        <price>79490.00</price>
      </offer>
      <offer>
        <shop_sku>this shop sku has spaces</shop_sku>
        <price>79490.00</price>
      </offer>
      <offer id="offerid">
        <shop_sku>this shop sku has spaces and offer_id</shop_sku>
        <price>79490.00</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "tyui",
        "Price": 19490.00,
        "IsValid": 1,
        "RawOfferId": "tyui",
    },
    {
        "OfferId": "qwer",
        "Price": 39490.00,
        "IsValid": 1,
    },
    {
        "OfferId": "baba",
        "Price": 79490.00,
        "IsValid": 1,
        "RawOfferId": "aba",
    },
]
)wrap");


TEST(BlueYmlParser, ShopSkuTagCountRestriction) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            result["IsValid"] = item->IsValid;
            if (item->RawOfferId) {
                result["RawOfferId"] = *item->RawOfferId;
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
