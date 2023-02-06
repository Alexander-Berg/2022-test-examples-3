#include <market/idx/datacamp/miner/processors/offer_content_converter/field_converters.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/gtest.h>

/// Empty test
TEST(ConditionConverterTest, Empty) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer offer = MakeDefault();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);

    NMiner::FillCondition(offer, capsFixer, fixedTimestamp);
    EXPECT_TRUE(offer.GetBasicByColor().content().partner().actual().condition().reason().empty());
}

/// Clear previous
TEST(ConditionConverterTest, ClearPrevious) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer offer = MakeDefault();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* actualCondition = offer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_actual()->mutable_condition();
    actualCondition->set_reason("old condition reason");

    NMiner::FillCondition(offer, capsFixer, fixedTimestamp);
    EXPECT_TRUE(offer.GetBasicByColor().content().partner().actual().condition().reason().empty());
}

/// Fill from original
TEST(ConditionConverterTest, FromOriginal) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* originalCondition = offer.mutable_content()->mutable_partner()->mutable_original()->mutable_condition();
    originalCondition->set_reason("original condition reason");
    originalCondition->set_type(Market::DataCamp::Condition::USED);

    NMiner::FillCondition(dcOffer, capsFixer, fixedTimestamp);
    EXPECT_EQ("original condition reason", offer.content().partner().actual().condition().reason());
    EXPECT_EQ(
        static_cast<int>(Market::DataCamp::Condition::USED),
        static_cast<int>(offer.content().partner().actual().condition().type()));
}

/// Fix capslock
TEST(ConditionConverterTest, FixCapslock) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* originalCondition = offer.mutable_content()->mutable_partner()->mutable_original()->mutable_condition();
    originalCondition->set_reason("ТЕКСТ ДЛЯ ДЕКАПИТАЛИЗАЦИИ");

    NMiner::FillCondition(dcOffer, capsFixer, fixedTimestamp);
    EXPECT_EQ("Текст ДЛЯ декапитализации", offer.content().partner().actual().condition().reason());
}
