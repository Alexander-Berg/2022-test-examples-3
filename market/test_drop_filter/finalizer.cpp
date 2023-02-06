#include <library/cpp/testing/unittest/registar.h>
#include <yt/yt/core/misc/shutdown.h>

Y_UNIT_TEST_SUITE(ZEndTests) {
    Y_UNIT_TEST(ZEndTest) {
        NYT::Shutdown();
    }
}
