#pragma once

#include "answer.h"

#include <util/stream/file.h>
#include <util/generic/ptr.h>
#include <util/generic/vector.h>
#include <util/stream/str.h>
#include <util/string/cast.h>

#include <extsearch/images/robot/index/link_selector/library/link_factors.pb.h>
#include <extsearch/images/robot/index/link_selector/library/ranker.h>

namespace NImages {
    namespace NLinkSelector {
        using TLinkFactorsPBPtr = TAtomicSharedPtr<TLinkFactorsPB>;

        class TFileReader {
        private:
            static constexpr auto Splitter = "\t";

            TIFStream InputFile;
            size_t TruthAspectsCount;
            TVector<TLinkFactorsPBPtr> Packages;
            TVector<TRankComparation> TruthLines;
            TVector<TString> ColumnNames;

            void ReadColumnNames();
            void GetRanksAndPackages();

        public:
            TFileReader(const TString& inputFile, size_t rankNumber);
            size_t size() const;
            const TRankComparation& operator[](size_t index) const;
            TLinkFactorsPBPtr GetPackage(size_t index) const;
            TVector<TString> GetColumnNames() const;
        };

        class TRankApplier {
        private:
            TVector<TRankComparation> RankedLines;

        public:
            TRankApplier(){};
            TRankApplier(const TFileReader& fileData, ILinkRankerPtr ranker);
            TRankAnswer GetTruthAspect(const TString& key) const;
            TRankAnswer GetCompositeTruthAspect(const THashMap<TString, float>& weights) const;
        };

        class TFileWriter {
        private:
            TOFStream OutputFile;

        public:
            TFileWriter(const TString& outputPath);
            void Print(float output);
            void Print(const TVector<TString>& output);
            void Print(const TVector<TPoint>& output);
        };
    }
}
