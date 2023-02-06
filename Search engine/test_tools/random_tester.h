#pragma once

#include <search/web/configurable_rearrange/graph_applier.h>
#include <search/web/configurable_rearrange/configs/data_io.h>

class TRandomTester {
  using TResult = NConfigurableRearrange::TConfigurableRearrangeResult;
  private:
    struct TFeatureData {
        TString Name;
        EFactorSlice Slice;
        ui32 Id = 0;
        float MinValue = 0.0;
        float MaxValue = 1.0;
    };

  public:
    TRandomTester(const NConfigurableRearrange::TGraph& graph,
                  const TVector<const NConfigurableRearrange::TGraphPatch*>& patches, bool debug);
    TVector<TResult> RunFrom(i32 index, i32 numTests) const;
    TVector<TResult> Run(i32 numTests) const;
    TResult RunTest(i32 index) const;
    TResult Apply(const THashMap<std::pair<NFactorSlices::EFactorSlice,
                               TString>, float>& values) const;

  private:
    TFeatureData PrepareFeatureData(const NConfigurableRearrange::TNode::TFeature& feature) const;
    float generateFeatureValue(const TFeatureData& feature, i32 testCase) const;
    TResult ApplyVectorValues(const TVector<float>& values) const;

  private:
    TFactorDomain Domain;
    TVector<TFeatureData> Features;
    NConfigurableRearrange::TGraphApplier Applier;
    bool Debug;
};

void TestPatchesFromExportData(const NConfigurableRearrange::TExportData& data, i32 numTests);
