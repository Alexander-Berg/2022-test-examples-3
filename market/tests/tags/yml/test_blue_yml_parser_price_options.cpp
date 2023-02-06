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
        <shop-sku>ignore-price-options</shop-sku>
        <price>1</price>
        <price-option>
          <min-quantity>1</min-quantity>
          <shipment-days>5</shipment-days>
          <discount unit="%">5</discount>
        </price-option>
        <price-option>
          <min-quantity>10</min-quantity>
          <shipment-days>5</shipment-days>
          <discount unit="%">7</discount>
        </price-option>
        <price-option>
           <min-quantity>10</min-quantity>
           <min-order-sum>50000</min-order-sum>
           <shipment-days>5</shipment-days>
           <discount unit="%">8</discount>
        </price-option>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "ignore-price-options",
        "Price": 1
    }
]
)wrap");


TEST(BlueYmlParser, PriceOptions) {
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

