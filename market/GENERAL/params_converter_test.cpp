#include <market/idx/datacamp/miner/processors/offer_content_converter/field_converters.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/gtest.h>

/// Empty test
TEST(ParamsConverterTest, Empty) {
    NMiner::TDatacampOfferBatchProcessingContext context;
    NMiner::TDatacampOffer offer = MakeDefault();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);

    NMiner::FillOfferParams(offer, context, fixedTimestamp);
    EXPECT_TRUE(offer.GetBasicByColor().content().partner().actual().offer_params().param().empty());
}

/// Clear previous
TEST(ParamsConverterTest, ClearPrevious) {
    NMiner::TDatacampOfferBatchProcessingContext context;
    NMiner::TDatacampOffer offer = MakeDefault();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* actual_offer_params = offer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_actual()->mutable_offer_params();
    auto* param = actual_offer_params->add_param();
    param->set_name("from");
    param->set_value("original");
    param->set_unit("part");

    NMiner::FillOfferParams(offer, context, fixedTimestamp);
    EXPECT_TRUE(offer.GetBasicByColor().content().partner().actual().offer_params().param().empty());
}

/// Fill from original
TEST(ParamsConverterTest, FromOriginal) {
    NMiner::TDatacampOfferBatchProcessingContext context;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* original_offer_params = offer.mutable_content()->mutable_partner()->mutable_original()->mutable_offer_params();
    auto* param = original_offer_params->add_param();
    param->set_name("from");
    param->set_value("original");
    param->set_unit("part");

    NMiner::FillOfferParams(dcOffer, context, fixedTimestamp);
    EXPECT_FALSE(offer.content().partner().actual().offer_params().param().empty());
    ASSERT_EQ(offer.content().partner().actual().offer_params().param_size(), 1);
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).name(), "from");
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).value(), "original");
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).unit(), "part");
}

/// Vendor
TEST(ParamsConverterTest, Vendor) {
    NMiner::TDatacampOfferBatchProcessingContext context;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* original_content = offer.mutable_content()->mutable_partner()->mutable_original();
    original_content->mutable_vendor()->set_value("some_vendor");

    NMiner::FillOfferParams(dcOffer, context, fixedTimestamp);
    EXPECT_FALSE(offer.content().partner().actual().offer_params().param().empty());
    ASSERT_EQ(offer.content().partner().actual().offer_params().param_size(), 1);
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).name(), "vendor");
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).value(), "some_vendor");
    EXPECT_FALSE(offer.content().partner().actual().offer_params().param(0).has_unit());
}

/// Weight
TEST(ParamsConverterTest, Weight) {
    NMiner::TDatacampOfferBatchProcessingContext context;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* original_content = offer.mutable_content()->mutable_partner()->mutable_original();

    // Заполнено поле value_mg
    original_content->mutable_weight()->set_value_mg(123);

    NMiner::FillOfferParams(dcOffer, context, fixedTimestamp);
    EXPECT_FALSE(offer.content().partner().actual().offer_params().param().empty());
    ASSERT_EQ(offer.content().partner().actual().offer_params().param_size(), 1);
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).name(), "delivery_weight");
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).value(), "0.000123");
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).unit(), "кг");

    // Заполнено legacy поле grams
    original_content->mutable_weight()->clear_value_mg();
    original_content->mutable_weight()->set_grams(1024);

    NMiner::FillOfferParams(dcOffer, context, fixedTimestamp);
    ASSERT_EQ(offer.content().partner().actual().offer_params().param_size(), 1);
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).value(), "1.024");

    // Заполнены оба поля, приоритет отдаем value_mg
    original_content->mutable_weight()->set_value_mg(123);
    original_content->mutable_weight()->set_grams(456);

    NMiner::FillOfferParams(dcOffer, context, fixedTimestamp);
    ASSERT_EQ(offer.content().partner().actual().offer_params().param_size(), 1);
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).value(), "0.000123");
}

/// Length
TEST(ParamsConverterTest, Length) {
    NMiner::TDatacampOfferBatchProcessingContext context;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* original_content = offer.mutable_content()->mutable_partner()->mutable_original();
    original_content->mutable_dimensions()->set_length_mkm(1024000);

    NMiner::FillOfferParams(dcOffer, context, fixedTimestamp);
    EXPECT_FALSE(offer.content().partner().actual().offer_params().param().empty());
    ASSERT_EQ(offer.content().partner().actual().offer_params().param_size(), 1);
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).name(), "delivery_length");
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).value(), "102.4");
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).unit(), "см");
}

/// Width
TEST(ParamsConverterTest, Width) {
    NMiner::TDatacampOfferBatchProcessingContext context;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* original_content = offer.mutable_content()->mutable_partner()->mutable_original();
    original_content->mutable_dimensions()->set_width_mkm(51200);

    NMiner::FillOfferParams(dcOffer, context, fixedTimestamp);
    EXPECT_FALSE(offer.content().partner().actual().offer_params().param().empty());
    ASSERT_EQ(offer.content().partner().actual().offer_params().param_size(), 1);
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).name(), "delivery_width");
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).value(), "5.12");
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).unit(), "см");
}

/// Height
TEST(ParamsConverterTest, Height) {
    NMiner::TDatacampOfferBatchProcessingContext context;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* original_content = offer.mutable_content()->mutable_partner()->mutable_original();
    original_content->mutable_dimensions()->set_height_mkm(2560);

    NMiner::FillOfferParams(dcOffer, context, fixedTimestamp);
    EXPECT_FALSE(offer.content().partner().actual().offer_params().param().empty());
    ASSERT_EQ(offer.content().partner().actual().offer_params().param_size(), 1);
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).name(), "delivery_height");
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).value(), "0.256");
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).unit(), "см");
}

/// TextParams
TEST(ParamsConverterTest, TextParams) {
    NMiner::TDatacampOfferBatchProcessingContext context;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* original_content = offer.mutable_content()->mutable_partner()->mutable_original();
    original_content->mutable_text_params()->set_value("TeXtPaRaMs");

    NMiner::FillOfferParams(dcOffer, context, fixedTimestamp);
    EXPECT_FALSE(offer.content().partner().actual().offer_params().param().empty());
    ASSERT_EQ(offer.content().partner().actual().offer_params().param_size(), 1);
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).name(), "textParams");
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).value(), "TeXtPaRaMs");
    EXPECT_FALSE(offer.content().partner().actual().offer_params().param(0).has_unit());
}

/// LongTextParams
TEST(ParamsConverterTest, LongTextParams) {
    NMiner::TDatacampOfferBatchProcessingContext context;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* original_content = offer.mutable_content()->mutable_partner()->mutable_original();
    original_content->mutable_text_params()->set_value(TString(1549, 'A'));

    NMiner::FillOfferParams(dcOffer, context, fixedTimestamp);
    EXPECT_FALSE(offer.content().partner().actual().offer_params().param().empty());
    ASSERT_EQ(offer.content().partner().actual().offer_params().param_size(), 1);
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).name(), "textParams");
    EXPECT_EQ(offer.content().partner().actual().offer_params().param(0).value().size(), 600);
    EXPECT_STREQ(offer.content().partner().actual().offer_params().param(0).value(), (TString(600, 'A')));
    EXPECT_FALSE(offer.content().partner().actual().offer_params().param(0).has_unit());
}
