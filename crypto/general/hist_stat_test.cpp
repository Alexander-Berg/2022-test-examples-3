#include <crypta/lib/native/stats/hist_stat.h>

#include <library/cpp/testing/unittest/registar.h>

static constexpr double EPS = 1e-7;

Y_UNIT_TEST_SUITE(HistStatTest) {
    Y_UNIT_TEST(Empty) {
        THistStat<> stat("hist", 100, 400, 3);

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(5u, values.size());
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b3"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b4"), EPS);
    }

    Y_UNIT_TEST(Tail) {
        THistStat<> stat("hist", 100, 400, 3);

        stat.Add(401, 10);

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(5u, values.size());
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b3"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(10, values.at("b4"), EPS);
    }

    Y_UNIT_TEST(Min) {
        THistStat<> stat("hist", 100, 400, 3);

        stat.Add(-301, 10);

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(5u, values.size());
        UNIT_ASSERT_DOUBLES_EQUAL(10, values.at("b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b3"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b4"), EPS);
    }

    Y_UNIT_TEST(EachBin) {
        THistStat<> stat("hist", 100, 400, 3);

        stat.Add(99);
        stat.Add(199);
        stat.Add(299);
        stat.Add(399);
        stat.Add(401);

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(5u, values.size());
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("b3"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("b4"), EPS);
    }

    Y_UNIT_TEST(EachBinBorder) {
        THistStat<> stat("hist", 100, 400, 3);

        stat.Add(100);
        stat.Add(200);
        stat.Add(300);
        stat.Add(400);

        auto values = stat.GetAndReset();

        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("b3"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b4"), EPS);
    }

    Y_UNIT_TEST(Weights) {
        THistStat<> stat("hist", 0, 1, 2);

        stat.Add(0, 1);
        stat.Add(0.2, 1);
        stat.Add(0.3, 1);
        stat.Add(0.6, 1);
        stat.Add(0.7, 2);
        stat.Add(1.1, 4);

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(4u, values.size());
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(2, values.at("b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(3, values.at("b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(4, values.at("b3"), EPS);
    }


    Y_UNIT_TEST(AddOtherStat) {
        THistStat<TStatLockingPolicy> stat("hist", 0, 1, 2);
        stat.Add(0, 1);
        stat.Add(0.2, 1);
        stat.Add(0.7, 1);
        stat.Add(1.1, 2);

        THistStat<TStatNonLockingPolicy> otherStat("hist", 0, 1, 2);
        otherStat.Add(0.3, 1);
        otherStat.Add(0.6, 1);
        otherStat.Add(0.7, 1);
        otherStat.Add(1.1, 2);

        stat.Add(otherStat);

        auto values = stat.GetAndReset();
        UNIT_ASSERT_EQUAL(4u, values.size());
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(2, values.at("b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(3, values.at("b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(4, values.at("b3"), EPS);

        auto otherValues = otherStat.GetAndReset();
        UNIT_ASSERT_EQUAL(4u, otherValues.size());
        UNIT_ASSERT_DOUBLES_EQUAL(0, otherValues.at("b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, otherValues.at("b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(2, otherValues.at("b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(2, otherValues.at("b3"), EPS);
    }

    Y_UNIT_TEST(GetAndReset) {
        THistStat<> stat("hist", 0, 1, 2);

        stat.Add(0, 1);
        stat.Add(0.2, 1);
        stat.Add(0.3, 1);
        stat.Add(0.6, 1);
        stat.Add(0.7, 2);
        stat.Add(1.1, 4);

        stat.GetAndReset();
        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(4u, values.size());
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b3"), EPS);
    }

    Y_UNIT_TEST(Reset) {
        THistStat<> stat("hist", 0, 1, 2);

        stat.Add(0, 1);
        stat.Add(0.2, 1);
        stat.Add(0.3, 1);
        stat.Add(0.6, 1);
        stat.Add(0.7, 2);
        stat.Add(1.1, 4);

        stat.Reset();
        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(4u, values.size());
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("b3"), EPS);
    }

    Y_UNIT_TEST(Minimal) {
        THistStat<> stat("hist", 0, 1, 1);

        stat.Add(-1, 1);
        stat.Add(0, 2);
        stat.Add(1, 3);
        stat.Add(2, 4);

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(3u, values.size());
        UNIT_ASSERT_DOUBLES_EQUAL(3, values.at("b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(3, values.at("b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(4, values.at("b2"), EPS);
    }

    Y_UNIT_TEST(MinimalNonLocking) {
        //TODO: Rewrite all tests to support both locking and non-locking policy
        THistStat<TStatNonLockingPolicy> stat("hist", 0, 1, 1);

        stat.Add(-1, 1);
        stat.Add(0, 2);
        stat.Add(1, 3);
        stat.Add(2, 4);

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(3u, values.size());
        UNIT_ASSERT_DOUBLES_EQUAL(3, values.at("b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(3, values.at("b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(4, values.at("b2"), EPS);
    }

    Y_UNIT_TEST(IncompatibleStats) {
        {
            THistStat<> stat1("hist", 0, 1, 2);
            THistStat<> stat2("hist", 0, 1, 1);
            UNIT_CHECK_GENERATED_EXCEPTION_C(stat1.Add(stat2), yexception, "stat1 and stat2 must be reported incompatible");
        }

        {
            THistStat<> stat1("hist", 0, 1, 2);
            THistStat<> stat2("hist", 0, 1.1, 2);
            UNIT_CHECK_GENERATED_EXCEPTION_C(stat1.Add(stat2), yexception, "stat1 and stat must be reported incompatible");
        }

        {
            THistStat<> stat1("hist", 0, 1, 2);
            THistStat<> stat2("hist", 0.1, 1, 2);
            UNIT_CHECK_GENERATED_EXCEPTION_C(stat1.Add(stat2), yexception, "stat1 and stat must be reported incompatible");
        }
    }
}
