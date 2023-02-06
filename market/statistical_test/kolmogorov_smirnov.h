#pragma once

#include <market/library/shiny/external/lunapark/lunapark.h>

/// See: https://en.wikipedia.org/wiki/Kolmogorov-Smirnov_test#Two-sample_Kolmogorov–Smirnov_test
namespace NMarket::NKombat::NStatisticalTest::NKolmogorovSmirnov {
    using TDistribution = TVector<NMarket::NShiny::NLunapark::TTimeBucket>;

    struct TTestResult {
        double Rejection = 0.0;
        double MaxAbsDiffPercent = 0.0;
        double BasePercentile = 0.0;
        double TestPercentile = 0.0;
        TDuration BaseTime;
        TDuration TestTime;
        bool Accept = false;
    };

    /// @note: base and test should be sorted by 'Time' and 0.0 < significance < 1.0
    TTestResult PerformTest(const TDistribution& base, const TDistribution& test, double significance);
}
