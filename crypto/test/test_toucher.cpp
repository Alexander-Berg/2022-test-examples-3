#include <crypta/cm/services/api/lib/toucher/toucher.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace NCrypta::NCm;
using namespace NCrypta::NCm::NApi;

namespace {
    const TId EXT_ID = TId("type", "value");
    const TDuration TTL_ZERO = TDuration::Zero();
    const TInstant TOUCH_TIME = TInstant::Seconds(1600000000);
    const TDuration TOUCH_TIMEOUT = TDuration::Seconds(5);

    TToucher GetToucher() {
        return TToucher(TOUCH_TIMEOUT);
    }
}

TEST(TToucher, Touch) {
    TMatch match;
    match.SetExtId(EXT_ID);

    const auto& toucher = GetToucher();

    toucher.Touch(match, TOUCH_TIME);

    ASSERT_EQ(TOUCH_TIME, match.GetTouch());
    ASSERT_EQ(TTL_ZERO, match.GetTtl());
}

TEST(TToucher, NeedTouch) {
    TMatch match;
    match.SetExtId(EXT_ID);

    const auto& toucher = GetToucher();

    toucher.Touch(match, TOUCH_TIME);

    const auto& touchTimeWithinTimeout = TOUCH_TIME + TOUCH_TIMEOUT;
    const auto& touchTimeBeyondTimeout = TOUCH_TIME + TOUCH_TIMEOUT + TDuration::Seconds(1);

    ASSERT_FALSE(toucher.NeedTouch(match.GetTouch(), touchTimeWithinTimeout));
    ASSERT_TRUE(toucher.NeedTouch(match.GetTouch(), touchTimeBeyondTimeout));
}
