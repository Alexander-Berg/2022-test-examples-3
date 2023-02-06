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
        <shop-sku>yml-with-enable-auto-discounts-flag</shop-sku>
        <price>1</price>
        <enable_auto_discounts>false</enable_auto_discounts>
      </offer>
      <offer>
        <shop-sku>yml-without-enable-auto-discounts-flag</shop-sku>
        <price>2</price>
        <enable-auto-discounts>invalid</enable-auto-discounts>
      </offer>
      <offer>
        <shop-sku>yml-with-invalid-enable-auto-discounts-flag</shop-sku>
        <price>3</price>
      </offer>
      <offer>
        <shop-sku>yml-with-enable-auto-discounts-flag-yes</shop-sku>
        <price>4</price>
        <enable_auto_discounts>yes</enable_auto_discounts>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-enable-auto-discounts-flag",
        "Price": 1,
        "EnableAutoDiscounts": "0",
    },
    {
        "OfferId": "yml-without-enable-auto-discounts-flag",
        "Price": 2,
        "EnableAutoDiscounts": "1",
    },
    {
        "OfferId": "yml-with-invalid-enable-auto-discounts-flag",
        "Price": 3,
        "EnableAutoDiscounts": "1",
    },
    {
        "OfferId": "yml-with-enable-auto-discounts-flag-yes",
        "Price": 4,
        "EnableAutoDiscounts": "1",
    },
]
)wrap");


TEST(BlueYmlParser, EnableAutoDiscounts) {
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::YML);
    feedInfo.EnableAutoDiscounts = true;
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [&feedInfo](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            const auto enableAutoDiscounts =
                    item->DataCampOffer.price().enable_auto_discounts().has_flag()
                        ? item->DataCampOffer.price().enable_auto_discounts().flag()
                        : feedInfo.EnableAutoDiscounts;
            result["EnableAutoDiscounts"] = ToString(enableAutoDiscounts);
            return result;
        },
        feedInfo
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
