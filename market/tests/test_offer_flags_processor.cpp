#include "processor_test_runner.h"
#include "test_utils.h"

#include <market/idx/feeds/qparser/lib/flags/disabled.h>
#include <market/idx/feeds/qparser/lib/parser_context.h>
#include <market/idx/feeds/qparser/lib/processors/offer_flags_processor.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/common/env.h>

using namespace NMarket;

namespace {

class TestOfferFlagsProcessor : public TestProcessor<TOfferFlagsProcessor> {
public:
    NMarket::TFeedInfo FeedInfo;
    NMarket::TFeedShopInfo FeedShopInfo;
    ui64 Seconds = 1000;
    Market::DataCamp::Offer DataCampOffer;
public:
    IWriter::TMsgPtr DoProcess(ECpaOption cpa = ECpaOption::NOTSET) {
        return Process(
            FeedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(FeedInfo)
                    .WithCpa(cpa)
                    .WithDataCampOffer(std::move(DataCampOffer))
            )
        );
    }
private:
    void SetUp() override {
        TestProcessor::SetUp();
        FeedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);
        TParserContext ctx(&FeedInfo, &FeedShopInfo, nullptr);
        FeedInfo.Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(Seconds);
    }
};

} // anonymous namespace

TEST_F(TestOfferFlagsProcessor, EnableAutoDiscountsUseFeedShopInfo) {
    FeedShopInfo.EnableAutoDiscounts = true;

    auto offer = DoProcess();

    auto enableAutoDiscount = offer->DataCampOffer.price().enable_auto_discounts();
    EXPECT_TRUE(enableAutoDiscount.meta().has_timestamp());
    EXPECT_EQ(enableAutoDiscount.meta().timestamp().seconds(), Seconds);
    EXPECT_TRUE(enableAutoDiscount.flag());
}

TEST_F(TestOfferFlagsProcessor, EnableAutoDiscountsPreferOfferFlag) {
    DataCampOffer.mutable_price()->mutable_enable_auto_discounts()->set_flag(false);
    DataCampOffer.mutable_price()->mutable_enable_auto_discounts()->mutable_meta()->mutable_timestamp()->set_seconds(Seconds);
    FeedShopInfo.EnableAutoDiscounts = true;

    auto offer = DoProcess();

    EXPECT_TRUE(offer->DataCampOffer.price().enable_auto_discounts().meta().has_timestamp());
    EXPECT_EQ(offer->DataCampOffer.price().enable_auto_discounts().meta().timestamp().seconds(), Seconds);
    EXPECT_FALSE(offer->DataCampOffer.price().enable_auto_discounts().flag());
}

TEST_F(TestOfferFlagsProcessor, AdultUseFeedShopInfoFlag) {
    // adult flag in offer is undefined
    FeedShopInfo.Adult = true;

    auto offer = DoProcess();

    auto flag = offer->DataCampOffer.content().partner().original().adult();
    EXPECT_TRUE(flag.meta().has_timestamp());
    EXPECT_EQ(flag.meta().timestamp().seconds(), Seconds);
    EXPECT_TRUE(flag.flag());
}

TEST_F(TestOfferFlagsProcessor, AdultUseFeedShopInfoFlagFalse) {
    // adult flag in offer is undefined
    FeedShopInfo.Adult = false;

    auto offer = DoProcess();

    auto flag = offer->DataCampOffer.content().partner().original().adult();
    EXPECT_TRUE(flag.meta().has_timestamp());
    EXPECT_EQ(flag.meta().timestamp().seconds(), Seconds);
    EXPECT_FALSE(flag.flag());
}

TEST_F(TestOfferFlagsProcessor, AdultUseOfferFlag) {
    constexpr ui64 secondsOffer = 601;

    auto flagMutable = DataCampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_adult();
    flagMutable->set_flag(false);
    flagMutable->mutable_meta()->mutable_timestamp()->set_seconds(secondsOffer);

    FeedShopInfo.Adult = true;

    auto offer = DoProcess();

    auto flag = offer->DataCampOffer.content().partner().original().adult();
    EXPECT_TRUE(flag.meta().has_timestamp());
    EXPECT_EQ(flag.meta().timestamp().seconds(), secondsOffer);
    EXPECT_FALSE(flag.flag());
}

TEST_F(TestOfferFlagsProcessor, WithoutDelivery) {
    /* поля нет ни в секции shop, ни в оффере => не передаем ничего */
    auto offer = DoProcess();
    EXPECT_FALSE(offer->DataCampOffer.delivery().partner().original().has_delivery());
}

TEST_F(TestOfferFlagsProcessor, DeliveryFromOffer) {
    /* поля нет в секции shop, но есть в оффере => передаем значение из оффера */
    constexpr ui64 secondsOffer = 601;

    auto flagMutable = DataCampOffer.mutable_delivery()->mutable_partner()->mutable_original()->mutable_delivery();
    flagMutable->set_flag(true);
    flagMutable->mutable_meta()->mutable_timestamp()->set_seconds(secondsOffer);

    auto offer = DoProcess();

    auto flag = offer->DataCampOffer.delivery().partner().original().delivery();
    EXPECT_TRUE(flag.meta().has_timestamp());
    EXPECT_EQ(flag.meta().timestamp().seconds(), secondsOffer);
    EXPECT_TRUE(flag.flag());
}

TEST_F(TestOfferFlagsProcessor, DeliveryFromFeedShopInfo) {
    /* поле есть в секции shop, но нет в оффере => передаем из секции shop */
    FeedShopInfo.Delivery = true;

    auto offer = DoProcess();

    auto flag = offer->DataCampOffer.delivery().partner().original().delivery();
    EXPECT_TRUE(flag.meta().has_timestamp());
    EXPECT_EQ(flag.meta().timestamp().seconds(), Seconds);
    EXPECT_TRUE(flag.flag());
}

TEST_F(TestOfferFlagsProcessor, PreferDeliveryFromOffer) {
    /* поля есть и в секции shop, и в оффере => передаем из оффера */
    constexpr ui64 secondsOffer = 601;

    auto flagMutable = DataCampOffer.mutable_delivery()->mutable_partner()->mutable_original()->mutable_delivery();
    flagMutable->set_flag(false);
    flagMutable->mutable_meta()->mutable_timestamp()->set_seconds(secondsOffer);

    FeedShopInfo.Delivery = true;

    auto offer = DoProcess();

    auto flag = offer->DataCampOffer.delivery().partner().original().delivery();
    EXPECT_TRUE(flag.meta().has_timestamp());
    EXPECT_EQ(flag.meta().timestamp().seconds(), secondsOffer);
    EXPECT_FALSE(flag.flag());
}

TEST_F(TestOfferFlagsProcessor, PreferDeliveryFromShopInfo) {
    /* поле есть в секции shop, в оффере поле пустое (удаление) => передаем из секции shop */
    constexpr ui64 secondsOffer = 601;

    auto flagMutable = DataCampOffer.mutable_delivery()->mutable_partner()->mutable_original()->mutable_delivery();
    flagMutable->mutable_meta()->mutable_timestamp()->set_seconds(secondsOffer);

    FeedShopInfo.Delivery = true;

    auto offer = DoProcess();

    auto flag = offer->DataCampOffer.delivery().partner().original().delivery();
    EXPECT_TRUE(flag.meta().has_timestamp());
    EXPECT_EQ(flag.meta().timestamp().seconds(), Seconds);
    EXPECT_TRUE(flag.flag());
}

TEST_F(TestOfferFlagsProcessor, RemoveDelivery) {
    /* поля нет в секции shop, в оффере поле пустое (удаление) => удаление */
    constexpr ui64 secondsOffer = 601;

    auto flagMutable = DataCampOffer.mutable_delivery()->mutable_partner()->mutable_original()->mutable_delivery();
    flagMutable->mutable_meta()->mutable_timestamp()->set_seconds(secondsOffer);

    auto offer = DoProcess();

    auto flag = offer->DataCampOffer.delivery().partner().original().delivery();
    EXPECT_TRUE(flag.meta().has_timestamp());
    EXPECT_EQ(flag.meta().timestamp().seconds(), secondsOffer);
    EXPECT_FALSE(flag.has_flag());
}

TEST_F(TestOfferFlagsProcessor, MismatchCpaCpc) {
    FeedInfo.Cpa = ECpa::REAL;
    auto offer = DoProcess(ECpaOption::DISABLED);

    EXPECT_TRUE(offer->IsIgnored());
}

TEST_F(TestOfferFlagsProcessor, MatchCpaCpc) {
    FeedInfo.Cpa = ECpa::REAL;
    auto offer = DoProcess();

    EXPECT_FALSE(offer->IsIgnored());
    EXPECT_TRUE(offer->DataCampOffer.status().original_cpa().flag());
}

TEST_F(TestOfferFlagsProcessor, MismatchCpaCpcPartnerCpaUnknown) {
    FeedInfo.Cpa = ECpa::UNKNOWN;
    auto offer = DoProcess(ECpaOption::ENABLED);

    EXPECT_FALSE(offer->IsIgnored());
    EXPECT_TRUE(offer->DataCampOffer.status().original_cpa().flag());
}

TEST_F(TestOfferFlagsProcessor, AllowCpaOfferForADV_FeatureOff) {
    FeedInfo.Cpa = ECpa::NO;
    auto offer = DoProcess(ECpaOption::ENABLED);

    EXPECT_TRUE(offer->IsIgnored());
}

TEST_F(TestOfferFlagsProcessor, AllowCpaOfferForADV_FeatureOn) {
    FeedInfo.AcceptCpaOffersForADV = true;
    FeedInfo.Cpa = ECpa::NO;
    auto offer = DoProcess(ECpaOption::ENABLED);

    EXPECT_FALSE(offer->IsIgnored());
    // Спорный момент. Ставим origina_cpa как принес партнер, но в actual_cpa для ADV магазина получится false
    EXPECT_TRUE(offer->DataCampOffer.status().original_cpa().flag());
}
