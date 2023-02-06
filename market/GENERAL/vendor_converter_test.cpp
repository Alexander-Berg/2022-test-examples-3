#include <market/idx/datacamp/miner/processors/offer_content_converter/field_converters.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/gtest.h>

/// Empty test
TEST(VendorConverterTest, Empty) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer offer = MakeDefault();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);

    NMiner::FillVendor(offer, capsFixer, fixedTimestamp);
    EXPECT_TRUE(offer.GetBasicByColor().content().partner().actual().vendor().value().empty());
}

/// Clear previous
TEST(VendorConverterTest, ClearPrevious) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer offer = MakeDefault();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* actualVendor = offer.GetBasicByColor().mutable_content()->mutable_partner()->mutable_actual()->mutable_vendor();
    actualVendor->set_value("old vendor");

    NMiner::FillVendor(offer, capsFixer, fixedTimestamp);
    EXPECT_TRUE(offer.GetBasicByColor().content().partner().actual().vendor().value().empty());
}

/// Fill from original
TEST(VendorConverterTest, FromOriginal) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& originalTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1689833911);
    auto* actualVendor = offer.mutable_content()->mutable_partner()->mutable_original()->mutable_vendor();
    actualVendor->set_value("original vendor");
    actualVendor->mutable_meta()->mutable_timestamp()->CopyFrom(originalTimestamp);

    NMiner::FillVendor(dcOffer, capsFixer, fixedTimestamp);
    EXPECT_EQ("original vendor", offer.content().partner().actual().vendor().value());
}

/// Fix capslock
TEST(VendorConverterTest, FixCapslock) {
    NMarket::NCapsLock::TCapsLockFixer capsFixer;
    NMiner::TDatacampOffer dcOffer = MakeDefault();
    auto& offer = dcOffer.GetBasicByColor();
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    auto* actualVendor = offer.mutable_content()->mutable_partner()->mutable_original()->mutable_vendor();
    actualVendor->set_value("ТЕКСТ ДЛЯ ДЕКАПИТАЛИЗАЦИИ");

    NMiner::FillVendor(dcOffer, capsFixer, fixedTimestamp);
    EXPECT_EQ("Текст ДЛЯ декапитализации", offer.content().partner().actual().vendor().value());
}
