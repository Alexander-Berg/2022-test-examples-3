#include <crypta/lib/native/stats/percentile_stat.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/ymath.h>

static constexpr double EPS = 1e-7;

Y_UNIT_TEST_SUITE(PercentileStatTest) {
    Y_UNIT_TEST(Empty) {
        TPercentileStat<> stat("percentile", 400, 3);

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(0u, values.size());
    }

    Y_UNIT_TEST(Float) {
        TPercentileStat<> stat("percentile", 400, 3, {100, 99.9, 50});

        stat.Add(500);
        stat.Add(400);
        stat.Add(200, 999);

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(3u, values.size());
        UNIT_ASSERT_DOUBLES_EQUAL(200, values.at("p50"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(400, values.at("p99_9"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(500, values.at("p100"), EPS);
    }

    void PrecisionTest(i64 ref, i64 result, i32 precision) {
        i64 diff = ref - result;
        i64 maxDeviation = ref;
        for (int i = 0; i < precision; ++i) {
            maxDeviation /= 10;
        }
        UNIT_ASSERT_C(Abs(diff) < maxDeviation, TStringBuilder() << ref << " != " << result << " with precision " << precision << " (max deviation " << maxDeviation << ")");
    }

    Y_UNIT_TEST(PrecisionLoss) {
        const int PRECISION = 2;
        TPercentileStat<> stat("percentile", 10000, PRECISION, {100, 99.9, 99, 90, 80, 70, 60, 50});

        for (int i = 0; i <= 1000; ++i) {
            stat.Add(4000 + i);
        }

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(8u, values.size());

        PrecisionTest(4500, values.at("p50"), PRECISION);
        PrecisionTest(4600, values.at("p60"), PRECISION);
        PrecisionTest(4700, values.at("p70"), PRECISION);
        PrecisionTest(4800, values.at("p80"), PRECISION);
        PrecisionTest(4900, values.at("p90"), PRECISION);
        PrecisionTest(4990, values.at("p99"), PRECISION);
        PrecisionTest(4999, values.at("p99_9"), PRECISION);
        PrecisionTest(5000, values.at("p100"), PRECISION);
    }

    Y_UNIT_TEST(OnePercent) {
        TPercentileStat<> stat("percentile", 100, 3);

        stat.Add(95, 96);
        stat.Add(96);
        stat.Add(98);
        stat.Add(99);
        stat.Add(100);

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(4u, values.size());
        UNIT_ASSERT_DOUBLES_EQUAL(95, values.at("p95"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(98, values.at("p98"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(99, values.at("p99"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(100, values.at("p100"), EPS);
    }
}
