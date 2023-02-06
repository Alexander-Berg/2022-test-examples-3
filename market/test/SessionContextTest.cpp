#include <market/report/library/session_context/session_context.h>
#include <market/report/library/encryption_key/encryption_key.h>
#include <market/report/library/placement/placement.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/string_utils/base64/base64.h>

#include <iostream>

using namespace NMarketReport;

TEST(SessionContext, IsReversible) {
    const EncryptionKey key(Base64DecodeUneven("ayFVMGPqmKf4pZ0rnsGMGQ=="), 1);
    const std::string serialized = SessionContext::createSerializedOfferContext("13" /*offer id*/,
                                                                            555 /*shop id*/,
                                                                            111 /*hid*/,
                                                                            666 /*click price*/,
                                                                            777 /*click_price_before_bid_correction*/,
                                                                            678 /*click price no exp*/,
                                                                            888 /*bid*/,
                                                                            42 /*vendor_click price*/,
                                                                            44 /*vendor_click price no exp*/,
                                                                            4242 /*vendor_bid*/,
                                                                            12 /*minimal_bid*/,
                                                                            113 /*shop_fee*/,
                                                                            100 /*minimal_fee*/,
                                                                            109 /*fee*/,
                                                                            "minbid" /*bid_type*/,
                                                                            "TheRequestId" /*request_id*/,
                                                                            EPlacement::PERS_GRADE,
                                                                            key);
    SessionContext ctx(serialized.c_str(), key);
    ClickPrice cp = 0;

    ctx.getOfferClickPrice("13", 111 /*shop id*/, 222 /*bid*/, &cp);
    EXPECT_EQ(666U, cp);

    ctx.getOfferClickPriceNoExp("13", 111 /*shop id*/, &cp);
    EXPECT_EQ(678U, cp);

    ctx.getOfferBid("13", 111 /*shop id*/, 222 /*bid*/, &cp);
    EXPECT_EQ(888U, cp);

    ctx.getOfferVendorClickPrice("13", 111 /*shop id*/, 222 /*bid*/, &cp);
    EXPECT_EQ(42U, cp);

    ctx.getOfferVendorClickPriceNoExp("13", 111 /*shop id*/, &cp);
    EXPECT_EQ(44U, cp);

    ctx.getOfferVendorBid("13", 111 /*shop id*/, 222 /*bid*/, &cp);
    EXPECT_EQ(4242U, cp);

    cp = 0;
    ctx.getOfferMinimalBid("13", &cp);
    EXPECT_EQ(12U, cp);

    NQBid::TFee fee = 0;

    ctx.getOfferShopFee("13", &fee);
    EXPECT_EQ(113U, fee);

    ctx.getOfferMinimalFee("13", &fee);
    EXPECT_EQ(100U, fee);

    ctx.getOfferFee("13", &fee);
    EXPECT_EQ(109U, fee);

    TString bidType;
    ctx.getOfferBidType("13", &bidType);
    EXPECT_EQ("minbid", bidType);

    cp = 100;
    // not existing in context offer with not matched shop
    ctx.getOfferClickPrice("14", 111 /*shop id*/, 222 /*bid*/, &cp);
    EXPECT_EQ(100U, cp);

    // not existing in context offer with matched shop
    ctx.getOfferClickPrice("14", 555 /*shop id*/, 222 /*bid*/, &cp);
    EXPECT_EQ(222U, cp);

    // not existing in context offer
    cp = 17;
    ctx.getOfferMinimalBid("14", &cp);
    EXPECT_EQ(17U, cp);

    fee = 104;
    ctx.getOfferFee("14", &fee);
    EXPECT_EQ(104U, fee);


    fee = 102;
    ctx.getOfferMinimalFee("14", &fee);
    EXPECT_EQ(102U, fee);

    EPlacement pp = EPlacement::INVALID;
    ctx.getOfferPrevPp(&pp);
    EXPECT_EQ(static_cast<std::underlying_type<EPlacement>::type>(EPlacement::PERS_GRADE),
              static_cast<std::underlying_type<EPlacement>::type>(pp));

    const TMaybe<EPlacement> mpp = ctx.getOfferPrevPp();
    EXPECT_TRUE(mpp);
    EXPECT_EQ(static_cast<std::underlying_type<EPlacement>::type>(EPlacement::PERS_GRADE),
              static_cast<std::underlying_type<EPlacement>::type>(mpp.GetRef()));
}

TEST(SessionContext, OfferPriceTestCase1) {
    const EncryptionKey key(Base64DecodeUneven("ayFVMGPqmKf4pZ0rnsGMGQ=="), 1);
    const char* s = "RFJoDjFL32L5durICJku-Q";
    SessionContext ctx(s, key);
    ClickPrice cp = 0;
    // not existing in context offer
    ctx.getOfferClickPrice("615455637", 555 /*shop id*/, 222 /*bid*/, &cp);
    EXPECT_EQ(0U, cp);
    ctx.getOfferVendorClickPrice("615455637", 555 /*shop id*/, 222 /*bid*/, &cp);
    EXPECT_EQ(0U, cp);

    EPlacement pp = EPlacement::INVALID;
    ctx.getOfferPrevPp(&pp);
    EXPECT_EQ(static_cast<std::underlying_type<EPlacement>::type>(EPlacement::INVALID),
              static_cast<std::underlying_type<EPlacement>::type>(pp));
    EXPECT_FALSE(ctx.getOfferPrevPp());
}

TEST(SessionContext, ContextlessAccess) {
    const EncryptionKey key(Base64DecodeUneven("ayFVMGPqmKf4pZ0rnsGMGQ=="), 1);
    SessionContext ctx("", key);
    ClickPrice cp = 20;
    // not existing in context offer
    ctx.getOfferClickPrice("615455637", 555 /*shop id*/, 222 /*bid*/, &cp);
    EXPECT_EQ(20U, cp);
    ctx.getOfferBid("615455637", 555 /*shop id*/, 222 /*bid*/, &cp);
    EXPECT_EQ(20U, cp);
    ctx.getOfferVendorClickPrice("615455637", 555 /*shop id*/, 222 /*bid*/, &cp);
    EXPECT_EQ(20U, cp);
    ctx.getOfferVendorBid("615455637", 555 /*shop id*/, 222 /*bid*/, &cp);
    EXPECT_EQ(20U, cp);

    EPlacement pp = EPlacement::INVALID;
    ctx.getOfferPrevPp(&pp);
    EXPECT_EQ(static_cast<std::underlying_type<EPlacement>::type>(EPlacement::INVALID),
              static_cast<std::underlying_type<EPlacement>::type>(pp));
    EXPECT_FALSE(ctx.getOfferPrevPp());
}

