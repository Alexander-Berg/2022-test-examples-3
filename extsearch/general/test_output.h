#pragma once

/**
    @brief  test_output.h   This is interface to work with quality etalons.
                            Please try not to expose underlying types of etalon in this interface.
                            All should be done through TString variables. Underlying types are part
                            of realisation.
 */

#include <extsearch/video/robot/vt_extractor/lib/extractor.pb.h>
#include <yweb/video/protos/media.pb.h>

#include <util/generic/map.h>

namespace NVideo {
    /**
        @brief QualityTestOutput    returns serialized quality test etalon made from TVideosWithTexts.
     */
    TString QualityTestOutput(const NVtExtractor::TVideosWithTexts& res);

    /**
        @brief  QualityTestOutput   returns serialized quality test etalon.

        @param  urlsWithTexts   contains mapping "video url" -> {text for the video}
     */
    TString QualityTestOutput(const TMap<TString, TVector<TString>>& urlsWithTexts);

    /**
        @brief  QualityTestOutput   returns serailized quality test etalon made from TMediaProperties.
     */
    TString QualityTestOutput(const NMediaDB::TMediaProperties& media);

    struct TDiff {
        size_t ResultOnly = 0;
        size_t EtalonOnly = 0;
        size_t Common = 0;

        void MergeWith(const TDiff& other) noexcept;
    };

    struct TFullDiff {
        TDiff TextDiff;
        TDiff UrlDiff;

        void MergeWith(const TFullDiff& other) noexcept;
    };

    TDiff Diff(const TString& result, const TString& etalon);

    /**
        @brief  CompareQualityOutput    compares two contents of files that have format as
                                        QualityTestOutput returnes.
     */
    TFullDiff CompareQualityOutput(const TString& result, const TString& etalon);
}
