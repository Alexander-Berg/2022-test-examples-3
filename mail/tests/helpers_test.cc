#include <gtest/gtest.h>

#include <mail/sendbernar/core/include/configuration.h>

using namespace testing;

namespace sendbernar {

std::time_t now() {
    return 10000000;
}

constexpr unsigned additional = 42;


TEST(HelpersTest, testDelayedMessageSendDate) {
    params::SendDelayed params;
    params.relative = false;
    params.send_time = 50000;

    EXPECT_EQ(delayedMessageSendDate(params, additional, &now), 50042);

    params.relative = true;
    EXPECT_EQ(delayedMessageSendDate(params, additional, &now), 10050042);
}

}
