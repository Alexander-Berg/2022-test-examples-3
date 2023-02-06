#pragma once

#include <extsearch/images/base/factors/factor_names.h>

#include <kernel/keyinv/hitlist/full_pos.h>
#include <library/cpp/wordpos/wordpos.h>

#include <util/generic/vector.h>

namespace NImages {
    namespace NRelevance {
        using TDocumentHits = TVector<TFullPositionEx>;

        void ReadTestData(const TString& filePath, TVector<TVector<float>>& wordWeights, TVector<TVector<TDocumentHits>>& hits);
        void ReadTestAns(const TString& filePath, TVector<TVector<float>>& ans, size_t testCount);
    }
}
