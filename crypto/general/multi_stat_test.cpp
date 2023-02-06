#include <crypta/lib/native/stats/multi_stat.h>
#include <crypta/lib/native/stats/count_stat.h>
#include <crypta/lib/native/stats/hist_stat.h>

#include <library/cpp/testing/unittest/registar.h>

static constexpr double EPS = 1e-7;

Y_UNIT_TEST_SUITE(MultiStatTest) {
    Y_UNIT_TEST(SingleIncrement) {
        TMultiStat<TCountStat> stat("mc");

        stat.Add("x");
        stat.Add("y");
        stat.Add("y");

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(2u, values.size());
        UNIT_ASSERT_EQUAL(1, values.at("x"));
        UNIT_ASSERT_EQUAL(2, values.at("y"));
    }

    Y_UNIT_TEST(SingleAdd) {
        TMultiStat<TCountStat> stat("mc");

        stat.Add("x", 3);
        stat.Add("y", 10);
        stat.Add("y", 20);

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(2u, values.size());
        UNIT_ASSERT_EQUAL(3, values.at("x"));
        UNIT_ASSERT_EQUAL(30, values.at("y"));
    }

    Y_UNIT_TEST(SingleReset) {
        TMultiStat<TCountStat> stat("mc");

        stat.Add("x");
        stat.Add("y");
        stat.Add("y");

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(2u, values.size());
        UNIT_ASSERT_EQUAL(1, values.at("x"));
        UNIT_ASSERT_EQUAL(2, values.at("y"));

        UNIT_ASSERT(stat.GetAndReset().empty());
    }

    Y_UNIT_TEST(SingleMultipleUpdatesAndResets) {
        TMultiStat<TCountStat> stat("mc");

        stat.Add("x");
        stat.Add("y", 10);
        stat.Add("y");

        auto values1 = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(2u, values1.size());
        UNIT_ASSERT_EQUAL(1, values1.at("x"));
        UNIT_ASSERT_EQUAL(11, values1.at("y"));

        stat.Add("z");

        auto values2 = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(1u, values2.size());
        UNIT_ASSERT_EQUAL(1, values2.at("z"));
    }

    Y_UNIT_TEST(MapEmpty) {
        TMultiHistStat stat("hist", 100, 400, 3);

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(0u, values.size());
    }

    Y_UNIT_TEST(MapSimple) {
        TMultiHistStat stat("hist", 100, 400, 3);

        stat.Add("alpha", 99);
        stat.Add("alpha", 199);
        stat.Add("alpha", 299);
        stat.Add("alpha", 399);
        stat.Add("alpha", 401);
        stat.Add("beta", 401);

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(10u, values.size());
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("alpha.b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("alpha.b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("alpha.b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("alpha.b3"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("alpha.b4"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("beta.b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("beta.b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("beta.b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("beta.b3"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("beta.b4"), EPS);
    }

    Y_UNIT_TEST(Weights) {
        TMultiHistStat stat("hist", 0, 1, 2);

        stat.Add("alpha", 0, 1);
        stat.Add("alpha", 0.2, 1);
        stat.Add("alpha", 0.3, 1);
        stat.Add("alpha", 0.6, 1);
        stat.Add("alpha", 0.7, 2);
        stat.Add("alpha", 1.1, 4);
        stat.Add("beta", 0.7);

        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(8u, values.size());
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("alpha.b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(2, values.at("alpha.b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(3, values.at("alpha.b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(4, values.at("alpha.b3"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("beta.b0"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("beta.b1"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(1, values.at("beta.b2"), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(0, values.at("beta.b3"), EPS);
    }

    Y_UNIT_TEST(Reset) {
        TMultiHistStat stat("hist", 0, 1, 2);

        stat.Add("alpha", 0, 1);
        stat.Add("beta", 0.2, 1);
        stat.Add("gamma", 0.3, 1);
        stat.Add("delta", 0.6, 1);

        stat.GetAndReset(); // Reset
        auto values = stat.GetAndReset();

        UNIT_ASSERT_EQUAL(0u, values.size());
    }
}
