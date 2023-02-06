#include <market/idx/datacamp/miner/processors/offer_content_converter/converter.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>
#include <market/idx/delivery/lib/options/options.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NMiner;

namespace {
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    const auto oldTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(327605478); // 1980
}

NMiner::TDatacampOffer CreateOfferFromBasicAndService(Market::DataCamp::Offer& basic,
                                                      Market::DataCamp::Offer& service,
                                                      Market::DataCamp::MarketColor color,
                                                      const TMaybe<bool>& deliveryFlag,
                                                      const TMaybe<bool>& pickupFlag,
                                                      const TMaybe<bool>& storeFlag,
                                                      const TMaybe<bool>& availableFlag,
                                                      bool setDirectProductMapping)
{
    service.mutable_meta()->set_rgb(color);
    (*service.mutable_meta()->mutable_platforms())[color] = true;
    NMiner::TDatacampOffer dcOffer(&basic, &service);
    auto* original = dcOffer.GetService().mutable_delivery()->mutable_partner()->mutable_original();

    if (deliveryFlag.Defined()) {
        original->mutable_delivery()->set_flag(*deliveryFlag);
    }
    original->mutable_delivery()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);

    if (pickupFlag) {
        original->mutable_pickup()->set_flag(*pickupFlag);
    }
    original->mutable_pickup()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);

    if (storeFlag) {
        original->mutable_store()->set_flag(*storeFlag);
    }
    original->mutable_store()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);

    if (availableFlag.Defined()) {
        original->mutable_available()->set_flag(*availableFlag);
    }
    original->mutable_available()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);

    if (setDirectProductMapping) {
        dcOffer.GetService().mutable_partner_info()->set_direct_product_mapping(true);
    }
    return dcOffer;
}

Y_UNIT_TEST_SUITE(DeliveryConverterSuite) {
        NMiner::TOfferContentConverterConfig config("");
        NMarket::NCapsLock::TCapsLockFixer capsFixer;
        auto converter = NWhite::MakeContentConverter(capsFixer);

        void TestBasicAndService(const TMaybe<bool>& deliveryFlag,
                                 const TMaybe<bool>& pickupFlag,
                                 const TMaybe<bool>& storeFlag,
                                 const TMaybe<bool>& availableFlag,
                                 bool expectedDeliveryFlag,
                                 bool expectedPickupFlag,
                                 bool expectedStoreFlag,
                                 bool expectedAvailableFlag,
                                 bool setDirectProductMapping = false)
        {
            for (const auto color: {Market::DataCamp::MarketColor::WHITE, Market::DataCamp::MarketColor::BLUE}) {
                Market::DataCamp::Offer basic;
                Market::DataCamp::Offer service;
                auto offer = CreateOfferFromBasicAndService(
                    basic, service, color,
                    deliveryFlag, pickupFlag, storeFlag, availableFlag,
                    setDirectProductMapping
                );
                NMarket::TDatacampOfferBatchProcessingContext context {};

                converter->Process(offer, context, config, fixedTimestamp);

                if (color == Market::DataCamp::MarketColor::BLUE) {
                    UNIT_ASSERT(!offer.GetService().delivery().partner().actual().has_delivery());
                } else {
                    UNIT_ASSERT_EQUAL(offer.GetService().delivery().partner().actual().delivery().flag(),
                                      expectedDeliveryFlag);
                    UNIT_ASSERT_EQUAL(offer.GetService().delivery().partner().actual().pickup().flag(),
                                      expectedPickupFlag);
                    UNIT_ASSERT_EQUAL(offer.GetService().delivery().partner().actual().store().flag(),
                                      expectedStoreFlag);
                    UNIT_ASSERT_EQUAL(offer.GetService().delivery().partner().actual().available().flag(),
                                      expectedAvailableFlag);
                }

                if (color == Market::DataCamp::MarketColor::BLUE) {
                    UNIT_ASSERT_EQUAL(&offer.GetService(), &offer.GetBasicByColor());
                } else {
                    UNIT_ASSERT(!offer.GetBasicByColor().delivery().partner().actual().has_delivery());
                    UNIT_ASSERT(!offer.GetBasicByColor().delivery().partner().actual().has_pickup());
                    UNIT_ASSERT(!offer.GetBasicByColor().delivery().partner().actual().has_store());
                    UNIT_ASSERT(!offer.GetBasicByColor().delivery().partner().actual().has_available());
                }
             }
        }

        void TestDownloadableWithDelivery(bool isUnitedCatalog) {
            for (const auto color: {Market::DataCamp::MarketColor::WHITE, Market::DataCamp::MarketColor::BLUE}) {
                NMarket::TDatacampOfferBatchProcessingContext processingContext;

                Market::DataCamp::Offer basic;
                Market::DataCamp::Offer service;
                service.mutable_meta()->set_rgb(color);
                (*service.mutable_meta()->mutable_platforms())[color] = true;
                service.mutable_status()->mutable_united_catalog()->set_flag(isUnitedCatalog);
                NMiner::TDatacampOffer dcOffer(&basic, &service);

                auto* originalContent = dcOffer.GetBasicByUnitedCatalogStatus().mutable_content()->mutable_partner()->mutable_original();
                auto* originalDelivery = dcOffer.GetService().mutable_delivery()->mutable_partner()->mutable_original();

                originalContent->mutable_downloadable()->set_flag(true);

                originalDelivery->mutable_delivery()->set_flag(true);
                originalDelivery->mutable_delivery()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);

                auto originalDeliveryOptions = originalDelivery->mutable_delivery_options();
                originalDeliveryOptions->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
                {
                    auto option = originalDeliveryOptions->mutable_options()->Add();
                    option->SetCost(1);
                    option->SetDaysMin(2);
                    option->SetDaysMax(4);
                }

                converter->Process(dcOffer, processingContext, config, fixedTimestamp);

                // проверяем, что правильно положили флаг downloadable
                UNIT_ASSERT_EQUAL(dcOffer.GetBasicByUnitedCatalogStatus().content().partner().actual().downloadable().flag(), true);

                // для синих офферов вообще не должно быть доставки
                if (color == Market::DataCamp::MarketColor::BLUE) {
                    UNIT_ASSERT(!dcOffer.GetService().delivery().partner().actual().has_delivery());
                } else {
                    UNIT_ASSERT_EQUAL(dcOffer.GetService().delivery().partner().actual().delivery().flag(), false);
                    UNIT_ASSERT_EQUAL(dcOffer.GetService().delivery().partner().actual().delivery_options().options().size(), 0);
                }
            }
        }

        Y_UNIT_TEST(DefaultValues) {
            TestBasicAndService(Nothing(), Nothing(), Nothing(), Nothing(), true, true, false, true);
        }

        Y_UNIT_TEST(CopyAllFields) {
            TestBasicAndService(false, false, true, false, false, false, true, false);
        }

        Y_UNIT_TEST(EnabledStoreForSmb) {
            TestBasicAndService(Nothing(), Nothing(), Nothing(), Nothing(), true, true, true, true, true);
        }

        Y_UNIT_TEST(ValidateDeliveryOptions) {
            // Валидные опции доставки отмечены "ОК" (в тч опция с изменением минимального срока доставки)
            // Кейсы разделены на 2 оффера (сначала мы режем опций по max_amount, потом проверям (а кейсов больше, чем max_amount))
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer_1 = MakeDefault();
            auto original_1 = dcOffer_1.GetService().mutable_delivery()->mutable_partner()->mutable_original();
            auto originalOptions_1 = original_1->mutable_delivery_options()->mutable_options();

            original_1->mutable_delivery()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            {
                // OK
                auto option = originalOptions_1->Add();
                option->SetCost(1);
                option->SetDaysMin(2);
                option->SetDaysMax(4);
            }
            {
                // invalid cost
                auto option = originalOptions_1->Add();
                option->SetCost(-2);
                option->SetDaysMin(2);
                option->SetDaysMax(4);
            }
            {
                // too large range, but OK
                auto option = originalOptions_1->Add();
                option->SetCost(3);
                option->SetDaysMin(2);
                option->SetDaysMax(10);
            }
            {
                // invalid range
                auto option = originalOptions_1->Add();
                option->SetCost(4);
                option->SetDaysMin(4);
                option->SetDaysMax(2);
            }
            {
                // empty days, will set undefined period - OK
                auto option = originalOptions_1->Add();
                option->SetCost(5);
            }
            {
                // OK - but wont be used - out of max amount
                auto option = originalOptions_1->Add();
                option->SetCost(22);
                option->SetDaysMin(2);
                option->SetDaysMax(4);
            }

            converter->Process(dcOffer_1, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(originalOptions_1->size(), 6); // все оригинальные опции на месте

            const auto& deliveryOptions_1 = dcOffer_1.GetService().delivery().partner().actual().delivery_options();
            UNIT_ASSERT_EQUAL(deliveryOptions_1.has_meta(), true);
            UNIT_ASSERT_EQUAL(deliveryOptions_1.meta().timestamp(), fixedTimestamp);

            auto& actual_1 = deliveryOptions_1.options();

            UNIT_ASSERT_EQUAL(actual_1.size(), 3);
            UNIT_ASSERT_EQUAL(actual_1[0].GetCost(), 1);
            UNIT_ASSERT_EQUAL(actual_1[1].GetCost(), 3);
            UNIT_ASSERT_EQUAL(actual_1[1].GetDaysMin(), 8);
            UNIT_ASSERT_EQUAL(actual_1[1].GetDaysMax(), 10);
            UNIT_ASSERT_EQUAL(actual_1[1].GetOrderBeforeHour(), 13);
            UNIT_ASSERT_EQUAL(actual_1[2].GetCost(), 5);
            UNIT_ASSERT_EQUAL(actual_1[2].GetDaysMin(), 255);
            UNIT_ASSERT_EQUAL(actual_1[2].GetDaysMax(), 255);
            UNIT_ASSERT_EQUAL(actual_1[2].GetOrderBeforeHour(), 24);

            // часть 2
            auto dcOffer_2 = MakeDefault();
            auto original_2 = dcOffer_2.GetService().mutable_delivery()->mutable_partner()->mutable_original();
            auto originalOptions_2 = original_2->mutable_delivery_options()->mutable_options();

            original_2->mutable_delivery()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            {
                // Не валидная опция с очень большой ценой
                auto option = originalOptions_2->Add();
                option->SetCost(100100100);
                option->SetDaysMin(2);
                option->SetDaysMax(5);
            }
            {
                // Не валидная опция без цены вообще
                auto option = originalOptions_2->Add();
                option->SetDaysMin(2);
                option->SetDaysMax(4);
            }

            converter->Process(dcOffer_2, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(originalOptions_2->size(), 2); // все оригинальные опции на месте

            const auto& deliveryOptions_2 = dcOffer_2.GetService().delivery().partner().actual().delivery_options();
            UNIT_ASSERT_EQUAL(deliveryOptions_2.has_meta(), true);
            UNIT_ASSERT_EQUAL(deliveryOptions_2.meta().timestamp(), fixedTimestamp);

            auto& actual_2 = deliveryOptions_2.options();
            UNIT_ASSERT_EQUAL(actual_2.size(), 0);
        }

        Y_UNIT_TEST(CorrectDeliveryOptions) {
            // Проверяем, что при наличие нескольких опций с maxDays > DaysUnknownThreshold после обработки остается только 1
            // у нее обновляются maxDays=minDays=255, остальные длинные опции зачищаются
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();
            auto original = dcOffer.GetService().mutable_delivery()->mutable_partner()->mutable_original();
            auto originalOptions = original->mutable_delivery_options()->mutable_options();

            original->mutable_delivery()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            {
                // OK
                auto option = originalOptions->Add();
                option->SetCost(1);
                option->SetDaysMin(2);
                option->SetDaysMax(4);
            }
            {
                // not cheapest long option-1
                auto option = originalOptions->Add();
                option->SetCost(300);
                option->SetDaysMin(62);
                option->SetDaysMax(64);
            }
            {
                // !! cheapest long option
                auto option = originalOptions->Add();
                option->SetCost(50);
                option->SetDaysMin(62);
                option->SetDaysMax(64);
            }
            {
                // not cheapest long option-2
                auto option = originalOptions->Add();
                option->SetCost(400);
                option->SetDaysMin(62);
                option->SetDaysMax(64);
            }

            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(&dcOffer.GetService(), &dcOffer.GetBasicByColor());

            const auto& deliveryOptions = dcOffer.GetService().delivery().partner().actual().delivery_options();
            UNIT_ASSERT_EQUAL(deliveryOptions.has_meta(), true);
            UNIT_ASSERT_EQUAL(deliveryOptions.meta().timestamp(), fixedTimestamp);

            auto& actual = deliveryOptions.options();
            //осталась хорошая опция и сама дешевая из долгих
            UNIT_ASSERT_EQUAL(actual.size(), 2);
            UNIT_ASSERT_EQUAL(actual[0].GetCost(), 1);
            UNIT_ASSERT_EQUAL(actual[1].GetCost(), 50);
            UNIT_ASSERT_EQUAL(actual[1].GetDaysMin(), NDelivery::DaysUnknown);
            UNIT_ASSERT_EQUAL(actual[1].GetDaysMax(), NDelivery::DaysUnknown);
            UNIT_ASSERT_EQUAL(actual[1].GetOrderBeforeHour(), NDelivery::FreeOrderBeforeHour);
        }

        Y_UNIT_TEST(ValidatePickupOptions) {
            // Валидные опции самовывоза: 1, 3(с изменением минимального срока доставки), но из-за лимита на количество опций остается только первая
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();

            auto original = dcOffer.GetService().mutable_delivery()->mutable_partner()->mutable_original()->mutable_pickup_options()->mutable_options();
            {
                // OK
                auto option = original->Add();
                option->SetCost(1);
                option->SetDaysMin(2);
                option->SetDaysMax(4);
            }
            {
                // invalid cost
                auto option = original->Add();
                option->SetCost(-2);
                option->SetDaysMin(2);
                option->SetDaysMax(4);
            }
            {
                // too large range, but OK
                auto option = original->Add();
                option->SetCost(3);
                option->SetDaysMin(2);
                option->SetDaysMax(10);
            }
            {
                // invalid range
                auto option = original->Add();
                option->SetCost(4);
                option->SetDaysMin(4);
                option->SetDaysMax(2);
            }

            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(dcOffer.GetService().delivery().partner().actual().pickup_options().has_meta(), true);
            UNIT_ASSERT_EQUAL(dcOffer.GetService().delivery().partner().actual().pickup_options().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(&dcOffer.GetService(), &dcOffer.GetBasicByColor());
        }

        Y_UNIT_TEST(ClearOldDeliveryAndPickupOptions) {
            NMarket::TDatacampOfferBatchProcessingContext processingContext;

            auto dcOffer = MakeDefault();
            {
                auto actual = dcOffer.GetService().mutable_delivery()->mutable_partner()->mutable_actual()->mutable_delivery_options()->mutable_options();
                auto option = actual->Add();
                option->SetCost(3);
            }
            {
                auto actual = dcOffer.GetService().mutable_delivery()->mutable_partner()->mutable_actual()->mutable_pickup_options()->mutable_options();
                auto option = actual->Add();
                option->SetCost(3);
            }

            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(&dcOffer.GetService(), &dcOffer.GetBasicByColor());
            UNIT_ASSERT_EQUAL(dcOffer.GetService().delivery().partner().actual().pickup_options().has_meta(), true);
            UNIT_ASSERT_EQUAL(dcOffer.GetService().delivery().partner().actual().pickup_options().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(dcOffer.GetService().delivery().partner().actual().pickup_options().options().size(), 0);
            UNIT_ASSERT_EQUAL(dcOffer.GetService().delivery().partner().actual().delivery_options().has_meta(), true);
            UNIT_ASSERT_EQUAL(dcOffer.GetService().delivery().partner().actual().delivery_options().meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(dcOffer.GetService().delivery().partner().actual().delivery_options().options().size(), 0);
        }

        Y_UNIT_TEST(ValidatePickupOptionsForgiveMistakes) {
            // В actual хорошая опция, в original - нет => полностью сохраняем ту, что в actual
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();

            auto actual_pickup_options = dcOffer.GetService().mutable_delivery()->mutable_partner()->mutable_actual()->mutable_pickup_options();
            actual_pickup_options->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            {
                // OK
                auto option = actual_pickup_options->mutable_options()->Add();
                option->SetCost(1);
                option->SetDaysMin(2);
                option->SetDaysMax(4);
            }

            auto original = dcOffer.GetService().mutable_delivery()->mutable_partner()->mutable_original()->mutable_pickup_options()->mutable_options();
            {
                // invalid cost
                auto option = original->Add();
                option->SetCost(-2);
                option->SetDaysMin(3);
                option->SetDaysMax(5);
            }

            config.ForgiveMistakes = true;
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(&dcOffer.GetService(), &dcOffer.GetBasicByColor());
            auto final_pickup_options = dcOffer.GetService().delivery().partner().actual().pickup_options();
            UNIT_ASSERT_EQUAL(final_pickup_options.has_meta(), true);
            UNIT_ASSERT_EQUAL(final_pickup_options.meta().timestamp(), oldTimestamp);

            UNIT_ASSERT_EQUAL(final_pickup_options.options().size(), 1);
            UNIT_ASSERT_EQUAL(final_pickup_options.options()[0].GetCost(), 1);
        }

        Y_UNIT_TEST(EmptyPickupOptionsForgiveMistakes) {
            // В actual хорошая опция, в original - нет ничего => магазин может убрать пооферную опцию
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();

            auto actual_pickup_options = dcOffer.GetService().mutable_delivery()->mutable_partner()->mutable_actual()->mutable_pickup_options();
            actual_pickup_options->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            {
                // OK
                auto option = actual_pickup_options->mutable_options()->Add();
                option->SetCost(1);
                option->SetDaysMin(2);
                option->SetDaysMax(4);
            }

            config.ForgiveMistakes = true;
            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(&dcOffer.GetService(), &dcOffer.GetBasicByColor());
            auto final_pickup_options = dcOffer.GetService().delivery().partner().actual().pickup_options();
            UNIT_ASSERT_EQUAL(final_pickup_options.has_meta(), true);
            UNIT_ASSERT_EQUAL(final_pickup_options.meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(final_pickup_options.options().size(), 0);
        }

        Y_UNIT_TEST(ValidateDeliveryOptionsForgiveMistakes) {
            // В actual хорошая опция, в original - нет => полностью сохраняем ту, что в actual
            config.ForgiveMistakes = true;

            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();

            auto actual_delivery_options = dcOffer.GetService().mutable_delivery()->mutable_partner()->mutable_actual()->mutable_delivery_options();
            actual_delivery_options->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            {
                // OK
                auto option = actual_delivery_options->mutable_options()->Add();
                option->SetCost(1);
                option->SetDaysMin(2);
                option->SetDaysMax(4);
            }

            auto original = dcOffer.GetService().mutable_delivery()->mutable_partner()->mutable_original()->mutable_delivery_options()->mutable_options();
            {
                // invalid cost
                auto option = original->Add();
                option->SetCost(-2);
                option->SetDaysMin(3);
                option->SetDaysMax(5);
            }

            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(&dcOffer.GetService(), &dcOffer.GetBasicByColor());
            auto final_delivery_options = dcOffer.GetService().delivery().partner().actual().delivery_options();
            UNIT_ASSERT_EQUAL(final_delivery_options.has_meta(), true);
            UNIT_ASSERT_EQUAL(final_delivery_options.meta().timestamp(), oldTimestamp);

            UNIT_ASSERT_EQUAL(final_delivery_options.options().size(), 1);
            UNIT_ASSERT_EQUAL(final_delivery_options.options()[0].GetCost(), 1);
        }

        Y_UNIT_TEST(EmptyDeliveryOptionsForgiveMistakes) {
            // В actual хорошая опция, в original - нет ничего => магазин может убрать пооферную опцию
            config.ForgiveMistakes = true;

            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();

            auto actual_delivery_options = dcOffer.GetService().mutable_delivery()->mutable_partner()->mutable_actual()->mutable_delivery_options();
            actual_delivery_options->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            {
                // OK
                auto option = actual_delivery_options->mutable_options()->Add();
                option->SetCost(1);
                option->SetDaysMin(2);
                option->SetDaysMax(4);
            }

            converter->Process(dcOffer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(&dcOffer.GetService(), &dcOffer.GetBasicByColor());
            auto final_delivery_options = dcOffer.GetService().delivery().partner().actual().delivery_options();
            UNIT_ASSERT_EQUAL(final_delivery_options.has_meta(), true);
            UNIT_ASSERT_EQUAL(final_delivery_options.meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(final_delivery_options.options().size(), 0);
        }

        Y_UNIT_TEST(CopyDownloadable) {
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto offer = MakeDefault();
            auto* originalContent = offer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_original();
            originalContent->mutable_downloadable()->set_flag(true);

            converter->Process(offer, processingContext, config, fixedTimestamp);

            UNIT_ASSERT_EQUAL(&offer.GetService(), &offer.GetBasicByColor());
            UNIT_ASSERT_EQUAL(offer.GetService().content().partner().actual().downloadable().flag(), true);
        }

        Y_UNIT_TEST(DropDeliveryIfDownloadableIsSetForUnitedCatalog) {
            TestDownloadableWithDelivery(true);
        }

        Y_UNIT_TEST(DropDeliveryIfDownloadableIsSetForOldCatalog) {
            TestDownloadableWithDelivery(false);
        }

        Y_UNIT_TEST(NoDeliveryOptionsForAlcohol) {
            // У алкоголя нет курьерской доставки, даже если партнер ее передал. Тип оффера берем из оригинальной части
            // контента.
            NMarket::TDatacampOfferBatchProcessingContext processingContext;
            auto dcOffer = MakeDefault();
            dcOffer.GetBasic().mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(
                    NMarket::EProductType::ALCO);

            auto& origial = *dcOffer.GetService().mutable_delivery()->mutable_partner()->mutable_original();
            origial.mutable_delivery()->set_flag(true);
            origial.mutable_delivery_options()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
            {
                auto option = origial.mutable_delivery_options()->mutable_options()->Add();
                option->SetCost(1);
                option->SetDaysMin(2);
                option->SetDaysMax(4);
            }

            converter->Process(dcOffer, processingContext, config, fixedTimestamp);
            UNIT_ASSERT(dcOffer.GetService().delivery().partner().actual().has_delivery());
            UNIT_ASSERT_EQUAL(dcOffer.GetService().delivery().partner().actual().delivery().flag(), false);
            auto final_delivery_options = dcOffer.GetService().delivery().partner().actual().delivery_options();
            UNIT_ASSERT_EQUAL(final_delivery_options.has_meta(), true);
            UNIT_ASSERT_EQUAL(final_delivery_options.meta().timestamp(), fixedTimestamp);
            UNIT_ASSERT_EQUAL(final_delivery_options.options().size(), 0);
            UNIT_ASSERT_EQUAL(
                    dcOffer.GetBasic().content().partner().actual().type().value(),
                    NMarket::EProductType::ALCO);
        }
}
