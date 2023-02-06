#include <market/library/category_restrictions/CategoryRestrictions.h>

#include <market/proto/content/mbo/Restrictions.pb.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <util/generic/buffer.h>
#include <util/stream/buffer.h>

#include <functional>


namespace {
    namespace NRestrictions = Market::Mbo::Restrictions;
    namespace NProto = google::protobuf;

    using NRestrictions::RestrictionsData;

    using TConstructor = std::function<void(RestrictionsData&)>;

    const Market::ICategoryTree& GetGlobalCategoryTree() {
        static THolder<Market::ICategoryTree> tree;
        if (!tree) {
            tree = Market::CreateCategoryTreeFromProtoFile(SRC_("data/tovar-tree.pb"));
        }
        return *tree;
    }

    TBuffer MakeRestrictionsProtobuf(const TConstructor& constructor) {
        RestrictionsData data;
        constructor(data);

        TBuffer buffer;
        buffer.Resize(data.ByteSize());
        Y_PROTOBUF_SUPPRESS_NODISCARD data.SerializeToArray(buffer.Data(), buffer.Size());
        return buffer;
    }

    TCategoryRestrictions MakeRestrictions(
            const TConstructor& constructor)
    {
        TBuffer buffer = MakeRestrictionsProtobuf(constructor);
        TBufferInput input(buffer);
        return TCategoryRestrictions(
                input,
                GetGlobalCategoryTree());
    }

    template<typename TSetType>
    void AssertSubset(const TSetType& xs, const TSetType& ys) {
        for (auto&& x: xs) {
            ASSERT_TRUE(ys.contains(x));
        }
    }

    template<typename TSetType>
    void AssertSetsEqual(const TSetType& xs, const TSetType& ys) {
        ASSERT_EQ(xs, ys);
    }

} // namespace


TEST(TestCategoryRestrictions, Empty) {
    auto restrictions = MakeRestrictions([](RestrictionsData&) {
        // do nothing
    });

    AssertSetsEqual({}, restrictions.GetGlobalRestrictions());
    ASSERT_EQ(nullptr, restrictions.GetRegionalRestrictions(1234));

    AssertSetsEqual({}, restrictions.GetGlobalBans());
    ASSERT_EQ(nullptr, restrictions.GetRegionalBans(1234));
}

TEST(TestCategoryRestrictions, Invalid) {
    auto restrictions = MakeRestrictions([](RestrictionsData& data) {
        auto restriction = data.add_restriction();
        auto category = restriction->add_category();
        category->set_id(91282);

        restriction = data.add_restriction();
        category = restriction->add_category();
        category->set_id(91291);
        auto regRestriction = restriction->add_regional_restriction();
        regRestriction->set_display_only_matched_offers(false);
    });

    AssertSetsEqual({}, restrictions.GetGlobalRestrictions());
    ASSERT_EQ(nullptr, restrictions.GetRegionalRestrictions(91282));
    ASSERT_EQ(nullptr, restrictions.GetRegionalRestrictions(91291));
}

TEST(TestCategoryRestrictions, Global) {
    auto restrictions = MakeRestrictions([](RestrictionsData& data) {
        auto restriction = data.add_restriction();
        auto category = restriction->add_category();
        category->set_id(91282);

        category = restriction->add_category();
        category->set_id(91549);
        category->set_include_subtree(true);

        auto regRestriction = restriction->add_regional_restriction();
        regRestriction->set_display_only_matched_offers(true);
        regRestriction->set_banned(true);
    });

    {
        const auto& categories = restrictions.GetGlobalRestrictions();
        AssertSubset({91282, 91549, 91550, 91556}, categories);
        ASSERT_FALSE(categories.contains(91291));
    }

    {
        const auto& categories = restrictions.GetGlobalBans();
        AssertSubset({91282, 91549, 91550, 91556}, categories);
        ASSERT_FALSE(categories.contains(91291));
    }
}

TEST(TestCategoryRestrictions, Regional) {
    enum {
        CATEGORY_ID = 91282,
    };

    auto restrictions = MakeRestrictions([](RestrictionsData& data) {
        auto restriction = data.add_restriction();
        auto category = restriction->add_category();
        category->set_id(CATEGORY_ID);

        auto regRestriction = restriction->add_regional_restriction();
        regRestriction->set_display_only_matched_offers(true);
        regRestriction->set_banned(true);

        auto region = regRestriction->add_region();
        region->set_id(98849);

        region = regRestriction->add_region();
        region->set_id(98850);
        region->set_include_subtree(true);
    });

    {
        const auto* regions = restrictions.GetRegionalRestrictions(CATEGORY_ID);
        ASSERT_NE(nullptr, regions);
        AssertSubset({98849, 98850}, *regions);
        ASSERT_EQ(0, regions->count(131086));
    }

    {
        const auto* regions = restrictions.GetRegionalBans(CATEGORY_ID);
        ASSERT_NE(nullptr, regions);
        AssertSubset({98849, 98850}, *regions);
        ASSERT_EQ(0, regions->count(131086));
    }
}

TEST(TestCategoryRestrictions, Mixed) {
    enum {
        CATEGORY_ID = 91282,
    };

    auto restrictions = MakeRestrictions([](RestrictionsData& data) {
        auto restriction = data.add_restriction();
        auto category = restriction->add_category();
        category->set_id(CATEGORY_ID);

        // global restriction
        auto regRestriction = restriction->add_regional_restriction();
        regRestriction->set_display_only_matched_offers(true);
        regRestriction->set_banned(true);

        // regional restriction
        regRestriction = restriction->add_regional_restriction();
        regRestriction->set_display_only_matched_offers(true);
        regRestriction->set_banned(true);
        category = restriction->add_category();
        category->set_id(CATEGORY_ID);

        auto region = regRestriction->add_region();
        region->set_id(98849);
    });

    {
        AssertSetsEqual({CATEGORY_ID}, restrictions.GetGlobalRestrictions());
        const auto* regions = restrictions.GetRegionalRestrictions(CATEGORY_ID);
        ASSERT_NE(nullptr, regions);
        AssertSetsEqual({98849}, *regions);
    }

    {
        AssertSetsEqual({CATEGORY_ID}, restrictions.GetGlobalBans());
        const auto* regions = restrictions.GetRegionalBans(CATEGORY_ID);
        ASSERT_NE(nullptr, regions);
        AssertSetsEqual({98849}, *regions);
    }
}

TEST(TestCategoryRestrictions, OnlyMatchedOffers) {
    enum {
        CATEGORY_ID = 91282,
    };

    auto restrictions = MakeRestrictions([](RestrictionsData& data) {
        auto restriction = data.add_restriction();
        auto category = restriction->add_category();
        category->set_id(CATEGORY_ID);

        // global restriction
        auto regRestriction = restriction->add_regional_restriction();
        regRestriction->set_display_only_matched_offers(true);

        // regional restriction
        regRestriction = restriction->add_regional_restriction();
        regRestriction->set_display_only_matched_offers(true);
        category = restriction->add_category();
        category->set_id(CATEGORY_ID);

        auto region = regRestriction->add_region();
        region->set_id(98849);
    });

    {
        AssertSetsEqual({CATEGORY_ID}, restrictions.GetGlobalRestrictions());
        const auto* regions = restrictions.GetRegionalRestrictions(CATEGORY_ID);
        ASSERT_NE(nullptr, regions);
        AssertSetsEqual({98849}, *regions);
    }

    {
        AssertSetsEqual({}, restrictions.GetGlobalBans());
        const auto* regions = restrictions.GetRegionalBans(CATEGORY_ID);
        ASSERT_EQ(nullptr, regions);
    }
}

TEST(TestCategoryRestrictions, OnlyBans) {
    enum {
        CATEGORY_ID = 91282,
    };

    auto restrictions = MakeRestrictions([](RestrictionsData& data) {
        auto restriction = data.add_restriction();
        auto category = restriction->add_category();
        category->set_id(CATEGORY_ID);

        // global restriction
        auto regRestriction = restriction->add_regional_restriction();
        regRestriction->set_banned(true);

        // regional restriction
        regRestriction = restriction->add_regional_restriction();
        regRestriction->set_banned(true);
        category = restriction->add_category();
        category->set_id(CATEGORY_ID);

        auto region = regRestriction->add_region();
        region->set_id(98849);
    });

    {
        AssertSetsEqual({}, restrictions.GetGlobalRestrictions());
        const auto* regions = restrictions.GetRegionalRestrictions(CATEGORY_ID);
        ASSERT_EQ(nullptr, regions);
    }

    {
        AssertSetsEqual({CATEGORY_ID}, restrictions.GetGlobalBans());
        const auto* regions = restrictions.GetRegionalBans(CATEGORY_ID);
        ASSERT_NE(nullptr, regions);
        AssertSetsEqual({98849}, *regions);
    }
}
