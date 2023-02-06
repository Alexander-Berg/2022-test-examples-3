#include <library/cpp/testing/unittest/gtest.h>
#include <tuple>

#include <market/idx/library/validators/barcode.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>


std::pair<bool, std::vector<NMarket::NOfferError::TMessageGenerator>> Validate(Market::DataCamp::Offer& offer) {
    std::vector<NMarket::NOfferError::TMessageGenerator> messages;
    bool rc = NValidators::ValidateBarcodes(offer, [&messages](const auto& msg, const auto&) {
        messages.push_back(msg);
    });
    return {rc, messages};
}


TEST(TestBarcode, Empty) {
    Market::DataCamp::Offer offer;
    ASSERT_TRUE(NValidators::ValidateBarcodes(offer));
}

TEST(TestBarcode, ValidCase) {
    Market::DataCamp::Offer offer;
    *offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value() = "12345678";
    *offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value() = "23456789";

    bool rc = false;
    std::tie(rc, std::ignore) = Validate(offer);

    ASSERT_TRUE(rc);
    ASSERT_EQ(offer.content().partner().actual().barcode().value().size(), 2);
    EXPECT_EQ(offer.content().partner().actual().barcode().value(0), "12345678");
    EXPECT_EQ(offer.content().partner().actual().barcode().value(1), "23456789");
}

TEST(TestBarcode, RemoveInvalid) {
    Market::DataCamp::Offer offer;
    *offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value() = "ABC";
    *offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value() = "12345678";

    auto [rc, messages] = Validate(offer);

    ASSERT_TRUE(rc);
    ASSERT_EQ(offer.content().partner().actual().barcode().value().size(), 1);
    EXPECT_EQ(offer.content().partner().actual().barcode().value(0), "12345678");

    ASSERT_EQ(messages.size(), 1);
    EXPECT_EQ(messages[0].GetCode(), "355");
}

TEST(TestBarcode, AlcoOffer) {
    Market::DataCamp::Offer offer;
    offer.mutable_content()->mutable_partner()->mutable_actual()->mutable_type()->set_value(NMarket::EProductType::ALCO);

    {
        auto [rc, messages] = Validate(offer);

        ASSERT_FALSE(rc);
        EXPECT_EQ(offer.content().partner().actual().barcode().value().size(), 0);
        ASSERT_EQ(messages.size(), 1);
        EXPECT_EQ(messages[0].GetCode(), "45G");
    }

    {
        *offer.mutable_content()->mutable_partner()->mutable_original()->mutable_barcode()->add_value() = "12345678";

        auto [rc, messages] = Validate(offer);

        ASSERT_TRUE(rc);
        ASSERT_EQ(messages.size(), 0);
        ASSERT_EQ(offer.content().partner().actual().barcode().value().size(), 1);
        EXPECT_EQ(offer.content().partner().actual().barcode().value(0), "12345678");
    }
}

TEST(TestRoughBarcode, ValidBarcode) {
    TString barcode(30, '1');

    ASSERT_TRUE(NValidators::RoughCheckBarcode(barcode));
}

TEST(TestRoughBarcode, BarcodeIsTooLong) {
    TString barcode(31, '2');

    ASSERT_FALSE(NValidators::RoughCheckBarcode(barcode));
}

TEST(TestRoughBarcode, BarcodeIsTooShort) {
    TString barcode(4, '3');

    ASSERT_FALSE(NValidators::RoughCheckBarcode(barcode));
}

TEST(TestRoughBarcode, ContainingLetter) {
    // for now letters are allowed
    TString barcode("ab345678");

    ASSERT_TRUE(NValidators::RoughCheckBarcode(barcode));
}
