#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/once.h>

namespace {

using namespace sharpei;

TEST(OnceActionTest, callTwice_actionCalledOnceFirstTime) {
    unsigned count = 0;
    const auto handler = [&count] () { ++count; };
    OnceAction<decltype(handler)> once(handler);
    once();
    EXPECT_EQ(1u, count);
    once();
    EXPECT_EQ(1u, count);
}

} // namespace
