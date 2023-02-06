#include <market/idx/datacamp/miner/processors/offer_content_converter/converter.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/processor.h>
#include <market/idx/datacamp/miner/lib/test_utils.h>
#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>
#include <market/proto/common/common.pb.h>
#include <google/protobuf/util/time_util.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMiner;

Y_UNIT_TEST_SUITE(QuantityConverterTestSuite) {
    void Convert(NMiner::TDatacampOffer& offer, const NMiner::TOfferContentConverterConfig& config,
                 const google::protobuf::Timestamp& fixedTimestamp) {
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        NMarket::NCapsLock::TCapsLockFixer capsFixer;
        auto converter = NWhite::MakeContentConverter(capsFixer);
        converter->Process(offer, processingContext, config, fixedTimestamp);
    }

    NMiner::TDatacampOffer CreateOffer(Market::DataCamp::Offer& basic,
                                       Market::DataCamp::Offer& service,
                                       Market::DataCamp::MarketColor color,
                                       size_t min,
                                       size_t step) {
        service.mutable_meta()->set_rgb(color);
        (*service.mutable_meta()->mutable_platforms())[color] = true;
        NMiner::TDatacampOffer dcOffer(&basic, &service);
        auto originalTerms = dcOffer.GetService().mutable_content()->mutable_partner()->mutable_original_terms();
        originalTerms->mutable_quantity()->set_min(min);
        originalTerms->mutable_quantity()->set_step(step);
        return dcOffer;
    }

    NMiner::TDatacampOffer CreateOfferWithPrice(Market::DataCamp::Offer& basic,
                                                Market::DataCamp::Offer& service,
                                                Market::DataCamp::MarketColor color,
                                                size_t price, size_t default_price = 0) {
        service.mutable_meta()->set_rgb(color);
        (*service.mutable_meta()->mutable_platforms())[color] = true;
        NMiner::TDatacampOffer dcOffer(&basic, &service);
        if (price) {
            auto fixedPointValue = TFixedPointNumber(price).AsRaw();
            service.mutable_price()->mutable_basic()->mutable_binary_price()->set_price(fixedPointValue);
        }
        if (default_price) {
            auto fixedPointValue = TFixedPointNumber(default_price).AsRaw();
            basic.mutable_price()->mutable_basic()->mutable_binary_price()->set_price(fixedPointValue);
        }
        // Category from the file allowed-auto-min-quantity-categories.txt
        basic.mutable_content()->mutable_market()->mutable_enriched_offer()->set_category_id(91167);
        return dcOffer;
    }

    NMiner::TOfferContentConverterConfig config("");
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    NMarket::NCapsLock::TCapsLockFixer capsFixer;

    void CheckResult(const NMiner::TDatacampOffer& offer, size_t min = 0, size_t step = 0) {
        const auto& serviceOffer = offer.GetService();
        UNIT_ASSERT(!offer.GetBasic().content().partner().actual().has_quantity());
        if (!min) {
            UNIT_ASSERT(!serviceOffer.content().partner().actual().quantity().has_min());
        } else {
            UNIT_ASSERT_EQUAL(serviceOffer.content().partner().actual().quantity().min(), min);
        }

        if (!step) {
            UNIT_ASSERT(!serviceOffer.content().partner().actual().quantity().has_step());
        } else {
            UNIT_ASSERT_EQUAL(serviceOffer.content().partner().actual().quantity().step(), step);
        }

        UNIT_ASSERT_EQUAL(serviceOffer.content().partner().actual().quantity().meta().timestamp(), fixedTimestamp);
        UNIT_ASSERT_EQUAL(serviceOffer.content().partner().actual().quantity().meta().applier(), NMarketIndexer::Common::EComponent::MINER);
    }

    Y_UNIT_TEST(ActualPresentNoOriginal) {
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto converter = NWhite::MakeContentConverter(capsFixer);
        for (const auto color: {Market::DataCamp::MarketColor::WHITE, Market::DataCamp::MarketColor::BLUE}) {
            Market::DataCamp::Offer basic;
            Market::DataCamp::Offer service;
            service.mutable_meta()->set_rgb(color);
            (*service.mutable_meta()->mutable_platforms())[color] = true;
            NMiner::TDatacampOffer dcOffer(&basic, &service);

            auto actualService = dcOffer.GetService().mutable_content()->mutable_partner()->mutable_actual();
            actualService->mutable_quantity()->set_min(10);
            actualService->mutable_quantity()->set_step(10);

            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            CheckResult(dcOffer, 0, 0);
        }
    }

    Y_UNIT_TEST(NoActualNoOriginal) {
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        NMiner::TDatacampOffer offer = MakeDefault();

        auto converter = NWhite::MakeContentConverter(capsFixer);
        converter->Process(offer, processingContext, config, fixedTimestamp);

        UNIT_ASSERT(!offer.GetBasic().content().partner().actual().has_quantity());
        UNIT_ASSERT(!offer.GetService().content().partner().actual().has_quantity());
    }

    Y_UNIT_TEST(ActualAndOriginalPresent) {
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto converter = NWhite::MakeContentConverter(capsFixer);

        for (const auto color: {Market::DataCamp::MarketColor::WHITE, Market::DataCamp::MarketColor::BLUE}) {
            Market::DataCamp::Offer basic;
            Market::DataCamp::Offer service;
            NMiner::TDatacampOffer dcOffer = CreateOffer(basic, service, color, /*original.min*/ 20, /*original.step*/ 10);

            auto actualService = service.mutable_content()->mutable_partner()->mutable_actual();
            actualService->mutable_quantity()->set_min(5);
            actualService->mutable_quantity()->set_step(5);

            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            CheckResult(dcOffer, 20, 10);
        }
    }

    Y_UNIT_TEST(ForceSetQuantity_IfLowPrice) {
        NMiner::TOfferContentConverterConfig config("");
        config.SetQuantityAutomatically = true;
        config.QuantityAutomaticallyRubPriceLimit = 500;
        config.UseNewQuantityValidation = true;
        config.MaxQuantityStep = 35;
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto converter = NWhite::MakeContentConverter(capsFixer);

        // ceil(500.0/210.0) = 3
        {
            Market::DataCamp::Offer basic;
            Market::DataCamp::Offer service;
            auto dcOffer = CreateOfferWithPrice(basic, service, Market::DataCamp::MarketColor::BLUE, 210 /*RUB*/);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            CheckResult(dcOffer, 3, 3);
        }

        // Цена 0 - не применяем стратегию
        {
            Market::DataCamp::Offer basic;
            Market::DataCamp::Offer service;
            auto dcOffer = CreateOfferWithPrice(basic, service, Market::DataCamp::MarketColor::BLUE, 0 /*RUB*/);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            UNIT_ASSERT(!dcOffer.GetService().content().partner().actual().quantity().has_min());
            UNIT_ASSERT(!dcOffer.GetService().content().partner().actual().quantity().has_step());
        }

        // Если цена слишком низкая, формула расчета будет давать огромное значение step-quantity, обрезаем до допустимого максимума
        {
            Market::DataCamp::Offer basic;
            Market::DataCamp::Offer service;
            auto dcOffer = CreateOfferWithPrice(basic, service, Market::DataCamp::MarketColor::BLUE, 2 /*RUB*/);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            CheckResult(dcOffer, 35, 35);
        }

        // Есть партнерская step-quantity в допустимых границах (>= 1 & <= 30) - не применяем свою
        {
            Market::DataCamp::Offer basic;
            Market::DataCamp::Offer service;
            auto originalTerms = service.mutable_content()->mutable_partner()->mutable_original_terms();
            originalTerms->mutable_quantity()->set_step(5);
            auto dcOffer = CreateOfferWithPrice(basic, service, Market::DataCamp::MarketColor::BLUE, 200 /*RUB*/);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            CheckResult(dcOffer, 5, 5);
        }


        // Есть партнерская step-quantity вне допустимых границ (>= 1 & <= 30) - применяем свою
        {
            Market::DataCamp::Offer basic;
            Market::DataCamp::Offer service;
            auto originalTerms = service.mutable_content()->mutable_partner()->mutable_original_terms();
            originalTerms->mutable_quantity()->set_step(100);
            auto dcOffer = CreateOfferWithPrice(basic, service, Market::DataCamp::MarketColor::BLUE, 90 /*RUB*/);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            CheckResult(dcOffer, 6, 6);
        }

        // Оффер, у которого нет сервисной цены, но есть дефолтная
        // При подсчете квантов должны учитывать дефолтную цену - ceil(500.0/210.0) = 3
        {
            Market::DataCamp::Offer basic;
            Market::DataCamp::Offer service;
            auto dcOffer = CreateOfferWithPrice(basic, service, Market::DataCamp::MarketColor::BLUE, 0 /*price*/,  210 /*default_price - RUB*/);
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            CheckResult(dcOffer, 3, 3);
        }
    }

    Y_UNIT_TEST(DoNotSetQuantity_IfDeclinedCategory) {
        NMiner::TOfferContentConverterConfig config("");
        config.SetQuantityAutomatically = true;
        config.QuantityAutomaticallyRubPriceLimit = 500;
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto converter = NWhite::MakeContentConverter(capsFixer);

        Market::DataCamp::Offer basic;
        Market::DataCamp::Offer service;
        auto dcOffer = CreateOfferWithPrice(basic, service, Market::DataCamp::MarketColor::BLUE, 200 /*RUB*/);
        // Category is not in the allowed file
        basic.mutable_content()->mutable_market()->mutable_enriched_offer()->set_category_id(91666);
        converter->Process(dcOffer, processingContext, config, fixedTimestamp);
        UNIT_ASSERT(!dcOffer.GetService().content().partner().actual().quantity().has_min());
        UNIT_ASSERT(!dcOffer.GetService().content().partner().actual().quantity().has_step());
    }

    Y_UNIT_TEST(OriginalPresentWrongCategory) {
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        config.ShouldCheckCategoryForQuantity = true;
        auto converter = NWhite::MakeContentConverter(capsFixer);
        for (const auto color: {Market::DataCamp::MarketColor::WHITE, Market::DataCamp::MarketColor::BLUE}) {
            Market::DataCamp::Offer basic;
            Market::DataCamp::Offer service;
            NMiner::TDatacampOffer dcOffer = CreateOffer(basic, service, color, /*original.min*/ 20, /*original.step*/ 10);
            dcOffer.GetBasicByColor().mutable_content()->mutable_market()->mutable_enriched_offer()->set_category_id(666);

            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            CheckResult(dcOffer, 1, 1);
        }
    }

    Y_UNIT_TEST(OriginalPresentTurnOffCategoryCheck) {
        config.ShouldCheckCategoryForQuantity = false;
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto converter = NWhite::MakeContentConverter(capsFixer);
        for (const auto color: {Market::DataCamp::MarketColor::WHITE, Market::DataCamp::MarketColor::BLUE}) {
            Market::DataCamp::Offer basic;
            Market::DataCamp::Offer service;
            NMiner::TDatacampOffer dcOffer = CreateOffer(basic, service, color, /*original.min*/ 20, /*original.step*/ 10);
            dcOffer.GetBasicByColor().mutable_content()->mutable_market()->mutable_enriched_offer()->set_category_id(666);

            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            CheckResult(dcOffer, 20, 10);
        }
        config.ShouldCheckCategoryForQuantity = true;
    }

    Y_UNIT_TEST(OriginalPresentGoodCategory) {
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto converter = NWhite::MakeContentConverter(capsFixer);
        for (const auto color: {Market::DataCamp::MarketColor::WHITE, Market::DataCamp::MarketColor::BLUE}) {
            Market::DataCamp::Offer basic;
            Market::DataCamp::Offer service;
            NMiner::TDatacampOffer dcOffer = CreateOffer(basic, service, color, /*original.min*/ 20, /*original.step*/ 10);
            dcOffer.GetBasicByColor().mutable_content()->mutable_market()->mutable_enriched_offer()->set_category_id(7308012);

            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            CheckResult(dcOffer, 20, 10);
        }
    }

    Y_UNIT_TEST(ActualPresentNoOriginalIncorrectStep) {
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        config.UseNewQuantityValidation = false;
        auto converter = NWhite::MakeContentConverter(capsFixer);
        for (const auto color: {Market::DataCamp::MarketColor::WHITE, Market::DataCamp::MarketColor::BLUE}) {
            Market::DataCamp::Offer basic;
            Market::DataCamp::Offer service;
            NMiner::TDatacampOffer dcOffer = CreateOffer(basic, service, color, /*original.min*/ 1, /*original.step*/ 10);

            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            CheckResult(dcOffer, 1, 1);
        }
        config.UseNewQuantityValidation = true;
    }

    Y_UNIT_TEST(DropInvalidQuantityValues) {
        config.UseNewQuantityValidation = true;
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto converter = NWhite::MakeContentConverter(capsFixer);
        size_t bigValue = 100500;
        for (const auto color: {Market::DataCamp::MarketColor::WHITE, Market::DataCamp::MarketColor::BLUE}) {
            // Only step invalid - drop step
            {
                Market::DataCamp::Offer basic;
                Market::DataCamp::Offer service;
                NMiner::TDatacampOffer dcOffer = CreateOffer(basic, service, color, /*original.min*/ 1, /*original.step*/ bigValue);

                converter->Process(dcOffer, processingContext, config, fixedTimestamp);
                CheckResult(dcOffer, 1, 0);
            }
            // Both invalid - drop all quantity values
            {
                Market::DataCamp::Offer basic;
                Market::DataCamp::Offer service;
                NMiner::TDatacampOffer dcOffer = CreateOffer(basic, service, color, /*original.min*/ bigValue, /*original.step*/ bigValue);

                converter->Process(dcOffer, processingContext, config, fixedTimestamp);
                CheckResult(dcOffer, 0, 0);
            }
        }
        config.UseNewQuantityValidation = false;
    }

    Y_UNIT_TEST(ValidateQuantityValuesCombination) {
        config.UseNewQuantityValidation = true;
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto converter = NWhite::MakeContentConverter(capsFixer);
        for (const auto color: {Market::DataCamp::MarketColor::WHITE, Market::DataCamp::MarketColor::BLUE}) {
            // min = step - OK
            {
                Market::DataCamp::Offer basic;
                Market::DataCamp::Offer service;
                NMiner::TDatacampOffer dcOffer = CreateOffer(basic, service, color, /*original.min*/ 5, /*original.step*/ 5);

                converter->Process(dcOffer, processingContext, config, fixedTimestamp);
                CheckResult(dcOffer, 5, 5);
            }
            // min < step - set min = step
            {
                Market::DataCamp::Offer basic;
                Market::DataCamp::Offer service;
                NMiner::TDatacampOffer dcOffer = CreateOffer(basic, service, color, /*original.min*/ 5, /*original.step*/ 10);

                converter->Process(dcOffer, processingContext, config, fixedTimestamp);
                CheckResult(dcOffer, 10, 10);
            }
            // min > step and min is divisible by step - OK
            {
                Market::DataCamp::Offer basic;
                Market::DataCamp::Offer service;
                NMiner::TDatacampOffer dcOffer = CreateOffer(basic, service, color, /*original.min*/ 20, /*original.step*/ 5);

                converter->Process(dcOffer, processingContext, config, fixedTimestamp);
                CheckResult(dcOffer, 20, 5);
            }
            // min > step and min is not divisible by step - set min according to step
            {
                Market::DataCamp::Offer basic;
                Market::DataCamp::Offer service;
                NMiner::TDatacampOffer dcOffer = CreateOffer(basic, service, color, /*original.min*/ 30, /*original.step*/ 12);

                converter->Process(dcOffer, processingContext, config, fixedTimestamp);
                CheckResult(dcOffer, 36, 12);
            }
        }
        config.UseNewQuantityValidation = false;
    }

    Y_UNIT_TEST(SetMinIfHasOnlyStep) {
        config.UseNewQuantityValidation = true;
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto converter = NWhite::MakeContentConverter(capsFixer);
        size_t bigValue = 100500;
        for (const auto color: {Market::DataCamp::MarketColor::WHITE, Market::DataCamp::MarketColor::BLUE}) {
            // Min is invalid - set min = step
            {
                Market::DataCamp::Offer basic;
                Market::DataCamp::Offer service;
                NMiner::TDatacampOffer dcOffer = CreateOffer(basic, service, color, /*original.min*/ bigValue, /*original.step*/ 5);

                converter->Process(dcOffer, processingContext, config, fixedTimestamp);
                CheckResult(dcOffer, 5, 5);
            }
            // No min - set min = step
            {
                Market::DataCamp::Offer basic;
                Market::DataCamp::Offer service;
                NMiner::TDatacampOffer dcOffer = CreateOffer(basic, service, color, /*original.min*/ 0, /*original.step*/ 3);

                converter->Process(dcOffer, processingContext, config, fixedTimestamp);
                CheckResult(dcOffer, 3, 3);
            }
        }
        config.UseNewQuantityValidation = false;
    }
}
