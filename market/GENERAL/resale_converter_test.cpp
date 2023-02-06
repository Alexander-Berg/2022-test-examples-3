#include <market/idx/datacamp/miner/processors/offer_content_converter/field_converters.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/config.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/gtest.h>

const auto ORIGINAL_TIMESTAMP = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
const auto FIXED_TIMESTAMP = google::protobuf::util::TimeUtil::SecondsToTimestamp(1689833911);
const TString ORIGINAL_RESALE_DESCRIPTION = "original resale description";
const NMiner::TOfferContentConverterConfig CONFIG("OFFER_CONTENT_CONVERTER");

/// Empty test
TEST(ResaleConverterTest, Empty) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer offer = MakeDefault();

    NMiner::FillResale(offer, capsFixer, CONFIG, FIXED_TIMESTAMP);
    EXPECT_TRUE(offer.GetBasicByColor().content().partner().actual().resale_description().value().empty());
}

/// Clear previous
TEST(ResaleConverterTest, ClearPrevious) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer offer = MakeDefault();
    auto* originalResale = offer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_actual()->mutable_resale_description();
    originalResale->set_value(ORIGINAL_RESALE_DESCRIPTION);

    NMiner::FillResale(offer, capsFixer, CONFIG, FIXED_TIMESTAMP);
    EXPECT_TRUE(offer.GetBasicByColor().content().partner().actual().resale_description().value().empty());
}

/// Fill from original
TEST(ResaleConverterTest, FromOriginal) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    auto* originalResale = offer.mutable_content()->mutable_partner()->mutable_original()->mutable_resale_description();
    originalResale->set_value(ORIGINAL_RESALE_DESCRIPTION);
    originalResale->mutable_meta()->mutable_timestamp()->CopyFrom(ORIGINAL_TIMESTAMP);

    NMiner::FillResale(dcOffer, capsFixer, CONFIG, FIXED_TIMESTAMP);
    EXPECT_EQ(ORIGINAL_RESALE_DESCRIPTION, offer.content().partner().actual().resale_description().value());
}

/// Fix long description
TEST(ResaleConverterTest, ShortenLongDescription) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    TString longOriginalDescription(CONFIG.ResaleDescriptionMaxLength, 'a');
    TString textToBeDeleted = "текст для удаления";
    auto& offer = dcOffer.GetBasicByColor();
    auto* originalResale = offer.mutable_content()->mutable_partner()->mutable_original()->mutable_resale_description();
    originalResale->set_value(longOriginalDescription + textToBeDeleted);
    originalResale->mutable_meta()->mutable_timestamp()->CopyFrom(ORIGINAL_TIMESTAMP);

    NMiner::FillResale(dcOffer, capsFixer, CONFIG, FIXED_TIMESTAMP);
    EXPECT_EQ(longOriginalDescription, offer.content().partner().actual().resale_description().value());
}

/// Fix capslock
TEST(ResaleConverterTest, FixCapslock) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    auto* originalResale = offer.mutable_content()->mutable_partner()->mutable_original()->mutable_resale_description();
    originalResale->set_value("ТЕКСТ ДЛЯ ДЕКАПИТАЛИЗАЦИИ");

    NMiner::FillResale(dcOffer, capsFixer, CONFIG, FIXED_TIMESTAMP);
    EXPECT_EQ("Текст ДЛЯ декапитализации", offer.content().partner().actual().resale_description().value());
}
