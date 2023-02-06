#include <market/idx/datacamp/miner/processors/offer_content_converter/field_converters.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/gtest.h>

/// Empty test
TEST(ModelConverterTest, Empty) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer offer = MakeDefault();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);

    NMiner::FillModel(offer, capsFixer, fixedTimestamp);
    EXPECT_TRUE(offer.GetBasicByColor().content().partner().actual().model().value().empty());
}

/// Clear previous
TEST(ModelConverterTest, ClearPrevious) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer offer = MakeDefault();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* actualModel = offer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_actual()->mutable_model();
    actualModel->set_value("old model");

    NMiner::FillModel(offer, capsFixer, fixedTimestamp);
    EXPECT_TRUE(offer.GetBasicByColor().content().partner().actual().model().value().empty());
}

/// Fill from original
TEST(ModelConverterTest, FromOriginal) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* actualModel = offer.mutable_content()->mutable_partner()->mutable_original()->mutable_model();
    actualModel->set_value("original model");

    NMiner::FillModel(dcOffer, capsFixer, fixedTimestamp);
    EXPECT_EQ("original model", offer.content().partner().actual().model().value());
}

/// Fix capslock
TEST(ModelConverterTest, FixCapslock) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* actualModel = offer.mutable_content()->mutable_partner()->mutable_original()->mutable_model();
    actualModel->set_value("ТЕКСТ ДЛЯ ДЕКАПИТАЛИЗАЦИИ");

    NMiner::FillModel(dcOffer, capsFixer, fixedTimestamp);
    EXPECT_EQ("Текст ДЛЯ декапитализации", offer.content().partner().actual().model().value());
}

TEST(ModelConverterTest, FixCapslock2) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* actualModel = offer.mutable_content()->mutable_partner()->mutable_original()->mutable_model();
    actualModel->set_value("Пылесос TEFAL TW 3786 RA");

    NMiner::FillModel(dcOffer, capsFixer, fixedTimestamp);
    EXPECT_EQ("Пылесос TEFAL TW 3786 RA", offer.content().partner().actual().model().value());
}

TEST(ModelConverterTest, FixCapslock3) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* actualModel = offer.mutable_content()->mutable_partner()->mutable_original()->mutable_model();
    actualModel->set_value("Джинсы Skinny Fit");

    NMiner::FillModel(dcOffer, capsFixer, fixedTimestamp);
    EXPECT_EQ("Джинсы Skinny Fit", offer.content().partner().actual().model().value());
}
