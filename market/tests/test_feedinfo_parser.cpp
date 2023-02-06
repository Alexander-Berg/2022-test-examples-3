#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <market/idx/feeds/qparser/inc/feed_info.h>

#include <util/folder/path.h>
#include <util/generic/maybe.h>

using namespace NMarket;

namespace {

const TString FEED_INFO_JSON_1 = TString(R"wrap(
{
    "shop_id": 10264281,
    "market_color": "blue",
    "ignore_stocks": false,
    "is_discounts_enabled": true,
    "direct_shipping": true,
    "session_id": 1591070177,
    "ff_program": "REAL",
    "feed_type": "XLS",
    "feed_id": 0,
    "enable_auto_discounts": true,
    "vat": 6,
    "xls2csv_output": "X-MARKET-TEMPLATE: SUPPLIER",
    "cpa": "REAL",
    "shop_disabled_since_ts": 100
}
)wrap");

const TString FEED_INFO_JSON_CPA_NO = TString(R"wrap(
{
    "shop_id": 1234,
    "market_color": "white",
    "ignore_stocks": true,
    "is_discounts_enabled": false,
    "direct_shipping": false,
    "session_id": 5677,
    "ff_program": "REAL",
    "feed_type": "YML",
    "feed_id": 0,
    "enable_auto_discounts": false,
    "cpa": "NO"
}
)wrap");

const TString FEED_INFO_JSON_DEFAULT = TString(R"wrap(
{
    "shop_id": 1234,
    "market_color": "white",
    "ignore_stocks": true,
    "is_discounts_enabled": false,
    "direct_shipping": false,
    "session_id": 5677,
    "ff_program": "REAL",
    "feed_type": "YML",
    "feed_id": 0,
    "enable_auto_discounts": false
}
)wrap");

const TString FEED_INFO_JSON_BASIC_ASSORTMENT_FEED = TString(R"wrap(
{
    "business_id": 1234,
    "feed_id": 1011,
    "session_id": 5677,
    "feed_type": "YML"
}
)wrap");
}


TEST(FeedInfo, Parse) {
    TStringStream in(FEED_INFO_JSON_1);
    const auto parsedFeedInfo = ParseFeedInfo(&in, Nothing());
    ASSERT_EQ(parsedFeedInfo.ShopId, 10264281);
    ASSERT_EQ(parsedFeedInfo.MarketColor, EMarketColor::MC_BLUE);
    ASSERT_TRUE(parsedFeedInfo.IsDiscountsEnabled);
    ASSERT_EQ(parsedFeedInfo.SessionId, 1591070177);
    ASSERT_EQ(parsedFeedInfo.FeedType, EFeedType::XLS);
    ASSERT_EQ(parsedFeedInfo.FeedId, 0);
    ASSERT_TRUE(parsedFeedInfo.EnableAutoDiscounts);
    ASSERT_EQ(parsedFeedInfo.Vat.GetRef(), 6);
    ASSERT_STREQ(parsedFeedInfo.XlS2CsvOutput.GetRef(), TString("X-MARKET-TEMPLATE: SUPPLIER"));
    ASSERT_EQ(parsedFeedInfo.Cpa, ECpa::REAL);
    ASSERT_EQ(parsedFeedInfo.ShopDisabledSinceTimestamp.seconds(), 100);
}

TEST(FeedInfo, Parse_ShouldFail_IfUndefinedFeedId) {
    TStringStream in("{ \"shop_id\": 12345 }");
    ASSERT_THROW(ParseFeedInfo(&in, Nothing()), TUndefinedFeedInfoOption);
}

TEST(FeedInfo, Parse_WhenCpaAsInt) {
    TStringStream in(FEED_INFO_JSON_CPA_NO);
    const auto parsedFeedInfo = ParseFeedInfo(&in, Nothing());
    ASSERT_EQ(parsedFeedInfo.ShopId, 1234);
    ASSERT_EQ(parsedFeedInfo.MarketColor, EMarketColor::MC_WHITE);
    ASSERT_FALSE(parsedFeedInfo.IsDiscountsEnabled);
    ASSERT_EQ(parsedFeedInfo.SessionId, 5677);
    ASSERT_EQ(parsedFeedInfo.FeedType, EFeedType::YML);
    ASSERT_EQ(parsedFeedInfo.FeedId, 0);
    ASSERT_FALSE(parsedFeedInfo.EnableAutoDiscounts);
    ASSERT_EQ(parsedFeedInfo.Cpa, ECpa::NO);
}

TEST(FeedInfo, Parse_WhenCpaIsNotDefined) {
    TStringStream in(FEED_INFO_JSON_DEFAULT);
    const auto parsedFeedInfo = ParseFeedInfo(&in, Nothing());
    ASSERT_EQ(parsedFeedInfo.ShopId, 1234);
    ASSERT_EQ(parsedFeedInfo.MarketColor, EMarketColor::MC_WHITE);
    ASSERT_FALSE(parsedFeedInfo.IsDiscountsEnabled);
    ASSERT_EQ(parsedFeedInfo.SessionId, 5677);
    ASSERT_EQ(parsedFeedInfo.FeedType, EFeedType::YML);
    ASSERT_EQ(parsedFeedInfo.FeedId, 0);
    ASSERT_FALSE(parsedFeedInfo.EnableAutoDiscounts);
    ASSERT_EQ(parsedFeedInfo.Cpa, ECpa::UNKNOWN);
    ASSERT_EQ(parsedFeedInfo.ShopDisabledSinceTimestamp.seconds(), 0);
}

TEST(FeedInfo, Parse_BasicAssortment) {
    TStringStream in(FEED_INFO_JSON_BASIC_ASSORTMENT_FEED);
    const auto parsedFeedInfo = ParseFeedInfo(&in, Nothing());
    ASSERT_EQ(parsedFeedInfo.BusinessId, 1234);
    ASSERT_EQ(parsedFeedInfo.ShopId, 0);
    ASSERT_EQ(parsedFeedInfo.FeedId, 1011);
    ASSERT_EQ(parsedFeedInfo.MarketColor, EMarketColor::MC_UNDEFINED);
    ASSERT_EQ(parsedFeedInfo.OfferColor, EMarketColor::MC_UNDEFINED);
    ASSERT_EQ(parsedFeedInfo.SessionId, 5677);
    ASSERT_EQ(parsedFeedInfo.FeedType, EFeedType::YML);
}
