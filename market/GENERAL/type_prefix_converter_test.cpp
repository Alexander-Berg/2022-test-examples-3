#include <market/idx/datacamp/miner/processors/offer_content_converter/field_converters.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/gtest.h>

/// Empty test
TEST(TypePrefixConverterTest, Empty) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer offer = MakeDefault();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);

    NMiner::FillTypePrefix(offer, capsFixer, fixedTimestamp);
    EXPECT_TRUE(offer.GetBasicByColor().content().partner().actual().type_prefix().value().empty());
}

/// Clear previous
TEST(TypePrefixConverterTest, ClearPrevious) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer offer = MakeDefault();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* actualTypePrefix = offer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_actual()->mutable_type_prefix();
    actualTypePrefix->set_value("old type prefix");

    NMiner::FillTypePrefix(offer, capsFixer, fixedTimestamp);
    EXPECT_TRUE(offer.GetBasicByColor().content().partner().actual().type_prefix().value().empty());
}

/// Fill from original
TEST(TypePrefixConverterTest, FromOriginal) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& originalTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1689833911);
    auto* actualTypePrefix = offer.mutable_content()->mutable_partner()->mutable_original()->mutable_type_prefix();
    actualTypePrefix->set_value("original type prefix");
    actualTypePrefix->mutable_meta()->mutable_timestamp()->CopyFrom(originalTimestamp);

    NMiner::FillTypePrefix(dcOffer, capsFixer, fixedTimestamp);
    EXPECT_EQ("original type prefix", offer.content().partner().actual().type_prefix().value());
}

/// Fix capslock
TEST(TypePrefixConverterTest, FixCapslock) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* actualTypePrefix = offer.mutable_content()->mutable_partner()->mutable_original()->mutable_type_prefix();
    actualTypePrefix->set_value("ТЕКСТ ДЛЯ ДЕКАПИТАЛИЗАЦИИ");

    NMiner::FillTypePrefix(dcOffer, capsFixer, fixedTimestamp);
    EXPECT_EQ("Текст ДЛЯ декапитализации", offer.content().partner().actual().type_prefix().value());
}
