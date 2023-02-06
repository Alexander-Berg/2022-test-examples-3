#include "test_utils.h"

#include <market/idx/feeds/qparser/inc/feed_info.h>
#include <market/library/partners/colors.h>

#include <google/protobuf/util/time_util.h>


NMarket::TFeedInfo GetDefaultBlueFeedInfo(NMarket::EFeedType feedType /*= NMarket::EFeedType::YML*/) {
    NMarket::TFeedInfo feedInfo;
    feedInfo.ShopId = 1000;
    feedInfo.FeedId = 1500;
    feedInfo.FulfillmentFeedId = 1600;
    feedInfo.FeedType = feedType;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_BLUE;
    feedInfo.SessionId = 20190701;
    feedInfo.WarehouseId = 145;
    feedInfo.IsDiscountsEnabled = false;
    feedInfo.IsRegularParsing = false;
    feedInfo.HomeRegion = 225;
    feedInfo.Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1000);

    return feedInfo;
}


NMarket::TFeedInfo GetDefaultWhiteFeedInfo(NMarket::EFeedType feedType /*= NMarket::EFeedType::YML*/) {
    NMarket::TFeedInfo feedInfo;
    feedInfo.ShopId = 1000;
    feedInfo.FeedId = 1500;
    feedInfo.FulfillmentFeedId = 1600;
    feedInfo.FeedType = feedType;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_WHITE;
    feedInfo.SessionId = 20190701;
    feedInfo.IsDiscountsEnabled = false;
    feedInfo.IsRegularParsing = false;
    feedInfo.HomeRegion = 225;
    feedInfo.Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1000);

    return feedInfo;
}

NMarket::TFeedInfo GetDefaultYandexCustomFeedInfo(NMarket::EFeedType feedType /*= NMarket::EFeedType::CSV*/) {
    NMarket::TFeedInfo feedInfo;
    feedInfo.FeedType = feedType;
    feedInfo.HomeRegion = 225;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_UNDEFINED;
    return feedInfo;
}

NMarket::TFeedInfo GetDefaultGoogleCustomFeedInfo(NMarket::EFeedType feedType /*= NMarket::EFeedType::CSV*/) {
    NMarket::TFeedInfo feedInfo;
    feedInfo.FeedType = feedType;
    feedInfo.HomeRegion = 225;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_UNDEFINED;
    return feedInfo;
}

NMarket::TFeedInfo GetDefaultGoogleFlightFeedInfo(NMarket::EFeedType feedType /*= NMarket::EFeedType::CSV*/) {
    NMarket::TFeedInfo feedInfo;
    feedInfo.FeedType = feedType;
    feedInfo.HomeRegion = 225;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_UNDEFINED;

    return feedInfo;
}

NMarket::TFeedInfo GetDefaultGoogleTravelFeedInfo(NMarket::EFeedType feedType /*= NMarket::EFeedType::CSV*/) {
    NMarket::TFeedInfo feedInfo;
    feedInfo.FeedType = feedType;
    feedInfo.HomeRegion = 225;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_UNDEFINED;

    return feedInfo;
}

NMarket::TFeedInfo GetDefaultGoogleHotelFeedInfo(NMarket::EFeedType feedType /*= NMarket::EFeedType::CSV*/) {
    NMarket::TFeedInfo feedInfo;
    feedInfo.FeedType = feedType;
    feedInfo.HomeRegion = 225;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_UNDEFINED;

    return feedInfo;
}
