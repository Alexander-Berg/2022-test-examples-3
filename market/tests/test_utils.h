#pragma once

#include <market/idx/feeds/qparser/inc/feed_info.h>


NMarket::TFeedInfo GetDefaultBlueFeedInfo(NMarket::EFeedType feedType = NMarket::EFeedType::YML);

NMarket::TFeedInfo GetDefaultWhiteFeedInfo(NMarket::EFeedType feedType = NMarket::EFeedType::YML);

NMarket::TFeedInfo GetDefaultYandexCustomFeedInfo(NMarket::EFeedType feedType = NMarket::EFeedType::CSV);

NMarket::TFeedInfo GetDefaultGoogleCustomFeedInfo(NMarket::EFeedType feedType = NMarket::EFeedType::CSV);

NMarket::TFeedInfo GetDefaultGoogleFlightFeedInfo(NMarket::EFeedType feedType = NMarket::EFeedType::CSV);

NMarket::TFeedInfo GetDefaultGoogleTravelFeedInfo(NMarket::EFeedType feedType = NMarket::EFeedType::CSV);

NMarket::TFeedInfo GetDefaultGoogleHotelFeedInfo(NMarket::EFeedType feedType = NMarket::EFeedType::CSV);
