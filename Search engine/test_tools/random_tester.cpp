#include "random_tester.h"

#include <library/cpp/testing/unittest/registar.h>
#include <util/generic/algorithm.h>
#include <util/generic/string.h>
#include <util/random/fast.h>

using namespace NConfigurableRearrange;
using namespace NFactorSlices;

using TResult = NConfigurableRearrange::TConfigurableRearrangeResult;

TRandomTester::TRandomTester(const TGraph& graph, const TVector<const TGraphPatch*>& patches, bool debug = false)
    : Applier(graph, patches)
    , Debug(debug)
{
    Applier.UseTestingHints = true;
    THashMap<EFactorSlice, ui32> maxId;
    for (const TNode& node : Applier.GetEffectiveGraph().GetNodes()) {
        if (node.HasFeature()) {
            TFeatureData feature = PrepareFeatureData(node.GetFeature());
            maxId[feature.Slice] = Max(maxId[feature.Slice], feature.Id);
            Features.push_back(std::move(feature));
        }
    }
    TSlicesMetaInfo meta;
    for(const auto& [slice, value] : maxId) {
        meta.SetNumFactors(slice, value + 1);
        meta.SetSliceEnabled(slice, true);
        auto cur = slice;
        while (cur != EFactorSlice::ALL) {
            cur = GetSlicesInfo()->GetParent(cur);
            meta.SetSliceEnabled(cur, true);
        }
    }
    Domain = TFactorDomain(meta);
}


TVector<TResult> TRandomTester::RunFrom(i32 index, i32 numTests) const {
    TVector<TResult> result;
    if(Debug) {
        Cerr << "Affected features:" << Endl;
        for(ui32 i : xrange(Features.size())) {
            Cerr << Features[i].Name  << "[" << Features[i].Slice  << "]" << Endl;
        }
    }
    for(i32 i : xrange(index, index + numTests)) {
        result.push_back(RunTest(i));
    }
    return result;
}


TVector<TResult> TRandomTester::Run(i32 numTests) const {
    return RunFrom(0, numTests);
}


TResult TRandomTester::Apply(const THashMap<std::pair<EFactorSlice, TString>, float>& values) const {
    TVector<float> res;
    for(const auto& feature : Features) {
        auto key = std::make_pair(feature.Slice, feature.Name);
        res.push_back(values.count(key) ? values.at(key) : 0.0f);
    }
    return ApplyVectorValues(res);
}


TRandomTester::TFeatureData TRandomTester::PrepareFeatureData(const TNode::TFeature& feature) const {
    TFeatureData result;
    result.Name = feature.GetName();
    result.Slice = FromString(feature.GetSlice());
    result.Id = feature.GetPrivate_ResolvedInSliceId();
    auto info = NFactorSlices::GetSlicesInfo()->GetFactorsInfo(result.Slice);
    result.MaxValue = info->GetMaxValue(result.Id);
    result.MinValue = info->GetMinValue(result.Id);
    return result;
}


float TRandomTester::generateFeatureValue(const TFeatureData& feature, i32 testCase) const {
    THash<TString> hasher;
    i64 seed =  hasher(feature.Name) + static_cast<i32>(feature.Slice) + testCase;
    i64 rnd = TFastRng32(seed, 0).Uniform(101);
    float value = feature.MinValue + feature.MaxValue / 100.0f * rnd - feature.MinValue / 100.0f * rnd;
    return value;
}


TResult TRandomTester::ApplyVectorValues(const TVector<float>& values) const {
    UNIT_ASSERT_EQUAL(values.size(), Features.size());
    TFactorStorage doc(Domain);
    for(int i : xrange(Features.size())) {
        doc.CreateViewFor(Features[i].Slice)[Features[i].Id] = values[i];
    }
    return Applier.Apply(&doc, Debug);
}


TResult TRandomTester::RunTest(i32 index) const {
    TVector<float> values;
    for(const TFeatureData& feature : Features) {
        values.push_back(generateFeatureValue(feature, index));
    }
    TResult res = ApplyVectorValues(values);

    if(Debug) {
        Cerr << "TestCase " << index << "; Values: ";
        for(auto& val : values) {
            Cerr << val << "\t";
        }
        Cerr << "-> " << res.BoostValue << Endl;
    }
    return res;
}


void TestPatchesFromExportData(const TExportData& data, i32 numTests) {
    const float eps = 1e-6;
    TStringStream out;
    ui32 incorrectPatchCounter = 0;

    for(const auto& patch : data.GetPatches()) {
        const TGraph* graph = FindGraph(data, patch.GetDefaultBaseGraph());
        UNIT_ASSERT_C(graph != nullptr, "Graph \"" << patch.GetDefaultBaseGraph() << "\" not found");

        TRandomTester patchedGraph(*graph, {&patch}, false);
        TRandomTester defaultGraph(*graph, {}, false);

        bool patchIsCorrect = AnyOf(xrange(numTests), [&](const auto& i) {
            TResult defaultValue = defaultGraph.RunTest(i);
            TResult cur = patchedGraph.RunTest(i);
            return std::abs(cur.BoostValue - defaultValue.BoostValue) > eps ||
                    cur.Markers != defaultValue.Markers;
        });

        if(patchIsCorrect == false) {
            ++incorrectPatchCounter;
            out << "Patch \"" << patch.GetPatchName() << "\" from namespace \"" << patch.GetNamespace() <<
            "\" doesn't change the graph result on random values." << Endl;
        }
    }

    UNIT_ASSERT_C(incorrectPatchCounter == 0, out.Str());
}
