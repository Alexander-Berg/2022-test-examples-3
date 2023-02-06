#include <market/idx/datacamp/miner/processors/category_restrictions_validator/category_restrictions.h>

#include <market/library/offers_common/idx_exception.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <market/proto/content/mbo/Restrictions.pb.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <util/folder/path.h>
#include <util/stream/buffer.h>

namespace {
    using ::Market::Mbo::Restrictions::RestrictionsData;
    using ::Market::DataCamp::Offer;

    using TConstructor = std::function<void(RestrictionsData&)>;

    TBuffer MakeRestrictionsProtobuf(const TConstructor& constructor) {
        RestrictionsData data;
        constructor(data);

        TBuffer buffer;
        buffer.Resize(data.ByteSize());
        Y_VERIFY(data.SerializeToArray(buffer.Data(), buffer.Size()));
        return buffer;
    }

    TCategoryRestrictions MakeRestrictions(
            const TConstructor& constructor)
    {
        auto categoryTree = Market::CreateCategoryTreeFromProtoFile(
            JoinFsPaths(
                ArcadiaSourceRoot(),
                "market/library/category_restrictions/ut/data/tovar-tree.pb"));
        TBuffer buffer = MakeRestrictionsProtobuf(constructor);
        TBufferInput input(buffer);
        return TCategoryRestrictions(
                input,
                *categoryTree);
    }

    const TCategoryRestrictions& GetRestrictions() {
        static auto restrictions = MakeRestrictions([](RestrictionsData& data) {
            auto restriction = data.add_restriction();
            auto category = restriction->add_category();
            category->set_id(91282);

            auto regRestriction = restriction->add_regional_restriction();
            regRestriction->set_display_only_matched_offers(true);
            regRestriction->set_banned(true);

            auto region = regRestriction->add_region();
            region->set_id(98849);

            region = regRestriction->add_region();
            region->set_id(98850);
            region->set_include_subtree(true);

            restriction = data.add_restriction();
            regRestriction = restriction->add_regional_restriction();
            regRestriction->set_display_only_matched_offers(true);
            category = restriction->add_category();
            category->set_id(91549);
        });
        return restrictions;
    }
}

TEST(CategoryRestrictionsProcessor, CheckRestrictions) {
    const auto& restrictions = GetRestrictions();

    {
        const auto* regions = restrictions.GetRegionalRestrictions(91282);
        ASSERT_NE(nullptr, regions);
        ASSERT_TRUE(regions->contains(98849));
        ASSERT_TRUE(regions->contains(98850));
        ASSERT_FALSE(regions->contains(131086));
    }

    {
        const auto* regions = restrictions.GetRegionalBans(91282);
        ASSERT_NE(nullptr, regions);
        ASSERT_FALSE(regions->contains(131086));
    }

    {
        const auto& categories = restrictions.GetGlobalRestrictions();
        ASSERT_EQ(1, categories.size());
        ASSERT_TRUE(categories.contains(91549));
        ASSERT_FALSE(categories.contains(91291));
    }
}

TEST(CategoryRestrictionsProcessor, EmptyOffer) {
    Offer offer;
    const auto& restrictions = GetRestrictions();
    ASSERT_NO_THROW(NMiner::ProcessOffer(offer, restrictions));
    ASSERT_EQ(0, offer.offers_processor_fields().forbidden_regions_size());
}

TEST(CategoryRestrictionsProcessor, OfferWithUnknownModel) {
    Offer offer;
    offer.mutable_content()->mutable_market()->set_category_id(91291);

    const auto& restrictions = GetRestrictions();

    ASSERT_NO_THROW(NMiner::ProcessOffer(offer, restrictions));
    ASSERT_EQ(0, offer.offers_processor_fields().forbidden_regions_size());
}

TEST(CategoryRestrictionsProcessor, OfferWithUnknownCategory) {
    Offer offer;
    offer.mutable_content()->mutable_market()->mutable_ir_data()->set_matched_id(1);

    const auto& restrictions = GetRestrictions();
    ASSERT_NO_THROW(NMiner::ProcessOffer(offer, restrictions));
    ASSERT_EQ(0, offer.offers_processor_fields().forbidden_regions_size());
}

TEST(CategoryRestrictionsProcessor, OfferWithBannedCategory) {
    Offer offer;
    offer.mutable_content()->mutable_market()->mutable_ir_data()->set_matched_id(1);
    offer.mutable_content()->mutable_market()->set_category_id(91549);

    const auto& restrictions = GetRestrictions();
    ASSERT_THROW(NMiner::ProcessOffer(offer, restrictions), TDeclinedRecord);
    ASSERT_EQ(0, offer.offers_processor_fields().forbidden_regions_size());
}

TEST(CategoryRestrictionsProcessor, OfferWithBannedRegions) {
    Offer offer;
    offer.mutable_content()->mutable_market()->set_category_id(91282);

    const auto& restrictions = GetRestrictions();
    ASSERT_NO_THROW(NMiner::ProcessOffer(offer, restrictions));
    const auto& forbiddenRegions = offer.offers_processor_fields().forbidden_regions();
    ASSERT_EQ(2, forbiddenRegions.size());
    ASSERT_TRUE(Find(forbiddenRegions, 98849) != forbiddenRegions.end());
    ASSERT_TRUE(Find(forbiddenRegions, 98850) != forbiddenRegions.end());
}

TEST(CategoryRestrictionsProcessor, GoodOffer) {
    Offer offer;
    offer.mutable_content()->mutable_market()->mutable_ir_data()->set_matched_id(1);
    offer.mutable_content()->mutable_market()->set_category_id(91291);

    const auto& restrictions = GetRestrictions();
    ASSERT_NO_THROW(NMiner::ProcessOffer(offer, restrictions));
    ASSERT_EQ(0, offer.offers_processor_fields().forbidden_regions_size());
}
