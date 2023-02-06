#include "../cms_promo.h"

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/json/json_reader.h>

TEST(CMS_PROMO, ValidRecord) {
    const auto promos = NMarket::NCmsPromo::LoadCmsPromosFromString(R"({
        "result": [{
            "id": 101,
            "promo_id": "BlackFriday18",
            "featured_msku": [{
                "msku": 11,
                "picture": "pic1",
                "description": "descr1"
            }, {
                "msku": 13,
                "picture": "pic2",
                "description": "descr2"
            }],
            "force_relevance_msku": [{
                "msku": 12
            }, {
                "msku": 15
            }],
            "available_mskus": [11, 12, 13, 15],
        }]
    })");

    const auto* promoBlackFriday18 = promos.FindPromo("BlackFriday18");
    EXPECT_TRUE(promoBlackFriday18 != nullptr);
    const auto* featured_11 = promoBlackFriday18->FindFeaturedMsku(11);
    EXPECT_TRUE(featured_11 != nullptr);
    EXPECT_EQ(featured_11->Description, "descr1");
    EXPECT_EQ(featured_11->PictureUrl, "pic1");

    const auto* featured_13 = promoBlackFriday18->FindFeaturedMsku(13);
    EXPECT_TRUE(featured_13 != nullptr);
    EXPECT_EQ(featured_13->Description, "descr2");
    EXPECT_EQ(featured_13->PictureUrl, "pic2");

    const auto* featured_12 = promoBlackFriday18->FindFeaturedMsku(12);
    EXPECT_TRUE(featured_12 == nullptr);

    EXPECT_TRUE(promoBlackFriday18->IsMskuForced(12));
    EXPECT_TRUE(promoBlackFriday18->IsMskuForced(15));
    EXPECT_FALSE(promoBlackFriday18->IsMskuForced(13));

    EXPECT_TRUE(promoBlackFriday18->IsAvailableMsku(11));
    EXPECT_TRUE(promoBlackFriday18->IsAvailableMsku(15));
    EXPECT_FALSE(promoBlackFriday18->IsAvailableMsku(14));

    EXPECT_TRUE(promos.FindPromo("BlackFriday17") == nullptr);
}

TEST(CMS_PROMO, InvalidResult) {
    EXPECT_THROW(NMarket::NCmsPromo::LoadCmsPromosFromString(R"({
        "no_result": []
    })"), NJson::TJsonException);
}

TEST(CMS_PROMO, InvalidPromoId) {
    EXPECT_THROW(NMarket::NCmsPromo::LoadCmsPromosFromString(R"({
        "result": [{
            "no_promo_id": "101",
        }]
    })"), NJson::TJsonException);
}

TEST(CMS_PROMO, InvalidPromoIdFormat) {
    EXPECT_THROW(NMarket::NCmsPromo::LoadCmsPromosFromString(R"({
        "result": [{
            "promo_id": ["hahaha"],
        }]
    })"), NJson::TJsonException);
}

TEST(CMS_PROMO, InvalidFeaturedMsku) {
    EXPECT_THROW(NMarket::NCmsPromo::LoadCmsPromosFromString(R"({
        "result": [{
            "id": 101,
            "promo_id": "101",
            "featured_msku": [{
                "no_msku": 11,
                "picture": "pic1",
                "description": "descr1"
            }],
        }]
    })"), NJson::TJsonException);
}

TEST(CMS_PROMO, InvalidFeaturedMskuFormat) {
    EXPECT_THROW(NMarket::NCmsPromo::LoadCmsPromosFromString(R"({
        "result": [{
            "id": 101,
            "promo_id": "101",
            "featured_msku": [{
                "no_msku": "11",
                "picture": "pic1",
                "description": "descr1"
            }],
        }]
    })"), NJson::TJsonException);
}

TEST(CMS_PROMO, InvalidFeaturedPicture) {
    EXPECT_THROW(NMarket::NCmsPromo::LoadCmsPromosFromString(R"({
        "result": [{
            "id": 101,
            "promo_id": "101",
            "featured_msku": [{
                "msku": 11,
                "no_picture": "pic1",
                "description": "descr1"
            }]
        }]
    })"), NJson::TJsonException);
}

TEST(CMS_PROMO, InvalidFeaturedDescription) {
    EXPECT_THROW(NMarket::NCmsPromo::LoadCmsPromosFromString(R"({
        "result": [{
            "id": 101,
            "promo_id": "101",
            "featured_msku": [{
                "msku": 11,
                "picture": "pic1",
                "no_description": "descr1"
            }]
        }]
    })"), NJson::TJsonException);
}
