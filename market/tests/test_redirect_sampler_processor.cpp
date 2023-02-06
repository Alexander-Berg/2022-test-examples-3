#include "processor_test_runner.h"
#include "test_utils.h"

#include <market/idx/feeds/qparser/lib/flags/disabled.h>
#include <market/idx/feeds/qparser/lib/parser_context.h>
#include <market/idx/feeds/qparser/lib/processors/redirect_sampler_processor.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/common/env.h>

using namespace NMarket;

namespace {

    class TestRedirectSamplerProcessor : public TestProcessor<TRedirectSamplerProcessor> {
    public:
        NMarket::TFeedInfo FeedInfo;
        NMarket::TFeedShopInfo FeedShopInfo;
        Market::DataCamp::Offer DataCampOffer;
        ui64 Seconds = 1000;
    public:
        IWriter::TMsgPtr DoProcess() {
            DataCampOffer.clear_status();
            return Process(
                    FeedInfo,
                    FeedShopInfo,
                    MakeAtomicShared<IFeedParser::TMsg>(
                            TOfferCarrier(FeedInfo)
                                    .WithDataCampOffer(DataCampOffer)
                    )
            );
        }
    private:
        void SetUp() override {
            TestProcessor::SetUp();
            DataCampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_url()->set_value("http://ololo.my/url1");
            FeedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);
            TParserContext ctx(&FeedInfo, &FeedShopInfo, nullptr);
            FeedInfo.Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(Seconds);
        }
    };

} // anonymous namespace

TEST_F(TestRedirectSamplerProcessor, TestSampled) {
    IWriter::TMsgPtr offer;
    ui32 sampleCount = TRedirectSamplerProcessor().GetByDomainOffersCount();
    for (ui32 i = 0; i < sampleCount; i++) {
        offer = DoProcess();
        EXPECT_TRUE(offer->DataCampOffer.status().has_is_sampled_for_redirect());
        EXPECT_TRUE(offer->DataCampOffer.status().is_sampled_for_redirect().Getflag());
    };
    offer = DoProcess();
    EXPECT_FALSE(
            offer->DataCampOffer.has_status() &&
            offer->DataCampOffer.status().has_is_sampled_for_redirect() &&
            offer->DataCampOffer.status().is_sampled_for_redirect().Getflag()
    );
}
