#include "common.h"

#include <crypta/lib/native/features_calculator/features_calculator.h>
#include <crypta/lookalike/lib/native/common.h>

#include <library/cpp/iterator/enumerate.h>
#include <library/cpp/iterator/zip.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/generic/algorithm.h>
#include <util/generic/hash.h>
#include <util/generic/string.h>
#include <util/generic/vector.h>

namespace NCrypta::NLookalike {
    NCrypta::TFeaturesMapping MakeFeaturesMapping() {
        NCrypta::TFeaturesMapping featuresMapping;
        const TString segments[] = {"216_648", "547_1058", "216_616", "601_261", "601_260", "601_263", "546_1302",
                                    "gender_0", "gender_1", "gender_2", "age_0", "age_1", "age_2", "age_3", "age_4",
                                    "age_5", "age_6", "income_0", "income_1", "income_2", "income_3", "income_4", "income_5"};
        for (const auto& [value, key] : Enumerate(segments)) {
            featuresMapping[key] = value;
        }
        return featuresMapping;
    }

    void AssertFloatVectorsEqual(const TVector<float> &left, const TVector<float> &right) {
        for (const auto&[li, ri] : Zip(left, right)) {
            UNIT_ASSERT_DOUBLES_EQUAL(li, ri, EPS);
        }
    }

    void AssertStringFloatVectorsEqual(const TVector<TString> &left, const TVector<TString> &right) {
        for (const auto&[li, ri] : Zip(left, right)) {
            UNIT_ASSERT_DOUBLES_EQUAL(FromString<float>(li), FromString<float>(ri), EPS);
        }
    }

    void AssertAffinitiesEqual(TVector<TString> &left, TVector<TString> &right) {
        Sort(left);
        Sort(right);
        for (const auto&[li, ri] : Zip(left, right)) {
            UNIT_ASSERT_STRINGS_EQUAL(li, ri);
        }
    }
}
