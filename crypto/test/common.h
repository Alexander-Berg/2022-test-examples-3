#pragma once

#include <crypta/lib/native/features_calculator/features_calculator.h>
#include <crypta/lookalike/lib/native/common.h>

#include <util/generic/string.h>
#include <util/generic/vector.h>
#include <util/string/join.h>
#include <util/system/env.h>

#include <library/cpp/testing/common/env.h>

namespace NCrypta::NLookalike {
    static const TString TEST_DATA_DIR = GetWorkPath();
    static const TString SEGMETS_DICT_FILE_NAME = Join('/', TEST_DATA_DIR, "segments_dict");
    static const TString DSSM_MODEL_FILE_NAME = Join('/', TEST_DATA_DIR, "dssm_lal_model.applier");
    static const int SITE2VEC_SIZE = 512;
    static const float EPS = 0.0001;

    TFeaturesMapping MakeFeaturesMapping();

    void AssertFloatVectorsEqual(const TVector<float>& left, const TVector<float>& right);

    void AssertStringFloatVectorsEqual(const TVector<TString>& left, const TVector<TString>& right);

    void AssertAffinitiesEqual(TVector<TString>& left, TVector<TString>& right);
}
