#include <market/idx/datacamp/miner/processors/delivery_validator/validation.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <library/cpp/testing/unittest/gtest.h>

NMiner::TDatacampOffer CreateOfferFromBasicAndService(Market::DataCamp::Offer& basic,
                                                      Market::DataCamp::Offer& service,
                                                      Market::DataCamp::MarketColor color,
                                                      const TMaybe<bool>& deliveryFlag,
                                                      const TMaybe<bool>& pickupFlag,
                                                      const TMaybe<bool>& storeFlag,
                                                      const TMaybe<uint64_t>& pickupBucket,
                                                      bool writeToService)
{
    service.mutable_meta()->set_rgb(color);
    (*service.mutable_meta()->mutable_platforms())[color] = true;
    NMiner::TDatacampOffer dcOffer(&basic, &service);
    auto& offerForDelivery = writeToService ? dcOffer.GetService() : dcOffer.GetBasicByColor();

    if (deliveryFlag) {
        offerForDelivery.mutable_delivery()->mutable_partner()->mutable_actual()->mutable_delivery()->set_flag(*deliveryFlag);
    }
    if (pickupFlag) {
        offerForDelivery.mutable_delivery()->mutable_partner()->mutable_actual()->mutable_pickup()->set_flag(*pickupFlag);
    }
    if (storeFlag) {
        offerForDelivery.mutable_delivery()->mutable_partner()->mutable_actual()->mutable_store()->set_flag(*storeFlag);
    }
    if (pickupBucket) {
        offerForDelivery.mutable_delivery()->mutable_calculator()->add_pickup_bucket_ids(*pickupBucket);
    }
    return dcOffer;
}

void TestBasicAndService(const NMarket::NDataCamp::TSupplierRecord& shopsDat,
                         const NMiner::TShopOutletsInfo& shopInfo,
                         const TMaybe<bool>& deliveryFlag,
                         const TMaybe<bool>& pickupFlag,
                         const TMaybe<bool>& storeFlag,
                         const TMaybe<uint64_t>& pickupBucket,
                         bool expectedResult)
{
    for (const auto color: {Market::DataCamp::MarketColor::WHITE, Market::DataCamp::MarketColor::BLUE}) {
        for (const bool writeToService: {true, false}) {
            Market::DataCamp::Offer basic;
            Market::DataCamp::Offer service;
            auto offer = CreateOfferFromBasicAndService(basic, service, color, deliveryFlag, pickupFlag, storeFlag, pickupBucket, writeToService);
            NMarket::TDatacampOfferBatchProcessingContext context {};
            auto config = NMiner::TDeliveryValidatorConfig("");
            auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();
            auto result = CheckOfferDelivery(offer, shopsDat, shopInfo, context, config, timestamp);
            if (!writeToService && color == Market::DataCamp::MarketColor::WHITE) {
                ASSERT_EQ(result, false);
            } else {
                ASSERT_EQ(result, expectedResult);
            }
        }
    }
}

TEST (DeliveryValidator, NoDelivery49D) {
    NMiner::TDatacampOffer offer = MakeDefault();
    NMiner::TShopOutletsInfo shopInfo;
    // it is: hasDelivery = false;
    NMarket::NDataCamp::TSupplierRecord shopsDat;

    NMarket::TDatacampOfferBatchProcessingContext context {};
    auto config = NMiner::TDeliveryValidatorConfig("");
    auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();
    auto result = CheckOfferDelivery(offer, shopsDat, shopInfo, context, config, timestamp);
    ASSERT_FALSE(result);
    TestBasicAndService(shopsDat, shopInfo, Nothing(), Nothing(), Nothing(), Nothing(), false);
}

TEST (DeliveryValidator, NoDelivery49E) {
    NMiner::TDatacampOffer offer = MakeDefault();

    NMiner::TShopOutletsInfo shopInfo;

    // it is: hasDelivery = true;
    offer.GetService().mutable_delivery()->mutable_calculator()->add_pickup_bucket_ids(777);
    offer.GetService().mutable_delivery()->mutable_market()->mutable_use_yml_delivery()->set_flag(true);

    NMarket::NDataCamp::TSupplierRecord shopsDat;

    NMarket::TDatacampOfferBatchProcessingContext context {};
    auto config = NMiner::TDeliveryValidatorConfig("");
    auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();
    auto result = CheckOfferDelivery(offer, shopsDat, shopInfo, context, config, timestamp);
    ASSERT_FALSE(result);
    TestBasicAndService(shopsDat, shopInfo, Nothing(), Nothing(), Nothing(), 777, false);
}

TEST (DeliveryValidator, NoDelivery49I) {
    NMiner::TDatacampOffer offer = MakeDefault();

    NMiner::TShopOutletsInfo shopInfo;

    // it is: hasDelivery = false;
    NMarket::NDataCamp::TSupplierRecord shopsDat;

    // deliveryForbidden = true, pickupForbidden = true, storeForbidden = true
    auto* actual = offer.GetService().mutable_delivery()->mutable_partner()->mutable_actual();
    actual->mutable_delivery()->set_flag(true);
    actual->mutable_pickup()->set_flag(true);
    actual->mutable_store()->set_flag(true);

    NMarket::TDatacampOfferBatchProcessingContext context {};
    auto config = NMiner::TDeliveryValidatorConfig("");
    auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();
    auto result = CheckOfferDelivery(offer, shopsDat, shopInfo, context, config, timestamp);
    ASSERT_FALSE(result);
    TestBasicAndService(shopsDat, shopInfo, true, true, true, Nothing(), false);
}

TEST (DeliveryValidator, NoDelivery49A) {
    NMiner::TDatacampOffer offer = MakeDefault();

    NMiner::TShopOutletsInfo shopInfo;

    // deliveryForbidden = true, pickupForbidden = true, storeForbidden = false
    auto* actual = offer.GetService().mutable_delivery()->mutable_partner()->mutable_actual();
    actual->mutable_delivery()->set_flag(false);
    actual->mutable_pickup()->set_flag(false);
    actual->mutable_store()->set_flag(true);

    // it is: hasDelivery = false;
    NMarket::NDataCamp::TSupplierRecord shopsDat;


    NMarket::TDatacampOfferBatchProcessingContext context {};
    auto config = NMiner::TDeliveryValidatorConfig("");
    auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();
    auto result = CheckOfferDelivery(offer, shopsDat, shopInfo, context, config, timestamp);
    ASSERT_FALSE(result);
    TestBasicAndService(shopsDat, shopInfo, false, false, true, Nothing(), false);
}

TEST (DeliveryValidator, NoDelivery49B) {
    NMiner::TDatacampOffer offer = MakeDefault();

    NMiner::TShopOutletsInfo shopInfo;

    // deliveryForbidden = true, pickupForbidden = true, storeForbidden = false
    auto* actual = offer.GetService().mutable_delivery()->mutable_partner()->mutable_actual();
    actual->mutable_delivery()->set_flag(false);
    actual->mutable_pickup()->set_flag(false);
    actual->mutable_store()->set_flag(true);

    // it is: hasDelivery = true;
    offer.GetService().mutable_delivery()->mutable_calculator()->add_pickup_bucket_ids(777);
    offer.GetService().mutable_delivery()->mutable_market()->mutable_use_yml_delivery()->set_flag(true);

    NMarket::NDataCamp::TSupplierRecord shopsDat;


    NMarket::TDatacampOfferBatchProcessingContext context{};
    auto config = NMiner::TDeliveryValidatorConfig("");
    auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();
    auto result = CheckOfferDelivery(offer, shopsDat, shopInfo, context, config, timestamp);
    ASSERT_FALSE(result);
    TestBasicAndService(shopsDat, shopInfo, false, false, true, 777, false);
}

TEST (DeliveryValidator, NoDelivery49K) {
    NMiner::TDatacampOffer offer = MakeDefault();

    NMiner::TShopOutletsInfo shopInfo;

    // deliveryForbidden = true, pickupForbidden = false, storeForbidden = true
    auto* actual = offer.GetService().mutable_delivery()->mutable_partner()->mutable_actual();
    actual->mutable_delivery()->set_flag(false);
    actual->mutable_pickup()->set_flag(true);
    actual->mutable_store()->set_flag(false);

    // it is: hasDelivery = true;
    offer.GetService().mutable_delivery()->mutable_calculator()->add_pickup_bucket_ids(777);
    offer.GetService().mutable_delivery()->mutable_market()->mutable_use_yml_delivery()->set_flag(true);

    NMarket::NDataCamp::TSupplierRecord shopsDat;

    NMarket::TDatacampOfferBatchProcessingContext context{};
    auto config = NMiner::TDeliveryValidatorConfig("");
    auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();
    auto result = CheckOfferDelivery(offer, shopsDat, shopInfo, context, config, timestamp);
    ASSERT_FALSE(result);
    TestBasicAndService(shopsDat, shopInfo, false, true, false, 777, false);
}

TEST (DeliveryValidator, NoDelivery49L) {
    NMiner::TDatacampOffer offer = MakeDefault();

    NMiner::TShopOutletsInfo shopInfo;

    // deliveryForbidden = true, pickupForbidden = false, storeForbidden = true
    auto* actual = offer.GetService().mutable_delivery()->mutable_partner()->mutable_actual();
    actual->mutable_delivery()->set_flag(false);
    actual->mutable_pickup()->set_flag(true);
    actual->mutable_store()->set_flag(false);

    // it is: hasDelivery = false;
    NMarket::NDataCamp::TSupplierRecord shopsDat;

    NMarket::TDatacampOfferBatchProcessingContext context{};
    auto config = NMiner::TDeliveryValidatorConfig("");
    auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();
    auto result = CheckOfferDelivery(offer, shopsDat, shopInfo, context, config, timestamp);
    ASSERT_FALSE(result);
    TestBasicAndService(shopsDat, shopInfo, false, true, false, Nothing(), false);
}

TEST (DeliveryValidator, NoDelivery49C) {
    NMiner::TDatacampOffer offer = MakeDefault();

    NMiner::TShopOutletsInfo shopInfo;

    // deliveryForbidden = false, pickupForbidden = true, storeForbidden = true
    auto* actual = offer.GetService().mutable_delivery()->mutable_partner()->mutable_actual();
    actual->mutable_delivery()->set_flag(true);
    actual->mutable_pickup()->set_flag(false);
    actual->mutable_store()->set_flag(false);

    // it is: hasDelivery = false;
    NMarket::NDataCamp::TSupplierRecord shopsDat;

    NMarket::TDatacampOfferBatchProcessingContext context{};
    auto config = NMiner::TDeliveryValidatorConfig("");
    auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();
    auto result = CheckOfferDelivery(offer, shopsDat, shopInfo, context, config, timestamp);
    ASSERT_FALSE(result);
    TestBasicAndService(shopsDat, shopInfo, true, false, false, Nothing(), false);
}

TEST (DeliveryValidator, NoDelivery49M) {
    NMiner::TDatacampOffer offer = MakeDefault();

    NMiner::TShopOutletsInfo shopInfo;

    // deliveryForbidden = false, pickupForbidden = false, storeForbidden = true
    auto* actual = offer.GetService().mutable_delivery()->mutable_partner()->mutable_actual();
    actual->mutable_delivery()->set_flag(true);
    actual->mutable_pickup()->set_flag(true);
    actual->mutable_store()->set_flag(false);

    // it is: hasDelivery = false;
    NMarket::NDataCamp::TSupplierRecord shopsDat;

    NMarket::TDatacampOfferBatchProcessingContext context{};
    auto config = NMiner::TDeliveryValidatorConfig("");
    auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();
    auto result = CheckOfferDelivery(offer, shopsDat, shopInfo, context, config, timestamp);
    ASSERT_FALSE(result);
    TestBasicAndService(shopsDat, shopInfo, true, true, false, Nothing(), false);
}

TEST (DeliveryValidator, NoDelivery49G) {
    NMiner::TDatacampOffer offer = MakeDefault();

    NMiner::TShopOutletsInfo shopInfo;

    // deliveryForbidden = true, pickupForbidden = false, storeForbidden = false
    auto* actual = offer.GetService().mutable_delivery()->mutable_partner()->mutable_actual();
    actual->mutable_delivery()->set_flag(false);
    actual->mutable_pickup()->set_flag(true);
    actual->mutable_store()->set_flag(true);

    // it is: hasDelivery = false;
    NMarket::NDataCamp::TSupplierRecord shopsDat;

    NMarket::TDatacampOfferBatchProcessingContext context{};
    auto config = NMiner::TDeliveryValidatorConfig("");
    auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();
    auto result = CheckOfferDelivery(offer, shopsDat, shopInfo, context, config, timestamp);
    ASSERT_FALSE(result);
    TestBasicAndService(shopsDat, shopInfo, false, true, true, Nothing(), false);
}

TEST (DeliveryValidator, NoDelivery49H) {
    NMiner::TDatacampOffer offer = MakeDefault();

    NMiner::TShopOutletsInfo shopInfo;

    // deliveryForbidden = true, pickupForbidden = false, storeForbidden = false
    auto* actual = offer.GetService().mutable_delivery()->mutable_partner()->mutable_actual();
    actual->mutable_delivery()->set_flag(false);
    actual->mutable_pickup()->set_flag(true);
    actual->mutable_store()->set_flag(true);

    // it is: hasDelivery = true;
    offer.GetService().mutable_delivery()->mutable_calculator()->add_pickup_bucket_ids(777);
    offer.GetService().mutable_delivery()->mutable_market()->mutable_use_yml_delivery()->set_flag(true);

    NMarket::NDataCamp::TSupplierRecord shopsDat;

    NMarket::TDatacampOfferBatchProcessingContext context{};
    auto config = NMiner::TDeliveryValidatorConfig("");
    auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();
    auto result = CheckOfferDelivery(offer, shopsDat, shopInfo, context, config, timestamp);
    ASSERT_FALSE(result);
    TestBasicAndService(shopsDat, shopInfo, false, true, true, 777, false);
}

TEST (DeliveryValidator, NoDelivery49F) {
    NMiner::TDatacampOffer offer = MakeDefault();

    NMiner::TShopOutletsInfo shopInfo;

    // deliveryForbidden = false, pickupForbidden = true, storeForbidden = false
    auto* actual = offer.GetService().mutable_delivery()->mutable_partner()->mutable_actual();
    actual->mutable_delivery()->set_flag(true);
    actual->mutable_pickup()->set_flag(false);
    actual->mutable_store()->set_flag(true);

    // it is: hasDelivery = false;
    NMarket::NDataCamp::TSupplierRecord shopsDat;

    NMarket::TDatacampOfferBatchProcessingContext context{};
    auto config = NMiner::TDeliveryValidatorConfig("");
    auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();
    auto result = CheckOfferDelivery(offer, shopsDat, shopInfo, context, config, timestamp);
    ASSERT_FALSE(result);
    TestBasicAndService(shopsDat, shopInfo, true, false, true, Nothing(), false);
}

TEST (DeliveryValidator, OfferOKDeliveryValidation) {
    NMiner::TDatacampOffer offer = MakeDefault();

    NMiner::TShopOutletsInfo shopInfo {
        .HasPickupOutlet=true,
        .HasStoreOutlet=true
    };

    // deliveryForbidden = false, pickupForbidden = false, storeForbidden = false
    auto* actual = offer.GetService().mutable_delivery()->mutable_partner()->mutable_actual();
    actual->mutable_delivery()->set_flag(true);
    actual->mutable_pickup()->set_flag(false);
    actual->mutable_store()->set_flag(true);

    // it is: hasDelivery = true;
    offer.GetService().mutable_delivery()->mutable_calculator()->add_pickup_bucket_ids(777);
    offer.GetService().mutable_delivery()->mutable_market()->mutable_use_yml_delivery()->set_flag(true);

    NMarket::NDataCamp::TSupplierRecord shopsDat;

    NMarket::TDatacampOfferBatchProcessingContext context{};
    auto config = NMiner::TDeliveryValidatorConfig("");
    auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();
    auto result = CheckOfferDelivery(offer, shopsDat, shopInfo, context, config, timestamp);
    ASSERT_TRUE(result);
    TestBasicAndService(shopsDat, shopInfo, true, false, true, 777, true);
}

TEST (DeliveryValidator, DisableDsbsWithLongDelivery) {
    // Проверяем, что при отсутствии среди опций доставки хотя бы одной с daysMax <= DaysUnknownThreshold, dsbs оффер скрывается и проставляется признак available = false
    const auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();

    auto makeLongDeliveryOffer = [&](Market::DataCamp::Offer& basic,
                                              Market::DataCamp::Offer& service,
                                              bool is_dsbs = false, bool has_valid_delivery = false) {
        service.mutable_delivery()->mutable_market()->mutable_use_yml_delivery()->set_flag(true);
        service.mutable_meta()->set_rgb(Market::DataCamp::MarketColor::BLUE);
        auto* actual = service.mutable_delivery()->mutable_partner()->mutable_actual();
        actual->mutable_delivery()->set_flag(true);
        actual->mutable_delivery()->mutable_meta()->mutable_timestamp()->CopyFrom(timestamp);

        auto options = actual->mutable_delivery_options()->mutable_options();
        {
            // long option
            auto option = options->Add();
            option->SetCost(300);
            option->SetDaysMin(61);
            option->SetDaysMax(64);
        }

        if (has_valid_delivery) {
            // OK
            auto option = options->Add();
            option->SetCost(1);
            option->SetDaysMin(2);
            option->SetDaysMax(4);
        }

        if (is_dsbs) {
            service.mutable_partner_info()->set_is_dsbs(is_dsbs);
        }

        NMiner::TDatacampOffer dcOffer(&basic, &service);
        return dcOffer;
    };

    Market::DataCamp::Offer basic_1;
    Market::DataCamp::Offer service_1;
    auto offer_1 = makeLongDeliveryOffer(basic_1, service_1, true, true); // dsbs оффер, есть короткая опция => оффер не скрыт

    Market::DataCamp::Offer basic_2;
    Market::DataCamp::Offer service_2;
    auto offer_2 = makeLongDeliveryOffer(basic_2, service_2); // не dsbs оффер только с длинными опциями => оффер не скрыт, но available == false

    Market::DataCamp::Offer basic_3;
    Market::DataCamp::Offer service_3;
    auto offer_3 = makeLongDeliveryOffer(basic_3, service_3, true); // dsbs оффер только с длинными опциями => оффер скрыт, available == false

    Market::DataCamp::Offer basic_4;
    Market::DataCamp::Offer service_4;
    auto offer_4 = makeLongDeliveryOffer(basic_4, service_4, true); // dsbs оффер только с длинными опциями, но без yml delivery => оффер не скрыт
    offer_4.GetService().mutable_delivery()->mutable_market()->clear_use_yml_delivery();
    offer_4.GetService().mutable_delivery()->mutable_calculator()->add_pickup_bucket_ids(777);

    NMarket::TDatacampOfferBatchProcessingContext processingContext;
    NMarket::NDataCamp::TSupplierRecord shopsDat;
    NMiner::TShopOutletsInfo shopInfo;
    NMarket::TDatacampOfferBatchProcessingContext context{};
    auto config = NMiner::TDeliveryValidatorConfig("");
    config.DisableDsbsWithLongDelivery = true;

    ASSERT_TRUE(CheckOfferDelivery(offer_1, shopsDat, shopInfo, context, config, timestamp));
    ASSERT_FALSE(offer_1.GetService().delivery().partner().actual().available().has_flag());

    ASSERT_TRUE(CheckOfferDelivery(offer_2, shopsDat, shopInfo, context, config, timestamp));
    ASSERT_TRUE(offer_2.GetService().delivery().partner().actual().available().has_flag());
    ASSERT_FALSE(offer_2.GetService().delivery().partner().actual().available().flag());

    ASSERT_FALSE(CheckOfferDelivery(offer_3, shopsDat, shopInfo, context, config, timestamp));
    ASSERT_TRUE(offer_3.GetService().delivery().partner().actual().available().has_flag());
    ASSERT_FALSE(offer_3.GetService().delivery().partner().actual().available().flag());

    ASSERT_TRUE(CheckOfferDelivery(offer_4, shopsDat, shopInfo, context, config, timestamp));
    ASSERT_FALSE(offer_4.GetService().delivery().partner().actual().available().has_flag());
}
