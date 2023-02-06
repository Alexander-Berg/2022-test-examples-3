#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <market/library/contex_mms/reader.h>
#include <market/report/test/global_mock.h>
#include <market/report/library/search_filter/filter.h>
#include <kernel/qtree/richrequest/richnode.h>

namespace {
    using namespace NMarketReport;
    using namespace NMarketReport::NBase;

    class TEmptyRichTreeFactory final : public IRichTreeFactory {
    public:
        TRichTreePtr Create(const TString&) const override {
            return CreateEmptyRichTree();
        }
    };

    TCollectionFilters EmptyFilters() {
        return TCollectionFilters(MakeSimpleShared<TEmptyRichTreeFactory>(), CreateEmptyRichTree(), TFilterContext());
    }

    TCollectionFilters FiltersWithExp(const TVector<TString>& exps) {
        TFilterContext ctx;
        ctx.ContexPack = exps;
        ctx.ContexEnabled = true;
        return TCollectionFilters(MakeSimpleShared<TEmptyRichTreeFactory>(), CreateEmptyRichTree(), ctx);
    }

    struct TSavingStrategyMock final : public IFilterSaveStrategy {
        void Save(const TString& text, const TString&, const TString& collection) override {
            Text = text;
            Collection = collection;
        }

        TString Text;
        TString Collection;
    };

    using NMarket::NContex::TExpId;
    using NMarket::NContex::TModelId;

    struct TContexModelReaderMock final : public NMarket::NContex::IContexModelReader {
        bool HasExperiment(const TExpId& expId) const {
            return Exp == expId;
        }

        TMaybe<TModelId> GetExpModelId(const TExpId& expId, const TModelId baseModelId) const {
            if (Exp != expId) {
                return Nothing();
            }

            auto expModel = Models.FindPtr(baseModelId);
            if (expModel) {
                return *expModel;
            }

            return Nothing();
        }

        TMaybe<TModelId> GetBaseModelId(const TModelId expModelId) const {
            for (const auto& modelPair : Models) {
                if (modelPair.second == expModelId) {
                    return modelPair.first;
                }
            }

            return Nothing();
        }

        const THashSet<TExpId>& AllExperiments() const {
            const static THashSet<TExpId> emptySet;
            return emptySet;
        }

        void EnumerateExperiments(const TExpEnumerator&) const {}
        void EnumerateBaseModels(const TBaseModelEnumerator&) const {}

        TExpId Exp;
        THashMap<TModelId, TModelId> Models;
    };
}

template <typename TFilter>
void TFilterBase<TFilter>::TryOnContex() { }

TEST(CollectionFiltersTest, EmptyOnNoRichTree) {
    auto sut = EmptyFilters();
    EXPECT_TRUE(sut.Offers().Empty());
}

TEST(CollectionFiltersTest, BuildsOrGroup) {
    auto sut = EmptyFilters();
    sut.Offers().WithFeedOfferId({"1", "2", "3"});

    TSavingStrategyMock result;
    sut.Save(result);
    EXPECT_EQ(result.Text, R"((feed_offer_ids:"1" | feed_offer_ids:"2" | feed_offer_ids:"3"))");
}

TEST(CollectionFiltersTest, BuildsAndGroup) {
    auto sut = EmptyFilters();
    sut.Offers()
        .WithUrl({"abc", "def"}, {})
        .WithModelId({45, 46});

    TSavingStrategyMock result;
    sut.Save(result);
    EXPECT_EQ(
        result.Text,
        R"((offer_url_hash:"abc" | offer_url_hash:"def") << (hyper_id:"45" | hyper_id:"46"))"
    );
}

TEST(CollectionFiltersTest, BuildsOrOnesGroup) {
    auto sut = EmptyFilters();
    sut.Offers()
        .WithUrl({"abc"}, {})
        .WithModelId(45)
        .OrOnes()
            .WithVendorId({55});

    TSavingStrategyMock result;
    sut.Save(result);

    EXPECT_EQ(
        result.Text,
        R"((offer_url_hash:"abc" &/(-64 64) hyper_id:"45" | vendor_id:"55"))"
    );
}

TEST(CollectionFiltersTest, BuildsScopes) {
    auto sut = EmptyFilters();
    sut.Offers()
        .WithUrl({"abc"}, {})
        .AmongOnes()
            .WithModelId(45)
            .OrOnes()
                .WithVendorId({55});

    TSavingStrategyMock result;
    sut.Save(result);

    EXPECT_EQ(
        result.Text,
        R"(offer_url_hash:"abc" << (hyper_id:"45" | vendor_id:"55"))"
    );
}

TEST(CollectionFiltersTest, DoNotSupportContexForNonModelCollections) {
    TGlobalTestScope testGlobal;
    auto reader = MakeHolder<TContexModelReaderMock>();

    /// setup experiments database
    reader->Exp = "good exp";
    reader->Models = {{24, 38}};
    NMarketReport::NGlobal::LoadContexCustomImpl(std::move(reader));

    /// setup user context
    auto sut = FiltersWithExp({"good exp"});

    /// non model collections are immune to content experiments
    sut.Offers().WithFeedOfferId("1");

    TSavingStrategyMock result;
    sut.Save(result);
    EXPECT_EQ(result.Text, R"(feed_offer_ids:"1")");
}

TEST(CollectionFiltersTest, SupportsContexModel) {
    TGlobalTestScope testGlobal;
    auto reader = MakeHolder<TContexModelReaderMock>();

    /// setup experiments database
    reader->Exp = "good exp";
    reader->Models = {{24, 38}};
    NMarketReport::NGlobal::LoadContexCustomImpl(std::move(reader));

    /// setup user context
    auto sut = FiltersWithExp({"good exp"});

    /// single model query in experiment transforms to experimental model ID
    sut.Models()
        .WithModelId(24)
        .WithGroupId(24)
        .WithParentId(24);

    TSavingStrategyMock result;
    sut.Save(result);
    EXPECT_EQ(
        result.Text,
        R"((hyper_model_id:"24" | hyper_group_id:"24") model_color:"white" ~~ anti_contex:"good exp" << (contex:"green" | contex:"classic" | contex:"good exp") parent_model_id:"24" hyper_group_id:"24")"
    );
}

TEST(CollectionFiltersTest, SupportsContexModelWideQueries) {
    TGlobalTestScope testGlobal;
    auto reader = MakeHolder<TContexModelReaderMock>();

    /// setup experiments database
    reader->Exp = "good exp";
    reader->Models = {{24, 38}};
    NMarketReport::NGlobal::LoadContexCustomImpl(std::move(reader));

    /// setup user context
    auto sut = FiltersWithExp({"good exp"});

    /// single model query in experiment transforms to experimental model ID
    sut.Models().WithCategoryId(122);

    TSavingStrategyMock result;
    sut.Save(result);
    EXPECT_EQ(
        result.Text,
        R"(hyper_categ_id:"122" model_color:"white" ~~ anti_contex:"good exp" << (contex:"green" | contex:"classic" | contex:"good exp"))"
    );
}

TEST(CollectionFiltersTest, SupportsContexModelWideQueriesInsideOrFilters) {
    TGlobalTestScope testGlobal;
    auto reader = MakeHolder<TContexModelReaderMock>();

    /// setup experiments database
    reader->Exp = "good exp";
    reader->Models = {{24, 38}};
    NMarketReport::NGlobal::LoadContexCustomImpl(std::move(reader));

    /// setup user context
    auto sut = FiltersWithExp({"good exp"});

    /// single model query in experiment transforms to experimental model ID
    sut.Models()
        .WithCategoryId(122)
        .OrOnes()
            .WithModelId(24);

    TSavingStrategyMock result;
    sut.Save(result);
    EXPECT_EQ(
        result.Text,
        R"((hyper_categ_id:"122" &/(-64 64) (contex:"green" | contex:"classic" | contex:"good exp") &/(-64 64) model_color:"white" ~~ anti_contex:"good exp" | (hyper_model_id:"24" | hyper_group_id:"24") &/(-64 64) (contex:"green" | contex:"classic" | contex:"good exp") &/(-64 64) model_color:"white" ~~ anti_contex:"good exp"))"
    );
}

TEST(CollectionFiltersTest, DoNotReplaceModelOutsideExperiment) {
    TGlobalTestScope testGlobal;
    auto reader = MakeHolder<TContexModelReaderMock>();

    /// setup experiments database
    reader->Exp = "good exp";
    reader->Models = {{24, 38}};
    NMarketReport::NGlobal::LoadContexCustomImpl(std::move(reader));

    /// setup user context
    auto sut = FiltersWithExp({});

    sut.Models()
        .WithParentId(24);

    TSavingStrategyMock result;
    sut.Save(result);
    EXPECT_EQ(
        result.Text,
        R"(parent_model_id:"24" model_color:"white" << (contex:"green" | contex:"classic"))"
    );
}

TEST(CollectionFiltersTest, SupportsContexModelWideQueriesOutsideExperiment) {
    TGlobalTestScope testGlobal;
    auto reader = MakeHolder<TContexModelReaderMock>();

    /// setup experiments database
    reader->Exp = "good exp";
    reader->Models = {{24, 38}};
    NMarketReport::NGlobal::LoadContexCustomImpl(std::move(reader));

    /// setup user context
    auto sut = FiltersWithExp({});

    /// single model query in experiment transforms to experimental model ID
    sut.Models().WithCategoryId(122);

    TSavingStrategyMock result;
    sut.Save(result);
    EXPECT_EQ(
        result.Text,
        R"(hyper_categ_id:"122" model_color:"white" << (contex:"green" | contex:"classic"))"
    );
}
