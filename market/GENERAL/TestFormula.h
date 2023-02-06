#pragma once
#include <market/library/recom/formula/Formula.h>

namespace Market
{
    namespace NRecomMn
    {
        class TTestFormula : public IMatrixNetFormula
        {
        public:
            virtual TStringBuf GetFeatureName(size_t) const {
                return "position";
            }
            virtual size_t GetFeatureCount() const {
                return 1;
            }
            virtual float Calculate(float* fFactor) const {
                return -fFactor[0];
            }
        };
    }
}
