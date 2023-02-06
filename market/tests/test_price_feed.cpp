#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/common/csv/feed_parser.h>
#include <market/idx/feeds/qparser/src/feed_parsers/common/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;price;dynamicpricing-type;dynamicpricing-unit;dynamicpricing-value;category
    offer-with-empty-category;100;;;;
    offer-with-category;200;;;;some category
    offer-with-dynamicpricing;100;MINIMAL;%;15.34;
)wrap");

static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog>
    <shop>
    <offers>
        <offer>
            <shop-sku>offer-with-empty-category</shop-sku>
            <price>100</price>
            <category></category>
        </offer>
        <offer>
            <shop-sku>offer-with-category</shop-sku>
            <price>200</price>
            <category>some category</category>
        </offer>
    </offers>
    </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON_YML = TString(R"wrap(
[
    {
        "OfferId": "offer-with-empty-category",
        "Price": 100,
    },
    {
        "OfferId": "offer-with-category",
        "Price": 200,
    },
]
)wrap");

static const TString EXPECTED_JSON_CSV = TString(R"wrap(
[
    {
        "OfferId": "offer-with-empty-category",
        "Price": 100,
    },
    {
        "OfferId": "offer-with-category",
        "Price": 200,
    },
    {
        "OfferId": "offer-with-dynamicpricing",
        "Price": 100,
        "DynamicPricingType": 2,
        "DynamicPricingUnit": "%",
        "DynamicPricingValue": 15.34,
    }
]
)wrap");

auto processOffer = [](const TQueueItem& item) {
    NSc::TValue result;
    result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

    if (item->RawPrice) {
        result["Price"] = *item->RawPrice;
    }
    if (item->DataCampOffer.content().partner().original().has_category()) {
        result["Category"] = item->DataCampOffer.content().partner().original().category().id();
    }
    if (item->RawOldPrice) {
        result["OldPrice"] = *item->RawOldPrice;
    }
    if (item->RawDynamicPricing.ThresholdValue) {
        result["DynamicPricingValue"] = *item->RawDynamicPricing.ThresholdValue;
    }
    if (item->RawDynamicPricing.ThresholdUnit) {
        result["DynamicPricingUnit"] = (*item->RawDynamicPricing.ThresholdUnit == TDynamicPricing::PERCENT) ? "%" : "fixed";
    }
    if (item->DataCampOffer.price().dynamic_pricing().has_type()) {
        result["DynamicPricingType"] = int(item->DataCampOffer.price().dynamic_pricing().type());
    }
    return result;
};


TEST(TestCsvPriceFeed, NotWriteContentFieldErrors) {
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    feedInfo.FeedId = 12345;
    feedInfo.CheckFeedMode = true;

    const auto [actual, checkResult] = RunFeedParserWithCheckFeed<NMarket::NCommon::TCsvPriceFeedParser>(
            INPUT_CSV,
            processOffer,
            feedInfo
    );

    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_CSV);
    ASSERT_EQ(actual, expected);

    ASSERT_EQ(checkResult.log_message().size(), 0);
}


TEST(TestYmlPriceFeed, NotWriteContentFieldErrors) {
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::YML);
    feedInfo.FeedId = 12345;
    feedInfo.CheckFeedMode = true;
    feedInfo.IgnoreExtraTags = true;

    const auto [actual, checkResult] = RunFeedParserWithCheckFeed<NMarket::NCommon::TYmlPriceFeedParser>(
            INPUT_XML,
            processOffer,
            feedInfo
    );

    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_YML);
    ASSERT_EQ(actual, expected);

    ASSERT_EQ(checkResult.log_message().size(), 0);
}
