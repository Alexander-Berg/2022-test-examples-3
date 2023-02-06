#include <market/kombat/engine/statistical_test/kolmogorov_smirnov.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket::NKombat::NStatisticalTest;
using namespace NMarket::NShiny::NLunapark;

namespace {
    constexpr double EPSILON = 1e-5;
}

Y_UNIT_TEST_SUITE(TKolmogorovSmirnovTestSuite) {
    Y_UNIT_TEST(TestSameDistributions) {
        constexpr size_t BUCKET_COUNT = 1000;
        constexpr ui64 MULTIPLIER = 10;
        constexpr ui64 TOTAL = MULTIPLIER * BUCKET_COUNT * (BUCKET_COUNT + 1) / 2;
        constexpr double EXPECTED_MAX_ABS_DIFF_PERCENT = 0.0;

        TVector<TTimeBucket> distribution(BUCKET_COUNT);
        ui64 accum = 0;
        for (size_t i = 0; i < BUCKET_COUNT; ++i) {
            const ui64 countAndTime = (i + 1) * MULTIPLIER;
            accum += countAndTime;

            distribution[i].Time = TDuration::MicroSeconds(countAndTime);
            distribution[i].Count = countAndTime;
            distribution[i].Percentile = static_cast<double>(100 * accum) / TOTAL;
        }

        const auto result = NKolmogorovSmirnov::PerformTest(distribution, distribution, 0.1);
        UNIT_ASSERT_DOUBLES_EQUAL(result.MaxAbsDiffPercent, EXPECTED_MAX_ABS_DIFF_PERCENT, EPSILON);
    }

    Y_UNIT_TEST(TestSameCumulativeFunctionsOfDifferentDistributions) {
        constexpr size_t BUCKET_COUNT = 1000;
        constexpr ui64 MULTIPLIER = 2;
        constexpr ui64 BASE_TOTAL = BUCKET_COUNT * (BUCKET_COUNT + 1) / 2;
        constexpr double EXPECTED_MAX_ABS_DIFF_PERCENT = 0.0;

        TVector<TTimeBucket> base(BUCKET_COUNT);
        TVector<TTimeBucket> test(BUCKET_COUNT);
        ui64 accum = 0;
        for (ui64 i = 0; i < BUCKET_COUNT; ++i) {
            accum += 1;
            double commonPercentile = static_cast<double>(100 * accum) / BASE_TOTAL;

            base[i].Time = TDuration::MicroSeconds(1 + i);
            base[i].Count = 1;
            base[i].Percentile = commonPercentile;

            test[i].Time = TDuration::MicroSeconds(1 + i);
            test[i].Count = MULTIPLIER * base[i].Count;
            test[i].Percentile = commonPercentile;
        }

        const auto result = NKolmogorovSmirnov::PerformTest(base, test, 0.1);
        UNIT_ASSERT_DOUBLES_EQUAL(result.MaxAbsDiffPercent, EXPECTED_MAX_ABS_DIFF_PERCENT, EPSILON);
    }

    Y_UNIT_TEST(TestSymmetrical) {
        constexpr double EXPECTED_MAX_ABS_DIFF_PERCENT = 75.0;

        TVector<TTimeBucket> base(3);
        TVector<TTimeBucket> test(4);

        // Base
        base[0].Time = TDuration::MicroSeconds(1);
        base[0].Count = 1;
        base[0].Percentile = 100.0 * 1.0 / 3.0;

        base[1].Time = TDuration::MicroSeconds(4);
        base[1].Count = 1;
        base[1].Percentile = 100.0 * 2.0 / 3.0;

        base[2].Time = TDuration::MicroSeconds(8);
        base[2].Count = 1;
        base[2].Percentile = 100.0;

        // Test
        test[0].Time = TDuration::MicroSeconds(2);
        test[0].Count = 1;
        test[0].Percentile = 100.0 * 1.0 / 4.0;

        test[1].Time = TDuration::MicroSeconds(9);
        test[1].Count = 1;
        test[1].Percentile = 100.0 * 2.0 / 4.0;

        test[2].Time = TDuration::MicroSeconds(10);
        test[2].Count = 1;
        test[2].Percentile = 100.0 * 3.0 / 4.0;

        test[3].Time = TDuration::MicroSeconds(12);
        test[3].Count = 1;
        test[3].Percentile = 100.0;

        {
            const auto result = NKolmogorovSmirnov::PerformTest(base, test, 0.1);
            UNIT_ASSERT_DOUBLES_EQUAL(result.MaxAbsDiffPercent, EXPECTED_MAX_ABS_DIFF_PERCENT, EPSILON);
        }

        {
            const auto result = NKolmogorovSmirnov::PerformTest(test, base, 0.1);
            UNIT_ASSERT_DOUBLES_EQUAL(result.MaxAbsDiffPercent, EXPECTED_MAX_ABS_DIFF_PERCENT, EPSILON);
        }
    }

    Y_UNIT_TEST(TestSeveralMaximums) {
        constexpr double EXPECTED_MAX_ABS_DIFF_PERCENT = 100.0 * 1.0 / 3.0;

        TVector<TTimeBucket> base(3);
        TVector<TTimeBucket> test(3);

        // Base
        base[0].Time = TDuration::MicroSeconds(1);
        base[0].Count = 1;
        base[0].Percentile = 100.0 * 1.0 / 3.0;

        base[1].Time = TDuration::MicroSeconds(2);
        base[1].Count = 1;
        base[1].Percentile = 100.0 * 2.0 / 3.0;

        base[2].Time = TDuration::MicroSeconds(3);
        base[2].Count = 1;
        base[2].Percentile = 100.0;

        // Test
        test[0].Time = TDuration::MicroSeconds(2);
        test[0].Count = 1;
        test[0].Percentile = 100.0 * 1.0 / 3.0;

        test[1].Time = TDuration::MicroSeconds(3);
        test[1].Count = 1;
        test[1].Percentile = 100.0 * 2.0 / 3.0;

        test[2].Time = TDuration::MicroSeconds(4);
        test[2].Count = 1;
        test[2].Percentile = 100.0;

        {
            const auto result = NKolmogorovSmirnov::PerformTest(base, test, 0.1);
            UNIT_ASSERT_DOUBLES_EQUAL(result.MaxAbsDiffPercent, EXPECTED_MAX_ABS_DIFF_PERCENT, EPSILON);
        }
    }

    Y_UNIT_TEST(TestFewSteps) {
        constexpr double EXPECTED_MAX_ABS_DIFF_PERCENT = 50.0;

        TVector<TTimeBucket> base(2);
        TVector<TTimeBucket> test(6);

        // Base
        base[0].Time = TDuration::MicroSeconds(1);
        base[0].Count = 1;
        base[0].Percentile = 50.0;

        base[1].Time = TDuration::MicroSeconds(10);
        base[1].Count = 1;
        base[1].Percentile = 100.0;

        // Test
        test[0].Time = TDuration::MicroSeconds(1);
        test[0].Count = 1;
        test[0].Percentile = 100.0 * 1.0 / 6.0;

        test[1].Time = TDuration::MicroSeconds(4);
        test[1].Count = 1;
        test[1].Percentile = 100.0 * 2.0 / 6.0;

        test[2].Time = TDuration::MicroSeconds(10);
        test[2].Count = 1;
        test[2].Percentile = 50.0;

        test[3].Time = TDuration::MicroSeconds(12);
        test[3].Count = 1;
        test[3].Percentile = 100.0 * 4.0 / 6.0;

        test[4].Time = TDuration::MicroSeconds(14);
        test[4].Count = 1;
        test[4].Percentile = 100.0 * 5.0 / 6.0;

        test[5].Time = TDuration::MicroSeconds(18);
        test[5].Count = 1;
        test[5].Percentile = 100.0;

        {
            const auto result = NKolmogorovSmirnov::PerformTest(base, test, 0.1);
            UNIT_ASSERT_DOUBLES_EQUAL(result.MaxAbsDiffPercent, EXPECTED_MAX_ABS_DIFF_PERCENT, EPSILON);
        }
    }

    Y_UNIT_TEST(TestShiftedSteps) {
        constexpr double EXPECTED_MAX_ABS_DIFF_PERCENT = 99.0;

        TVector<TTimeBucket> base(2);
        TVector<TTimeBucket> test(3);

        // Base
        base[0].Time = TDuration::MicroSeconds(1);
        base[0].Count = 1;
        base[0].Percentile = 50.0;

        base[1].Time = TDuration::MicroSeconds(10);
        base[1].Count = 1;
        base[1].Percentile = 100.0;

        // Test
        test[0].Time = TDuration::MicroSeconds(5);
        test[0].Count = 1;
        test[0].Percentile = 100.0 * 1.0 / 100.0;

        test[1].Time = TDuration::MicroSeconds(15);
        test[1].Count = 98;
        test[1].Percentile = 100.0 * 1.0 / 99.0;

        test[2].Time = TDuration::MicroSeconds(20);
        test[2].Count = 1;
        test[2].Percentile = 100.0;

        {
            const auto result = NKolmogorovSmirnov::PerformTest(base, test, 0.1);
            UNIT_ASSERT_DOUBLES_EQUAL(result.MaxAbsDiffPercent, EXPECTED_MAX_ABS_DIFF_PERCENT, EPSILON);
        }
    }

    Y_UNIT_TEST(TestDegeneratedCases) {
        TVector<TTimeBucket> base(1);
        TVector<TTimeBucket> test(3);

        // Base
        base[0].Time = TDuration::MicroSeconds(5);
        base[0].Count = 1;
        base[0].Percentile = 100.0;

        // Test
        test[0].Time = TDuration::MicroSeconds(1);
        test[0].Count = 1;
        test[0].Percentile = 100.0 * 1.0 / 3.0;

        test[1].Time = TDuration::MicroSeconds(5);
        test[1].Count = 1;
        test[1].Percentile = 100.0 * 2.0 / 3.0;

        test[2].Time = TDuration::MicroSeconds(20);
        test[2].Count = 1;
        test[2].Percentile = 100.0;

        {
            constexpr double EXPECTED_MAX_ABS_DIFF_PERCENT = 100.0 / 3.0;
            const auto result = NKolmogorovSmirnov::PerformTest(base, test, 0.1);
            UNIT_ASSERT_DOUBLES_EQUAL(result.MaxAbsDiffPercent, EXPECTED_MAX_ABS_DIFF_PERCENT, EPSILON);
        }

        // Test
        test.resize(1);
        test[0].Time = TDuration::MicroSeconds(1);
        test[0].Count = 1;
        test[0].Percentile = 100.0;

        {
            constexpr double EXPECTED_MAX_ABS_DIFF_PERCENT = 100.0;
            const auto result = NKolmogorovSmirnov::PerformTest(base, test, 0.1);
            UNIT_ASSERT_DOUBLES_EQUAL(result.MaxAbsDiffPercent, EXPECTED_MAX_ABS_DIFF_PERCENT, EPSILON);
        }
    }
}
