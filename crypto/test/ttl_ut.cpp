#include "shifted_clock.h"
#include "ttl.h"

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta;

Y_UNIT_TEST_SUITE(Ttl) {
    Y_UNIT_TEST(ExplicitNow) {
        const TTtl ttl(100, 1500000100);
        UNIT_ASSERT(ttl.IsExpired(1499999999));
        UNIT_ASSERT(!ttl.IsExpired(1500000000));
    }

    Y_UNIT_TEST(ImplicitNow) {
        TShiftedClock::FreezeTimestamp(1500000100);

        const TTtl ttl(100);
        UNIT_ASSERT(ttl.IsExpired(1499999999));
        UNIT_ASSERT(!ttl.IsExpired(1500000000));
    }

    Y_UNIT_TEST(ZeroTtl) {
        const TTtl ttl(0, 1500000000);
        UNIT_ASSERT(!ttl.IsExpired(1499999999));
        UNIT_ASSERT(!ttl.IsExpired(1500000000));
    }
}
