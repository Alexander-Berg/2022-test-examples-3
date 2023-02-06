#include <crypta/lib/native/stats/avg_stat.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(AvgStatTest) {
    Y_UNIT_TEST(Empty) {
        TAvgStat stat("avg");

        UNIT_ASSERT_DOUBLES_EQUAL(0, stat.GetAndReset(), 0.000001);
    }

    Y_UNIT_TEST(Add) {
        TAvgStat stat("avg");

        stat.Add(100);
        stat.Add(200);
        stat.Add(600);

        UNIT_ASSERT_DOUBLES_EQUAL(300, stat.GetAndReset(), 0.000001);
    }

    Y_UNIT_TEST(AddWithCount) {
        TAvgStat stat("avg");

        stat.Add(100, 2);
        stat.Add(200, 4);

        UNIT_ASSERT_DOUBLES_EQUAL(50, stat.GetAndReset(), 0.000001);
    }

    Y_UNIT_TEST(Double) {
        TAvgStat stat("avg");

        stat.Add(1.5);
        stat.Add(0.47);
        stat.Add(40);

        UNIT_ASSERT_DOUBLES_EQUAL(13.99, stat.GetAndReset(), 0.000001);
    }

    Y_UNIT_TEST(Reset) {
        TAvgStat stat("avg");

        stat.Add(100);
        stat.Add(200);

        UNIT_ASSERT_DOUBLES_EQUAL(150, stat.GetAndReset(), 0.000001);
        UNIT_ASSERT_DOUBLES_EQUAL(0, stat.GetAndReset(), 0.000001);
    }

    Y_UNIT_TEST(MultipleUpdatesAndResets) {
        TAvgStat stat("avg");

        stat.Add(-1);
        stat.Add(1);
        stat.Add(3);

        UNIT_ASSERT_DOUBLES_EQUAL(1, stat.GetAndReset(), 0.000001);

        stat.Add(0);
        stat.Add(0);

        UNIT_ASSERT_DOUBLES_EQUAL(0, stat.GetAndReset(), 0.000001);

        stat.Add(3.333);
        stat.Add(0);
        stat.Add(0);

        UNIT_ASSERT_DOUBLES_EQUAL(1.111, stat.GetAndReset(), 0.000001);
    }
}
