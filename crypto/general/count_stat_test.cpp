#include <crypta/lib/native/stats/count_stat.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(CountStatTest) {
    Y_UNIT_TEST(Increment) {
        TCountStat stat("count");

        stat.Add();

        UNIT_ASSERT_DOUBLES_EQUAL(1, stat.GetAndReset(), 0.000001);
    }

    Y_UNIT_TEST(Add) {
        TCountStat stat("count");

        stat.Add(100);

        UNIT_ASSERT_DOUBLES_EQUAL(100, stat.GetAndReset(), 0.000001);
    }

    Y_UNIT_TEST(Reset) {
        TCountStat stat("count");

        stat.Add(100);

        UNIT_ASSERT_DOUBLES_EQUAL(100, stat.GetAndReset(), 0.000001);
        UNIT_ASSERT_DOUBLES_EQUAL(0, stat.GetAndReset(), 0.000001);
    }

    Y_UNIT_TEST(MultipleUpdatesAndResets) {
        TCountStat stat("count");

        stat.Add();
        stat.Add();
        stat.Add();

        UNIT_ASSERT_DOUBLES_EQUAL(3, stat.GetAndReset(), 0.000001);

        stat.Add(100);
        stat.Add();

        UNIT_ASSERT_DOUBLES_EQUAL(101, stat.GetAndReset(), 0.000001);
    }
}
