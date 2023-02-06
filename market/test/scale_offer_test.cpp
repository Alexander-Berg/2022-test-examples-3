#include <market/idx/cron/scale_datacamp_dump/src/scale_offer.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>

#include <util/generic/vector.h>

namespace {
    using ::delivery_calc::mbi::BucketInfo;
    using ::google::protobuf::int64;
    using ::google::protobuf::RepeatedField;
    using ::google::protobuf::RepeatedPtrField;
    using ::Market::DataCamp::DeliveryCalculatorOptions;
    using ::NMarket::NScale::ScaleDataCampOffer;
    using TOldBucketsSetter = RepeatedField<int64>* (DeliveryCalculatorOptions::*)();
    using TBucketsSetter = RepeatedPtrField<BucketInfo>* (DeliveryCalculatorOptions::*)();

    void SetOfferID(TDataCampOfferRaw& offerRaw, const TString& offerID) {
        offerRaw.set_offer_id(offerID);

        Market::DataCamp::Offer* offer = offerRaw.mutable_offer();
        Market::DataCamp::OfferIdentifiers* identifiers = offer->mutable_identifiers();
        identifiers->set_offer_id(offerID);
    }

    void SetWareMD5(TDataCampOfferRaw& offerRaw, const TString& wareMD5) {
        Market::DataCamp::Offer* offer = offerRaw.mutable_offer();
        Market::DataCamp::OfferIdentifiers* identifiers = offer->mutable_identifiers();
        Market::DataCamp::OfferExtraIdentifiers* extraIdentifiers = identifiers->mutable_extra();
        extraIdentifiers->set_ware_md5(wareMD5);
    }

    void SetTitle(TDataCampOfferRaw& offerRaw, const TString& title) {
        Market::DataCamp::Offer* offer = offerRaw.mutable_offer();
        Market::DataCamp::OfferContent* content = offer->mutable_content();
        Market::DataCamp::PartnerContent* partner = content->mutable_partner();
        Market::DataCamp::ProcessedSpecification* actual = partner->mutable_actual();
        Market::DataCamp::StringValue* titleValue = actual->mutable_title();
        titleValue->set_value(title);
    }

    void SetOldBuckets(TDataCampOfferRaw& offerRaw,
                       const TVector<i64>& buckets,
                       TOldBucketsSetter setter) {
        Market::DataCamp::Offer* offer = offerRaw.mutable_offer();
        Market::DataCamp::MarketDelivery* marketDelivery = offer->mutable_delivery()->mutable_market();
        Market::DataCamp::DeliveryCalculatorOptions* deliveryOptions = marketDelivery->mutable_calculator();
        (deliveryOptions->*setter)()->Add(buckets.begin(), buckets.end());
    }

    void SetOldCourier(TDataCampOfferRaw& offerRaw, const TVector<i64>& buckets) {
        SetOldBuckets(offerRaw, buckets, &DeliveryCalculatorOptions::mutable_delivery_bucket_ids);
    }

    void SetOldPickup(TDataCampOfferRaw& offerRaw, const TVector<i64>& buckets) {
        SetOldBuckets(offerRaw, buckets, &DeliveryCalculatorOptions::mutable_pickup_bucket_ids);
    }

    void SetOldPost(TDataCampOfferRaw& offerRaw, const TVector<i64>& buckets) {
        SetOldBuckets(offerRaw, buckets, &DeliveryCalculatorOptions::mutable_post_bucket_ids);
    }

    void SetBucketInfo(TDataCampOfferRaw& offerRaw, const TVector<BucketInfo>& buckets, TBucketsSetter setter) {
        Market::DataCamp::Offer* offer = offerRaw.mutable_offer();
        Market::DataCamp::MarketDelivery* marketDelivery = offer->mutable_delivery()->mutable_market();
        Market::DataCamp::DeliveryCalculatorOptions* deliveryOptions = marketDelivery->mutable_calculator();
        (deliveryOptions->*setter)()->Add(buckets.begin(), buckets.end());
    }

    void SetCourier(TDataCampOfferRaw& offerRaw, const TVector<BucketInfo>& buckets) {
        SetBucketInfo(offerRaw, buckets, &DeliveryCalculatorOptions::mutable_courier_buckets_info);
    }

    void SetPickup(TDataCampOfferRaw& offerRaw, const TVector<BucketInfo>& buckets) {
        SetBucketInfo(offerRaw, buckets, &DeliveryCalculatorOptions::mutable_pickup_buckets_info);
    }

    void SetPost(TDataCampOfferRaw& offerRaw, const TVector<BucketInfo>& buckets) {
        SetBucketInfo(offerRaw, buckets, &DeliveryCalculatorOptions::mutable_post_buckets_info);
    }

    TDataCampOfferRaw PrepareTestOffer() {
        TDataCampOfferRaw offerRaw;
        SetOfferID(offerRaw, "offer_for_scale");
        SetWareMD5(offerRaw, "8uDi4TxSDXy0ym5pggF-mQ");
        SetTitle(offerRaw, "Title of offer for scale");
        return offerRaw;
    }

    std::mt19937 PrepareRandomEngine() {
        return std::mt19937(1646868);
    }

    const THashMap<i64, TVector<i64>>& GetBucketRenumber() {
        static THashMap<i64, TVector<i64>> bucketRenumber{
            {1, {101}},
        };
        return bucketRenumber;
    }
}

TEST(ScaleOffer, ZeroGuids) {
    TVector<TString> guids;
    std::mt19937 randomEngine = PrepareRandomEngine();
    TVector<TDataCampOfferRaw> offers = ScaleDataCampOffer(PrepareTestOffer(), guids, GetBucketRenumber(), randomEngine);
    ASSERT_EQ(offers.size(), 0u);
}

TEST(ScaleOffer, OneGuid) {
    TVector<TString> guids{"some_guid"};
    std::mt19937 randomEngine = PrepareRandomEngine();
    TVector<TDataCampOfferRaw> offers = ScaleDataCampOffer(PrepareTestOffer(), guids, GetBucketRenumber(), randomEngine);
    ASSERT_EQ(offers.size(), 1u);
    TDataCampOfferRaw expected;
    SetOfferID(expected, "offer_for_scale_test_some_guid");
    SetWareMD5(expected, "4iOlmPGFx4hJzJaG5gNgkQ");
    SetTitle(expected, "Scaled offer 0 Title of offer for scale");
    EXPECT_THAT(offers[0], NGTest::EqualsProto(expected));
}

TEST(ScaleOffer, ManyGuids) {
    TVector<TString> guids{"some_guid", "another_guid"};
    std::mt19937 randomEngine = PrepareRandomEngine();
    TVector<TDataCampOfferRaw> offers = ScaleDataCampOffer(PrepareTestOffer(), guids, GetBucketRenumber(), randomEngine);
    ASSERT_EQ(offers.size(), 2u);

    TDataCampOfferRaw expectedFirst;
    SetOfferID(expectedFirst, "offer_for_scale_test_some_guid");
    SetWareMD5(expectedFirst, "4iOlmPGFx4hJzJaG5gNgkQ");
    SetTitle(expectedFirst, "Scaled offer 0 Title of offer for scale");
    EXPECT_THAT(offers[0], NGTest::EqualsProto(expectedFirst));

    TDataCampOfferRaw expectedSecond;
    SetOfferID(expectedSecond, "offer_for_scale_test_another_guid");
    SetWareMD5(expectedSecond, "SqbYUVCzEfTp1R2hjp0lyQ");
    SetTitle(expectedSecond, "Scaled offer 1 Title of offer for scale");
    EXPECT_THAT(offers[1], NGTest::EqualsProto(expectedSecond));
}

TEST(ScaleOffer, RenumberOldCourier) {
    TVector<TString> guids{"some_guid"};
    TDataCampOfferRaw offer = PrepareTestOffer();
    SetOldCourier(offer, {1, 2});
    std::mt19937 randomEngine = PrepareRandomEngine();
    TVector<TDataCampOfferRaw> offers = ScaleDataCampOffer(offer, guids, GetBucketRenumber(), randomEngine);
    ASSERT_EQ(offers.size(), 1u);
    TDataCampOfferRaw expected;
    SetOldCourier(expected, {101, 2});
    SetOfferID(expected, "offer_for_scale_test_some_guid");
    SetWareMD5(expected, "4iOlmPGFx4hJzJaG5gNgkQ");
    SetTitle(expected, "Scaled offer 0 Title of offer for scale");
    EXPECT_THAT(offers[0], NGTest::EqualsProto(expected));
}

TEST(ScaleOffer, RenumberOldPickup) {
    TVector<TString> guids{"some_guid"};
    TDataCampOfferRaw offer = PrepareTestOffer();
    SetOldPickup(offer, {1});
    std::mt19937 randomEngine = PrepareRandomEngine();
    TVector<TDataCampOfferRaw> offers = ScaleDataCampOffer(offer, guids, GetBucketRenumber(), randomEngine);
    ASSERT_EQ(offers.size(), 1u);
    TDataCampOfferRaw expected;
    SetOldPickup(expected, {101});
    SetOfferID(expected, "offer_for_scale_test_some_guid");
    SetWareMD5(expected, "4iOlmPGFx4hJzJaG5gNgkQ");
    SetTitle(expected, "Scaled offer 0 Title of offer for scale");
    EXPECT_THAT(offers[0], NGTest::EqualsProto(expected));
}

TEST(ScaleOffer, RenumberOldPost) {
    TVector<TString> guids{"some_guid"};
    TDataCampOfferRaw offer = PrepareTestOffer();
    SetOldPost(offer, {3});
    std::mt19937 randomEngine = PrepareRandomEngine();
    TVector<TDataCampOfferRaw> offers = ScaleDataCampOffer(offer, guids, GetBucketRenumber(), randomEngine);
    ASSERT_EQ(offers.size(), 1u);
    TDataCampOfferRaw expected;
    SetOldPost(expected, {3});
    SetOfferID(expected, "offer_for_scale_test_some_guid");
    SetWareMD5(expected, "4iOlmPGFx4hJzJaG5gNgkQ");
    SetTitle(expected, "Scaled offer 0 Title of offer for scale");
    EXPECT_THAT(offers[0], NGTest::EqualsProto(expected));
}

TEST(ScaleOffer, RenumberCourier) {
    TVector<TString> guids{"some_guid"};
    TDataCampOfferRaw offer = PrepareTestOffer();
    BucketInfo preparedBucketInfo;
    preparedBucketInfo.set_bucket_id(1);
    preparedBucketInfo.set_is_new(true);
    SetCourier(offer, {preparedBucketInfo});
    std::mt19937 randomEngine = PrepareRandomEngine();
    TVector<TDataCampOfferRaw> offers = ScaleDataCampOffer(offer, guids, GetBucketRenumber(), randomEngine);
    ASSERT_EQ(offers.size(), 1u);
    TDataCampOfferRaw expected;
    BucketInfo expectedBucketInfo;
    expectedBucketInfo.set_bucket_id(101);
    expectedBucketInfo.set_is_new(true);
    SetCourier(expected, {expectedBucketInfo});
    SetOfferID(expected, "offer_for_scale_test_some_guid");
    SetWareMD5(expected, "4iOlmPGFx4hJzJaG5gNgkQ");
    SetTitle(expected, "Scaled offer 0 Title of offer for scale");
    EXPECT_THAT(offers[0], NGTest::EqualsProto(expected));
}

TEST(ScaleOffer, RenumberPickup) {
    TVector<TString> guids{"some_guid"};
    TDataCampOfferRaw offer = PrepareTestOffer();
    BucketInfo preparedBucketInfo;
    preparedBucketInfo.set_bucket_id(1);
    preparedBucketInfo.add_region_availability_modifiers_ids(1337);
    SetPickup(offer, {preparedBucketInfo, preparedBucketInfo});
    std::mt19937 randomEngine = PrepareRandomEngine();
    TVector<TDataCampOfferRaw> offers = ScaleDataCampOffer(offer, guids, GetBucketRenumber(), randomEngine);
    ASSERT_EQ(offers.size(), 1u);
    TDataCampOfferRaw expected;
    BucketInfo expectedBucketInfo;
    expectedBucketInfo.set_bucket_id(101);
    expectedBucketInfo.add_region_availability_modifiers_ids(1337);
    SetPickup(expected, {expectedBucketInfo, expectedBucketInfo});
    SetOfferID(expected, "offer_for_scale_test_some_guid");
    SetWareMD5(expected, "4iOlmPGFx4hJzJaG5gNgkQ");
    SetTitle(expected, "Scaled offer 0 Title of offer for scale");
    EXPECT_THAT(offers[0], NGTest::EqualsProto(expected));
}

TEST(ScaleOffer, RenumberPost) {
    TVector<TString> guids{"some_guid"};
    TDataCampOfferRaw offer = PrepareTestOffer();
    BucketInfo preparedBucketInfo;
    preparedBucketInfo.set_bucket_id(1);
    preparedBucketInfo.set_program(delivery_calc::mbi::ProgramType::DAAS);
    SetPost(offer, {preparedBucketInfo, BucketInfo()});
    std::mt19937 randomEngine = PrepareRandomEngine();
    TVector<TDataCampOfferRaw> offers = ScaleDataCampOffer(offer, guids, GetBucketRenumber(), randomEngine);
    ASSERT_EQ(offers.size(), 1u);
    TDataCampOfferRaw expected;
    BucketInfo expectedBucketInfo;
    expectedBucketInfo.set_bucket_id(101);
    expectedBucketInfo.set_program(delivery_calc::mbi::ProgramType::DAAS);
    SetPost(expected, {expectedBucketInfo, BucketInfo()});
    SetOfferID(expected, "offer_for_scale_test_some_guid");
    SetWareMD5(expected, "4iOlmPGFx4hJzJaG5gNgkQ");
    SetTitle(expected, "Scaled offer 0 Title of offer for scale");
    EXPECT_THAT(offers[0], NGTest::EqualsProto(expected));
}
