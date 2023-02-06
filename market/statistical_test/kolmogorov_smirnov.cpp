#include <market/kombat/engine/statistical_test/kolmogorov_smirnov.h>

#include <cmath>
#include <util/generic/algorithm.h>

using namespace NMarket::NKombat::NStatisticalTest::NKolmogorovSmirnov;

namespace {
    using NMarket::NShiny::NLunapark::TTimeBucket;

    void CheckArguments(const TDistribution& base, const TDistribution& test, double significance) {
        if (base.empty() || test.empty()) {
            throw yexception() << "Distributions should be non-empty: base.size() == "
                               << base.size() << ", test.size() == " << test.size();
        }

        if (significance <= 0.0 || significance >= 1.0) {
            throw yexception() << "Significance should be lesser than 1.0 and greater than 0.0"
                               << " but significance == " << significance;
        }

        const auto cmp = [](const TTimeBucket& lhs, const TTimeBucket& rhs) {
            return lhs.Time < rhs.Time;
        };
        if (!IsSorted(base.begin(), base.end(), cmp) || !IsSorted(test.begin(), test.end(), cmp)) {
            throw yexception() << "Distributions should be sorted by time";
        }
    }

    ui64 CountSamples(const TDistribution& distribution) {
        return Accumulate(distribution, ui64(0), [](ui64 accum, const TTimeBucket& bucket) {
            return accum + bucket.Count;
        });
    }

    double ComputeRejection(const size_t n, const size_t m, double significance) {
        const double alpha = sqrt(-0.5 * log(significance));
        return alpha * sqrt(1.0 / m + 1.0 / n);
    }

    void NextStep(const TDistribution& distr, double& val, TDuration& time, size_t& idx) {
        val = distr[idx].Percentile;
        time = distr[idx].Time;
        ++idx;
    }

    TTestResult FindMaxAbs(const TDistribution& base, const TDistribution& test) {
        TTestResult result;
        double baseVal = 0.0;
        double testVal = 0.0;
        TDuration baseTime;
        TDuration testTime;
        size_t baseIt = 0;
        size_t testIt = 0;
        while (baseIt < base.size() && testIt < test.size()) {
            if (base[baseIt].Time < test[testIt].Time) {
                NextStep(base, baseVal, baseTime, baseIt);
            } else if (base[baseIt].Time > test[testIt].Time) {
                NextStep(test, testVal, testTime, testIt);
            } else {
                NextStep(base, baseVal, baseTime, baseIt);
                NextStep(test, testVal, testTime, testIt);
            }

            const double newAbsDiff = fabs(baseVal - testVal);
            if (newAbsDiff >= result.MaxAbsDiffPercent) {
                result.MaxAbsDiffPercent = newAbsDiff;
                result.BasePercentile = baseVal;
                result.TestPercentile = testVal;
                result.BaseTime = baseTime;
                result.TestTime = testTime;
            }
        }

        return result;
    }
}

namespace NMarket::NKombat::NStatisticalTest::NKolmogorovSmirnov {
    TTestResult PerformTest(const TDistribution& base, const TDistribution& test, double significance) {
        CheckArguments(base, test, significance);

        const auto rejection = ComputeRejection(CountSamples(base), CountSamples(test), significance);
        auto result = FindMaxAbs(base, test);

        result.Rejection = rejection;
        result.Accept = result.MaxAbsDiffPercent / 100.0 <= rejection;
        return result;
    }
}
