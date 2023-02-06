#include <market/report/library/constants/constants.h>
#include <market/report/library/relevance/relevance/fields.h>
#include <market/report/library/relevance/money/hybrid_auction_calculator.h>
#include <market/report/library/global/contex/contex.h>
#include <market/library/trees/category_tree.h>
#include <market/library/glparams/legacy_gl_mbo_reader.h>
#include <market/library/algorithm/tree.h>

#include <library/cpp/testing/unittest/gtest.h>

#include "global_mock.h"

using namespace NMarketReport;
using namespace MarketRelevance;

namespace {
    template <typename TData>
    class TTreeStub : public Market::ITree<TData> {
    public:
        using TNode = Market::ITreeNode<TData>;
        using TId = typename TNode::TId;
        using TTreeCode =  typename Market::ITree<TData>::TTreeCode;

    public:
        virtual TId GetId() const {
            return 123;
        }

        virtual TTreeCode GetCode() const {
            return "";
        }

        virtual const TNode* FindNode(TId) const {
            ythrow yexception() << "not impl";
        }

        virtual const TNode* FindNode(typename Market::ITreeSecondaryIndex<TData>::TId, const TData&) const {
            ythrow yexception() << "not impl";
        }

        virtual const typename TNode::TListType& FindAny(typename Market::ITreeSecondaryIndex<TData>::TId, const TData&) const {
            ythrow yexception() << "not impl";
        }

        virtual const TNode* FindRoot() const {
            return nullptr;
        }

        virtual size_t GetSize() const {
            return 1;
        }
    };

    class TNavigationConverterStub : public TNavigationConverter {
    public:
        TNavigationConverterStub()
            : TNavigationConverter(
                GetTree<Market::TNavigationNodeData>(),
                GetTree<Market::TCategoryData>()
            )
        {}
    private:
        template <typename TData>
        static TTreeStub<TData> GetTree() {
            static TTreeStub<TData> Tree;
            return Tree;
        }
    };
}

class GuruLightRelevance: public NewRel::TGuruLightField {
public:
    GuruLightRelevance(const GuruLightSC3::TParams& gl_offer_params, const SortingParams& params)
        : TGuruLightField(gl_offer_params, params)
    {
    }
    int get(const void* attrs, int attr_id, int default_value) const {
        (void)attrs;
        (void)attr_id;
        return default_value;
    }
};

class TRegionTreeStub : public Market::IRegionTree {
public:
    virtual const TNode* FindNode(Market::IRegionNode::TId) const {
        return nullptr;
    }

    virtual const TNode* FindNode(size_t, const Market::TRegionData&) const {
        return nullptr;
    }

    virtual const TNode::TListType& FindAny(size_t, const Market::TRegionData&) const {
        static const TNode::TListType empty;
        return empty;
    }

    virtual const TNode* FindRoot() const {
        return nullptr;
    }

    virtual size_t GetSize() const {
        return 0;
    }

    virtual size_t GetId() const {
        return 0;
    }

    virtual Market::IRegionTree::TTreeCode GetCode() const {
        return "";
    }
};

TNavigationConverterStub navigationConverter;

TEST(UserRelevance, GuruLightFieldCreate) {

    NGlobal::SetNavigationConverterStub(navigationConverter);

    MarketSearch::SortingSpec ss;
    // Set up the sorting spec object.
    TRelevanceOrder inOrder(ERelevanceName::Main, ss);
    auto outOrder = TRelevanceOrder::Deserialize(inOrder.Serialize().c_str());

    NMarketReport::NGlobal::LoadContex("mockPath", "mockPath");
    TRegionTreeStub tree;
    SortingParams sp(tree, outOrder);

    GuruLightMBO2::TParams mbo_params;
    THolder<NGlMbo::IReader> glMboReader = CreateLegacyGlMboReader(mbo_params);
    GuruLightSC3::TParams offer_params(*glMboReader);
    GuruLightRelevance gl(offer_params, sp);

    EXPECT_TRUE(gl.CheckGlFilters(GuruLightSC3::TGuruOfferId(0, 0, 0)));
}

TEST(UserRelevance, OrderRelevanceProperSerialization) {
    MarketSearch::SortingSpec ss;
    ss.set_debug(true);


    TRelevanceOrder inOrder(ERelevanceName::Main, ss);
    auto outOrder = TRelevanceOrder::Deserialize(inOrder.Serialize().c_str());

    EXPECT_EQ(ERelevanceName::Main, outOrder.GetRelevanceType());
    EXPECT_TRUE(outOrder.GetRelevanceArgsRef().debug());
}
