#pragma once

#include "answer.h"

#include <extsearch/images/robot/index/link_selector/library/link_factors.pb.h>
#include <util/generic/ptr.h>
#include <util/generic/vector.h>
#include <extsearch/images/robot/index/link_selector/library/ranker.h>

#include <cmath>

namespace NImages {
    namespace NLinkSelector {
        class TLinkRocQualityCounter {
        private:
            static constexpr auto PointsNumber = 100;

            struct TCurvePosition {
                size_t TruePositiveCount = 0;
                size_t FalsePositiveCount = 0;
                size_t FalseNegativeCount = 0;
                size_t TrueNegativeCount = 0;

                TCurvePosition(){};
                TCurvePosition(const TRankAnswer& data);
                void Next(float element);
                float CountTruePositiveRate() const;
                float CountFalsePositiveRate() const;
            };

            TRankAnswer RankedSet;
            size_t PositiveSetSize;

            void CountPositiveSetSize();
            TVector<TPoint> FilterPoints(const TVector<TPoint>& points) const;

        public:
            TLinkRocQualityCounter(const TRankAnswer& input);
            TVector<TPoint> GetRocCurve() const;
        };

        class TLinkRankerQualityCounter {
        private:
            TRankAnswer RankedSet;

            float GetPairNumber() const;
            static float WeightOfElement(float x);

        public:
            TLinkRankerQualityCounter(const TRankAnswer& input);

            float GetInversionQuality() const;
            float GetDCGQuality() const;
        };

        using TLinkRocQualityCounterPtr = TAtomicSharedPtr<TLinkRocQualityCounter>;
        using TLinkRankerQualityCounterPtr = TAtomicSharedPtr<TLinkRankerQualityCounter>;
    }
}
