#include "processor_test_runner.h"
#include "test_utils.h"

#include <market/idx/feeds/qparser/lib/parser_context.h>
#include <market/idx/feeds/qparser/lib/processors/offer_condition_processor.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/common/env.h>

using namespace Market::DataCamp;
using namespace NMarket;

namespace {

class TestOfferConditionMultiProcessor : public TestProcessor<TOfferConditionProcessor> {
public:
    NMarket::TFeedInfo FeedInfo;
    NMarket::TFeedShopInfo FeedShopInfo;
    ui64 Seconds = 1000;
public:
    IWriter::TMsgPtr DoProcess(Offer offer) {
        return Process(
            FeedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(FeedInfo)
                    .WithDataCampOffer(std::move(offer))
            )
        );
    }
private:
    void SetUp() override {
        TestProcessor::SetUp();
        FeedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);
        FeedInfo.PushFeedClass = Market::DataCamp::API::FeedClass::FEED_CLASS_ASSORTMENT_BASIC_PATCH_UPDATE_SALE_TERMS_SERVICE_FULL_COMPLETE;
        TParserContext ctx(&FeedInfo, &FeedShopInfo, nullptr);
        FeedInfo.Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(Seconds);
    }
};

} // anonymous namespace

TEST_F(TestOfferConditionMultiProcessor, NotHideOfferWithCondition) {
    Offer datacampOffer;
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_condition()->set_type(
            Condition::LIKENEW);
    auto offer = DoProcess(datacampOffer);

    EXPECT_TRUE(offer->IsBasicIgnored());
    EXPECT_FALSE(offer->IsServiceIgnored());
}

TEST_F(TestOfferConditionMultiProcessor, NotHideOfferWithoutCondition) {
    Offer datacampOffer;
    auto offer = DoProcess(datacampOffer);

    EXPECT_FALSE(offer->IsBasicIgnored());
    EXPECT_FALSE(offer->IsServiceIgnored());
}
