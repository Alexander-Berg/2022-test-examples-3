#include "processor_test_runner.h"
#include "test_utils.h"

#include <market/idx/feeds/qparser/lib/parser_context.h>
#include <market/idx/feeds/qparser/lib/price_calculator.h>
#include <market/idx/feeds/qparser/lib/processors/price_processor.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/common/env.h>

static const int UsdRate = 29'2859;
static const int EurRate = 39'6795;
using namespace NMarket;

class TestPriceProcessor : public TestProcessor<TPriceProcessor> {
public:
    NMarket::TFeedInfo FeedInfo;
    NMarket::TFeedShopInfo FeedShopInfo;
private:
    void SetUp() override {
        TestProcessor::SetUp();
        FeedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);
        TParserContext ctx(&FeedInfo, &FeedShopInfo, nullptr);
        TPriceCalculator& priceCalculator = FeedShopInfo.PriceCalculator;
        priceCalculator.Clear();
        if (!CEXCHANGE.IsLoaded()) {
            CEXCHANGE.Load(SRC_("data/currency_rates.xml"));
        }
        priceCalculator.SetBank("CBRF");
        priceCalculator.AddCurrency(TCurrencyRecord{"RUR", "1"}, &ctx);
        priceCalculator.AddCurrency(TCurrencyRecord{"UAH", ""}, &ctx);
        priceCalculator.AddCurrency(TCurrencyRecord{"EUR", "1"}, &ctx);
    }
};


TEST_F(TestPriceProcessor, Price) {
    {
        TFeedInfo feedInfo = {.MarketColor = EMarketColor::MC_BLUE};
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithRawPrice(1234)
            )
        );
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_price().price(), 1234'0000000);
        ASSERT_EQ(offer->RurPrice, 1234);
    }
    {
        // проверка округления цены до целого
        TFeedInfo feedInfo = {.MarketColor = EMarketColor::MC_BLUE};
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithRawPrice(1234.56)
            )
        );
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_price().price(), 1235'0000000);
        ASSERT_EQ(offer->RurPrice, 1235);
    }
    {
        // В долларах
        TFeedInfo feedInfo = {.MarketColor = EMarketColor::MC_WHITE};
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithCurrency("USD")
                    .WithRawPrice(10.01)
            )
        );
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_price().price(), 10'01'00000L);
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_price().id(), "USD");
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_price().rate(), "CBRF");
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_price().ref_id(), "RUR");
        ASSERT_EQ(offer->RurPrice, 10.01 * UsdRate / 1'0000);
    }
    {
        // В евро: не смотря на rate=1, в качестве rate берем CBRF
        TFeedInfo feedInfo = {.MarketColor = EMarketColor::MC_WHITE};
        auto offer = Process(
                feedInfo,
                FeedShopInfo,
                MakeAtomicShared<IFeedParser::TMsg>(
                        TOfferCarrier(feedInfo)
                            .WithCurrency("EUR")
                            .WithRawPrice(10.01)
                )
        );
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_price().price(), 10'01'00000L);
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_price().id(), "EUR");
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_price().rate(), "CBRF");
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_price().ref_id(), "RUR");
        ASSERT_EQ(offer->RurPrice, 10.01 * EurRate / 1'0000);
    }
    {
        // Неизвестная валюта сохраняется, как есть
        TFeedInfo feedInfo = {.MarketColor = EMarketColor::MC_WHITE};
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithCurrency("JPY")
                    .WithRawPrice(10.01)
            )
        );
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_price().price(), 10'01'00000L);
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_price().id(), "JPY");
    }
}


TEST_F(TestPriceProcessor, OldPrice) {
    {
        // проверяем, что oldprice игнорируется при IsDiscountsEnabled = false
        TFeedInfo feedInfo = {
            .MarketColor = EMarketColor::MC_BLUE,
            .IsDiscountsEnabled = false,
        };
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithRawPrice(100)
                    .WithRawOldPrice(150)
            )
        );

        ASSERT_FALSE(offer->DataCampOffer.price().basic().has_binary_oldprice());
    }

    {
        TFeedInfo feedInfo = {
            .MarketColor = EMarketColor::MC_BLUE,
            .IsDiscountsEnabled = true,
        };
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithRawPrice(100)
                    .WithRawOldPrice(150.99)
            )
        );

        ASSERT_TRUE(offer->DataCampOffer.price().basic().has_binary_oldprice());
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_oldprice().price(), 151'0000000);
    }
    {
        TFeedInfo feedInfo = {
            .MarketColor = EMarketColor::MC_WHITE,
            .IsDiscountsEnabled = true,
        };
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithCurrency("USD")
                    .WithRawPrice(100)
                    .WithRawOldPrice(150.99)
            )
        );

        ASSERT_TRUE(offer->DataCampOffer.price().basic().has_binary_oldprice());
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_oldprice().price(), 150'990'0000L);
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_oldprice().id(), "USD");
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_oldprice().ref_id(), "RUR");
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_oldprice().rate(), "CBRF");
    }

    {
        // игнорируем oldprice, если он слишком большой
        TFeedInfo feedInfo = {
            .IsDiscountsEnabled = true
        };
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithRawPrice(100)
                    .WithRawOldPrice(100000)
            )
        );

        ASSERT_FALSE(offer->DataCampOffer.price().basic().has_binary_oldprice());
        // TODO(isabirzyanov) сообщение в process log
    }
    {
        TFeedInfo feedInfo = {
                .MarketColor = EMarketColor::MC_BLUE,
                .IsDiscountsEnabled = true,
                .CheckFeedMode = true,
        };
        auto offer = Process(
                feedInfo,
                FeedShopInfo,
                MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithRawOldPrice(150.99)
                )
        );

        ASSERT_TRUE(offer->DataCampOffer.price().basic().has_binary_oldprice());
        ASSERT_EQ(offer->DataCampOffer.price().basic().binary_oldprice().price(), 151'0000000);
    }
    {
        TVector<EMarketColor> marketColors{EMarketColor::MC_WHITE, EMarketColor::MC_BLUE, EMarketColor::MC_DIRECT};
        TVector<ui32> discountPercentages{0, 1, 5, 50, 75, 95, 99, 100};
        for (auto marketColor: marketColors) {
            TFeedInfo feedInfo = {
                .MarketColor = marketColor,
                .IsDiscountsEnabled = true,
                .IsDsbs = (marketColor == EMarketColor::MC_WHITE),
                .CheckFeedMode = false /* check feed mode forces filling binary_oldprice */
            };
            for (ui32 discountPercent: discountPercentages) {
                ui32 oldPrice = 1'000'000;
                ui32 price = oldPrice * (100 - discountPercent) / 100;
                auto offer = Process(
                    feedInfo,
                    FeedShopInfo,
                    MakeAtomicShared<IFeedParser::TMsg>(
                        TOfferCarrier(feedInfo)
                            .WithRawPrice(price)
                            .WithRawOldPrice(oldPrice)
                    )
                );

                bool expectedResult;
                if (marketColor == EMarketColor::MC_WHITE || marketColor == EMarketColor::MC_BLUE) {
                    expectedResult = (5 <= discountPercent && discountPercent <= 75);
                } else {
                    expectedResult = (5 <= discountPercent && discountPercent <= 95);
                }
                ASSERT_EQ(expectedResult, offer->DataCampOffer.price().basic().has_binary_oldprice());
            }
        }
    }
}

TEST_F(TestPriceProcessor, HugePriceUnderLimit) {
    TFeedInfo feedInfo = {.MarketColor = EMarketColor::MC_WHITE};
    auto offer = Process(
        feedInfo,
        FeedShopInfo,
        MakeAtomicShared<IFeedParser::TMsg>(
            TOfferCarrier(feedInfo)
                .WithRawPrice(99000889)
        )
    );
    ASSERT_EQ(offer->DataCampOffer.price().basic().binary_price().price(), 99'000'889'0000000L);
    ASSERT_EQ(offer->RurPrice, 99000889);
}

TEST_F(TestPriceProcessor, HugePriceOverRURLimit) {
    TFeedInfo feedInfo = {.MarketColor = EMarketColor::MC_WHITE};
    auto offer = Process(
        feedInfo,
        FeedShopInfo,
        MakeAtomicShared<IFeedParser::TMsg>(
            TOfferCarrier(feedInfo)
                .WithRawPrice(101000889)
        )
    );
    // Should disable the offer and clear the price
    ASSERT_TRUE(offer->IsDisabled);
    ASSERT_FALSE(offer->DataCampOffer.price().basic().has_binary_price());
}

TEST_F(TestPriceProcessor, ValidateCurrencyIdForBlue) {
    TFeedInfo feedInfo = {.MarketColor = EMarketColor::MC_BLUE};
    feedInfo.ForceRurCurrencyForBlueMode = false;
    auto offer = Process(
        feedInfo,
        FeedShopInfo,
        MakeAtomicShared<IFeedParser::TMsg>(
            TOfferCarrier(feedInfo)
                .WithCurrency("USD")
                .WithRawPrice(105)
        )
    );
    ASSERT_FALSE(offer->DataCampOffer.price().basic().has_binary_price());
}

TEST_F(TestPriceProcessor, VatInBasicPrice) {
    {
        constexpr ui64 seconds = 567;
        Market::DataCamp::Offer dataCampOffer;
        TFeedInfo feedInfo = {
            .Vat = Market::DataCamp::Vat::VAT_10,
            .Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds),
        };
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithDataCampOffer(std::move(dataCampOffer))
            )
        );

        const auto& basicPrice = offer->DataCampOffer.price().basic();
        ASSERT_TRUE(basicPrice.meta().has_timestamp());
        EXPECT_EQ(basicPrice.meta().timestamp().seconds(), seconds);
        EXPECT_TRUE(basicPrice.vat() == Market::DataCamp::Vat::VAT_10);
    }

    {
        constexpr ui64 seconds = 567;
        TFeedInfo feedInfo = {
            .Vat = 5, // не должен проставиться
            .Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds),
        };
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
            )
        );

        ASSERT_FALSE(offer->DataCampOffer.price().original_price_fields().has_vat());
    }
}

TEST_F(TestPriceProcessor, EnableAutoDiscounts) {
    /// В TFeedInfo поле EnableAutoDiscounts это bool, так что заполнним его в явном виде
    {
        constexpr ui64 seconds = 567;
        TFeedInfo feedInfo = {
            .EnableAutoDiscounts = false,
            .Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds),
        };
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
            )
        );

        EXPECT_TRUE(offer->DataCampOffer.price().enable_auto_discounts().meta().has_timestamp());
        EXPECT_EQ(offer->DataCampOffer.price().enable_auto_discounts().meta().timestamp().seconds(), seconds);
        EXPECT_FALSE(offer->DataCampOffer.price().enable_auto_discounts().flag());
    }

    {
        constexpr ui64 seconds = 567;
        TFeedInfo feedInfo = {
            .EnableAutoDiscounts = true,
            .Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds),
        };
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
            )
        );

        EXPECT_TRUE(offer->DataCampOffer.price().enable_auto_discounts().meta().has_timestamp());
        EXPECT_EQ(offer->DataCampOffer.price().enable_auto_discounts().meta().timestamp().seconds(), seconds);
        EXPECT_TRUE(offer->DataCampOffer.price().enable_auto_discounts().flag());
    }

    {
        constexpr ui64 seconds = 567;
        Market::DataCamp::Offer dataCampOffer;
        dataCampOffer.mutable_price()->mutable_enable_auto_discounts()->set_flag(false);
        TFeedInfo feedInfo = {
            .EnableAutoDiscounts = false,
            .Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds),
        };
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithDataCampOffer(std::move(dataCampOffer))
            )
        );

        EXPECT_TRUE(offer->DataCampOffer.price().enable_auto_discounts().meta().has_timestamp());
        EXPECT_EQ(offer->DataCampOffer.price().enable_auto_discounts().meta().timestamp().seconds(), seconds);
        EXPECT_FALSE(offer->DataCampOffer.price().enable_auto_discounts().flag());
    }

    {
        constexpr ui64 seconds = 567;
        Market::DataCamp::Offer dataCampOffer;
        dataCampOffer.mutable_price()->mutable_enable_auto_discounts()->set_flag(false);
        TFeedInfo feedInfo = {
            .EnableAutoDiscounts = true,
            .Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds),
        };
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithDataCampOffer(std::move(dataCampOffer))
            )
        );

        EXPECT_TRUE(offer->DataCampOffer.price().enable_auto_discounts().meta().has_timestamp());
        EXPECT_EQ(offer->DataCampOffer.price().enable_auto_discounts().meta().timestamp().seconds(), seconds);
        EXPECT_FALSE(offer->DataCampOffer.price().enable_auto_discounts().flag());
    }

    {
        constexpr ui64 seconds = 567;
        Market::DataCamp::Offer dataCampOffer;
        dataCampOffer.mutable_price()->mutable_enable_auto_discounts()->set_flag(true);
        TFeedInfo feedInfo = {
            .EnableAutoDiscounts = false,
            .Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds),
        };
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithDataCampOffer(std::move(dataCampOffer))
            )
        );

        EXPECT_TRUE(offer->DataCampOffer.price().enable_auto_discounts().meta().has_timestamp());
        EXPECT_EQ(offer->DataCampOffer.price().enable_auto_discounts().meta().timestamp().seconds(), seconds);
        EXPECT_TRUE(offer->DataCampOffer.price().enable_auto_discounts().flag());
    }

    {
        constexpr ui64 seconds = 567;
        Market::DataCamp::Offer dataCampOffer;
        dataCampOffer.mutable_price()->mutable_enable_auto_discounts()->set_flag(true);
        TFeedInfo feedInfo = {
            .EnableAutoDiscounts = true,
            .Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds),
        };
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithDataCampOffer(std::move(dataCampOffer))
            )
        );

        EXPECT_TRUE(offer->DataCampOffer.price().enable_auto_discounts().meta().has_timestamp());
        EXPECT_EQ(offer->DataCampOffer.price().enable_auto_discounts().meta().timestamp().seconds(), seconds);
        EXPECT_TRUE(offer->DataCampOffer.price().enable_auto_discounts().flag());
    }
}


TEST_F(TestPriceProcessor, DynamicPricing) {
    {
        constexpr ui64 seconds = 567;
        Market::DataCamp::Offer dataCampOffer;
        dataCampOffer.mutable_price()->mutable_dynamic_pricing()->set_type(Market::DataCamp::DynamicPricing_Type_RECOMMENDED_PRICE);
        TFeedInfo feedInfo = {
            .Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds),
        };
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithRawDynamicPricing(
                        TDynamicPricing{
                            .ThresholdUnit = TDynamicPricing::PERCENT,
                            .ThresholdValue = 12.347,
                        }
                    )
                    .WithDataCampOffer(std::move(dataCampOffer))
            )
        );

        EXPECT_TRUE(offer->DataCampOffer.price().dynamic_pricing().has_threshold_percent());
        EXPECT_EQ(offer->DataCampOffer.price().dynamic_pricing().threshold_percent(), 1235);
        EXPECT_FALSE(offer->DataCampOffer.price().dynamic_pricing().has_threshold_fixed_value());
    }

    {
        constexpr ui64 seconds = 567;
        Market::DataCamp::Offer dataCampOffer;
        dataCampOffer.mutable_price()->mutable_dynamic_pricing()->set_type(Market::DataCamp::DynamicPricing_Type_MINIMAL_PRICE_ON_MARKET);
        TFeedInfo feedInfo = {
            .Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds),
        };
        auto offer = Process(
            feedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(feedInfo)
                    .WithRawDynamicPricing(
                        TDynamicPricing{
                            .ThresholdUnit = TDynamicPricing::FIXED,
                            .ThresholdValue = 234.5,
                        }
                    )
                    .WithDataCampOffer(std::move(dataCampOffer))
            )
        );

        EXPECT_FALSE(offer->DataCampOffer.price().dynamic_pricing().has_threshold_percent());
        EXPECT_TRUE(offer->DataCampOffer.price().dynamic_pricing().has_threshold_fixed_value());
        EXPECT_EQ(offer->DataCampOffer.price().dynamic_pricing().threshold_fixed_value(), 234'5000000);
    }

    // Threshold values are ignored because dynamic pricing stategy type is not provided
    {
        auto defaultFeedInfo = TFeedInfo{};
        auto offer = Process(
            defaultFeedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(defaultFeedInfo)
                    .WithRawDynamicPricing(
                        TDynamicPricing{
                            .ThresholdUnit = TDynamicPricing::FIXED,
                            .ThresholdValue = 234.5,
                        }
                    )
            )
        );

        EXPECT_FALSE(offer->DataCampOffer.price().dynamic_pricing().has_threshold_percent());
        EXPECT_FALSE(offer->DataCampOffer.price().dynamic_pricing().has_threshold_fixed_value());
    }

    // Threshold values are ignored if not unit or value is not provided
    {
        Market::DataCamp::Offer dataCampOffer;
        dataCampOffer.mutable_price()->mutable_dynamic_pricing()->set_type(Market::DataCamp::DynamicPricing_Type_MINIMAL_PRICE_ON_MARKET);
        auto defaultFeedInfo = TFeedInfo{};
        auto offer = Process(
            defaultFeedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(defaultFeedInfo)
                    .WithRawDynamicPricing(
                        TDynamicPricing{
                            .ThresholdValue = 234.5,
                        }
                    )
                    .WithDataCampOffer(std::move(dataCampOffer))
            )
        );

        EXPECT_FALSE(offer->DataCampOffer.price().dynamic_pricing().has_threshold_percent());
        EXPECT_FALSE(offer->DataCampOffer.price().dynamic_pricing().has_threshold_fixed_value());
    }

}

TEST_F(TestPriceProcessor, CheckWhiteOldPriceIsHigher) {
    TFeedInfo feedInfo = { .MarketColor = EMarketColor::MC_WHITE };
    feedInfo.IsDiscountsEnabled = true;
    auto offer = Process(
        feedInfo,
        FeedShopInfo,
        MakeAtomicShared<IFeedParser::TMsg>(
            TOfferCarrier(feedInfo)
                .WithRawPrice(12)
                .WithRawOldPrice(13)
        )
    );

    EXPECT_TRUE(offer->DataCampOffer.price().basic().has_binary_oldprice());

    feedInfo = { .MarketColor = EMarketColor::MC_WHITE };
    feedInfo.IsDiscountsEnabled = true;
    offer = Process(
        feedInfo,
        FeedShopInfo,
        MakeAtomicShared<IFeedParser::TMsg>(
            TOfferCarrier(feedInfo)
                .WithRawPrice(13)
                .WithRawOldPrice(12)
        )
    );

    EXPECT_FALSE(offer->DataCampOffer.price().basic().has_binary_oldprice());
}
