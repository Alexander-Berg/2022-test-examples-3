#include <library/cpp/testing/unittest/gtest.h>
#include <tuple>
#include <market/idx/library/validators/title.h>
#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>


void DoCheck(const Market::DataCamp::Offer& offer,
             bool expectedRC,
             const std::vector<NMarket::NOfferError::TMessageGenerator> &expectedMessages
) {
    std::vector<NMarket::NOfferError::TMessageGenerator> actualMessages;
    bool rc = NValidators::ValidateTitle(offer, [&actualMessages](const auto &msg) {
        actualMessages.push_back(msg);
    });

    EXPECT_EQ(rc, expectedRC);
    EXPECT_EQ(actualMessages.size(), expectedMessages.size());
    for (size_t i = 0; i < expectedMessages.size(); i++) {
        EXPECT_EQ(actualMessages[i].GetCode(), expectedMessages[i].GetCode());
    }
}

TEST(TestTitle, EmptyOffer) {
    Market::DataCamp::Offer offer;
    DoCheck(offer, false, {NMarket::NOfferError::OE459_EMPTY_TITLE});
}

TEST(TestTitle, TestEmptyTitle) {
    Market::DataCamp::Offer offer;
    offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->set_value("");
    DoCheck(offer, false, {NMarket::NOfferError::OE459_EMPTY_TITLE});
}

TEST(TestTitle, TestLongTitle) {
    std::string title = "";
    for (int i = 0; i < 768; i++) {
        title += "q";
    }
    Market::DataCamp::Offer offer;
    offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->set_value(title.c_str());
    DoCheck(offer, false, {NMarket::NOfferError::OE45X_LONG_TITLE});
}

TEST(TestTitle, TestAlphaCharacter) {
    Market::DataCamp::Offer offer;
    offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->set_value("123456");
    DoCheck(offer, false, {NMarket::NOfferError::OE459_EMPTY_TITLE});
}


TEST(TestTitle, TestCyrillic) {
    Market::DataCamp::Offer offer;
    offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->set_value("өчпочмак");
    DoCheck(offer, true, {});
}

TEST(TestTitle, TestBook) {
    Market::DataCamp::Offer offer;
    offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->set_value("123456");
    offer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(NMarket::EProductType::BOOKS);
    DoCheck(offer, true, {});
}

TEST(TestTitle, TestArtistTitle) {
    Market::DataCamp::Offer offer;
    offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_title()->set_value("123456");
    offer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(NMarket::EProductType::ARTIST_TITLE);
    DoCheck(offer, true, {});
}

TEST(TestTitle, Blue) {
    Market::DataCamp::Offer offer;
    auto rc = NValidators::NBlue::ValidateTitle(offer, [](const NMarket::NOfferError::TMessageGenerator&) {
        ASSERT_FALSE(true);
    });
    ASSERT_TRUE(rc);
}


