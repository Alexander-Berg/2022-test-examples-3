#include <crypta/cm/services/common/expiration/need_touch.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace NCrypta::NCm;

TEST(NeedTouch, All) {
    const auto& touchTimeout = TDuration::Seconds(500);
    const auto& oldTouch = TInstant::Seconds(100);

    ASSERT_FALSE(NeedTouch(oldTouch, oldTouch + touchTimeout, touchTimeout));
    ASSERT_TRUE(NeedTouch(oldTouch, oldTouch + touchTimeout + TDuration::Seconds(1), touchTimeout));
}
