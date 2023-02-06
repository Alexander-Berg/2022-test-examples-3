#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/common/csv/feed_parser.h>
#include <market/idx/feeds/qparser/src/feed_parsers/common/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;category;price;delivery
    offer-without-category;;100;true
    offer-with-category;tables;200;false
)wrap");

static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog>
    <shop>
    <offers>
        <offer>
            <shop-sku>offer-without-category</shop-sku>
            <category></category>
            <price>100</price>
            <delivery>true</delivery>
        </offer>
        <offer>
            <shop-sku>offer-with-category</shop-sku>
            <category>tables</category>
            <price>200</price>
            <delivery>false</delivery>
        </offer>
    </offers>
    </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "offer-without-category",
        "Price": 100,
        "Delivery": 1,
    },
    {
        "OfferId": "offer-with-category",
        "Price": 200,
        "Delivery": 0,
    },
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
    if (item->GetOriginalPartnerDelivery().has_delivery()) {
        result["Delivery"] = item->GetOriginalPartnerDelivery().delivery().flag();
    }
    return result;
};


TEST(TestCsvSaleTermsFeed, NotWriteContentFieldErrors) {
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    feedInfo.FeedId = 12345;
    feedInfo.CheckFeedMode = true;

    const auto [actual, checkResult] = RunFeedParserWithCheckFeed<NMarket::NCommon::TCsvSaleTermsFeedParser>(
            INPUT_CSV,
            processOffer,
            feedInfo
    );

    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);

    ASSERT_EQ(checkResult.log_message().size(), 0);
}


TEST(TestYmlSaleTermsFeed, NotWriteContentFieldErrors) {
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::YML);
    feedInfo.FeedId = 12345;
    feedInfo.CheckFeedMode = true;
    feedInfo.IgnoreExtraTags = true;

    const auto [actual, checkResult] = RunFeedParserWithCheckFeed<NMarket::NCommon::TYmlSaleTermsFeedParser>(
            INPUT_XML,
            processOffer,
            feedInfo
    );

    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);

    ASSERT_EQ(checkResult.log_message().size(), 0);
}
