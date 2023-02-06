#pragma once

#include <util/generic/ptr.h>
#include <util/generic/vector.h>
#include <extsearch/images/robot/index/link_selector/library/link_factors.pb.h>
#include <extsearch/images/robot/index/link_selector/library/ranker.h>
#include <util/generic/hash.h>

namespace NImages {
    namespace NLinkSelector {
        using TLinkFactorsPBPtr = TAtomicSharedPtr<TLinkFactorsPB>;

        struct TPoint {
            float X = 0;
            float Y = 0;
            TPoint(float x, float y)
                : X(x)
                , Y(y)
            {
            }
        };

        struct TRankPair {
            float Truth;
            float Result;
            bool operator<(const TRankPair& second) const;
            bool operator==(const TRankPair& second);

            TRankPair(){};
            TRankPair(float truth, float result)
                : Truth(truth)
                , Result(result)
            {
            }
            bool LessByTruth(const TRankPair& second) const;
        };

        class TRankAnswer {
        private:
            TVector<TRankPair> RankedSet;

        public:
            TRankAnswer(){};
            size_t size() const;
            TRankPair operator[](const size_t index) const;
            void SortDataByResult();
            void ReverseData();
            void Add(TRankPair newElement);
        };

        class TRankComparation {
        private:
            float RankerResult;
            THashMap<TString, float> Truth;

        public:
            TRankComparation(){};
            void AddTruth(const TString& key, float value);
            void SetRankerResult(float result);
            TRankPair GetRank(const TString& key) const;
            TRankPair GetRank(const THashMap<TString, float>& weights) const;
        };
    }
}
