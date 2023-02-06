#include <crypta/lib/native/periodic_task/periodic_task.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/deprecated/atomic/atomic.h>

using namespace NCrypta;

Y_UNIT_TEST_SUITE(TPeriodicTask) {
    Y_UNIT_TEST(Basic) {
        TDuration timeout = TDuration::MilliSeconds(100);
        TAtomic counter = 0;
        TPeriodicTask runner(timeout, [&](){ AtomicIncrement(counter); }, "thread");

        runner.Start();
        Sleep(timeout / 2);
        for (int i = 0; i < 10; ++i) {
            UNIT_ASSERT_EQUAL(i, AtomicGet(counter));
            Sleep(timeout);
        }
        runner.Stop();
        for (int i = 1; i < 10; ++i) {
            UNIT_ASSERT_EQUAL(10, AtomicGet(counter));
            Sleep(timeout);
        }
        runner.Start();
        Sleep(timeout / 2);
        for (int i = 10; i < 20; ++i) {
            UNIT_ASSERT_EQUAL(i, AtomicGet(counter));
            Sleep(timeout);
        }
    }
}
