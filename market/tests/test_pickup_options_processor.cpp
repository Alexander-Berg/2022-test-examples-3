#include "processor_test_runner.h"
#include "test_utils.h"
#include <market/idx/feeds/qparser/lib/processors/pickup_options_processor.h>
#include <library/cpp/testing/common/env.h>

#include <google/protobuf/util/time_util.h>

using namespace NMarket;

using TestPickupOptionsProcessor = TestProcessor<TPickupOptionsProcessor>;

TEST_F(TestPickupOptionsProcessor, Merge) {
    NMarket::TFeedInfo feedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);
    TOfferCarrier initialOffer(feedInfo);
    NMarket::TFeedShopInfo feedShopInfo;
    feedShopInfo.PickupOptions.push_back({300.0, 1, 2, Nothing()});
    const auto offer = Process(
        feedInfo,
        feedShopInfo,
        MakeAtomicShared<IFeedParser::TMsg>(initialOffer)
    );
    ASSERT_TRUE(offer->GetOriginalPartnerDelivery().has_pickup_options());
    ASSERT_EQ(offer->GetOriginalPartnerDelivery().pickup_options().options_size(), 1);
    const auto& option = offer->GetOriginalPartnerDelivery().pickup_options().options(0);
    ASSERT_EQ(option.GetCost(), 300.0);
    ASSERT_EQ(option.GetDaysMin(), 1);
    ASSERT_EQ(option.GetDaysMax(), 2);
}
