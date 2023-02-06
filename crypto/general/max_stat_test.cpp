#include <crypta/lib/native/stats/max_stat.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(MaxStatTest) {
    Y_UNIT_TEST(Empty) {
        TMaxStat stat("max");

        UNIT_ASSERT_DOUBLES_EQUAL(0, stat.GetAndReset(), 0.000001);
    }

    Y_UNIT_TEST(Add) {
        TMaxStat stat("max");

        stat.Add(100);
        stat.Add(600);
        stat.Add(200);

        UNIT_ASSERT_DOUBLES_EQUAL(600, stat.GetAndReset(), 0.000001);
    }

    Y_UNIT_TEST(Double) {
        TMaxStat stat("max");

        stat.Add(1.5123);
        stat.Add(0.47);
        stat.Add(1.421);

        UNIT_ASSERT_DOUBLES_EQUAL(1.5123, stat.GetAndReset(), 0.000001);
    }

    Y_UNIT_TEST(Negative) {
        TMaxStat stat("max");

        stat.Add(-1.5123);
        stat.Add(-0.47);
        stat.Add(-1.421);

        UNIT_ASSERT_DOUBLES_EQUAL(-0.47, stat.GetAndReset(), 0.000001);
    }

    Y_UNIT_TEST(Reset) {
        TMaxStat stat("max");

        stat.Add(100);
        stat.Add(200);

        UNIT_ASSERT_DOUBLES_EQUAL(200, stat.GetAndReset(), 0.000001);
        UNIT_ASSERT_DOUBLES_EQUAL(0, stat.GetAndReset(), 0.000001);
    }

    Y_UNIT_TEST(MultipleUpdatesAndResets) {
        TMaxStat stat("max");

        stat.Add(-1);
        stat.Add(1);
        stat.Add(3);

        UNIT_ASSERT_DOUBLES_EQUAL(3, stat.GetAndReset(), 0.000001);

        stat.Add(0);
        stat.Add(0);

        UNIT_ASSERT_DOUBLES_EQUAL(0, stat.GetAndReset(), 0.000001);

        stat.Add(3.333);
        stat.Add(0);
        stat.Add(0);

        UNIT_ASSERT_DOUBLES_EQUAL(3.333, stat.GetAndReset(), 0.000001);
    }
}
