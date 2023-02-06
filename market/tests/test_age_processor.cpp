#include "processor_test_runner.h"
#include "test_utils.h"
#include <market/idx/feeds/qparser/lib/processors/age_processor.h>
#include <library/cpp/testing/common/env.h>

#include <google/protobuf/util/time_util.h>

using namespace NMarket;

using TestAgeProcessor = TestProcessor<TOfferAgeProcessor>;

TEST_F(TestAgeProcessor, KnownUnit) {
    NMarket::TFeedInfo feedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);

    auto initialWithMonth = TOfferCarrier(feedInfo)
        .WithRawAgeUnit("month");
    initialWithMonth.GetMutableOriginalSpecification()->mutable_age()->set_value(10);

    auto initialWithYear = TOfferCarrier(feedInfo)
        .WithRawAgeUnit("year");
    initialWithYear.GetMutableOriginalSpecification()->mutable_age()->set_value(10);

    NMarket::TFeedShopInfo feedShopInfo;
    const auto offerMonth = Process(feedInfo, feedShopInfo, MakeAtomicShared<IFeedParser::TMsg>(initialWithMonth));
    const auto offerYear = Process(feedInfo, feedShopInfo, MakeAtomicShared<IFeedParser::TMsg>(initialWithYear));

    ASSERT_TRUE(offerMonth->GetOriginalSpecification().age().has_unit());
    ASSERT_TRUE(offerMonth->GetOriginalSpecification().age().unit() == Market::DataCamp::Age::MONTH);

    ASSERT_TRUE(offerYear->GetOriginalSpecification().age().has_unit());
    ASSERT_TRUE(offerYear->GetOriginalSpecification().age().unit() == Market::DataCamp::Age::YEAR);
}

TEST_F(TestAgeProcessor, KnownUnitEmptyAge) {
    NMarket::TFeedInfo feedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);

    auto initialWithMonth = TOfferCarrier(feedInfo)
        .WithRawAgeUnit("month");

    auto initialWithYear = TOfferCarrier(feedInfo)
        .WithRawAgeUnit("year");

    NMarket::TFeedShopInfo feedShopInfo;
    const auto offerMonth = Process(feedInfo, feedShopInfo, MakeAtomicShared<IFeedParser::TMsg>(initialWithMonth));
    const auto offerYear = Process(feedInfo, feedShopInfo, MakeAtomicShared<IFeedParser::TMsg>(initialWithYear));

    ASSERT_FALSE(offerMonth->GetOriginalSpecification().age().has_unit());
    ASSERT_FALSE(offerYear->GetOriginalSpecification().age().has_unit());
}

TEST_F(TestAgeProcessor, UnknownOrEmptyUnitEmptyAge) {
    NMarket::TFeedInfo feedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);

    auto initialWithUnknown = TOfferCarrier(feedInfo)
        .WithRawAgeUnit("unknown");

    auto initialWithEmpty = TOfferCarrier(feedInfo);

    NMarket::TFeedShopInfo feedShopInfo;

    const auto offerUnknownUnit = Process(feedInfo, feedShopInfo, MakeAtomicShared<IFeedParser::TMsg>(initialWithUnknown));
    const auto offerEmptyUnit = Process(feedInfo, feedShopInfo, MakeAtomicShared<IFeedParser::TMsg>(initialWithEmpty));

    ASSERT_FALSE(offerUnknownUnit->GetOriginalSpecification().has_age());
    ASSERT_FALSE(offerEmptyUnit->GetOriginalSpecification().has_age());
}
