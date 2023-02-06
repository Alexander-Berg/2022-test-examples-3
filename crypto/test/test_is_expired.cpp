#include <crypta/cm/services/common/expiration/is_expired.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/vector.h>

using namespace NCrypta::NCm;

const TInstant NOW = TInstant::Seconds(1500000000);
const TDuration TTL = TDuration::Hours(1);

TEST(IsExpired, Touch) {
    TMatch match;

    match.SetTouch(NOW - TTL + TDuration::Seconds(1));
    match.SetTtl(TTL);
    ASSERT_FALSE(IsExpired(match, NOW));

    match.SetTouch(match.GetTouch() - TDuration::Seconds(1));
    ASSERT_TRUE(IsExpired(match, NOW));
}
