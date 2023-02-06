#include <gtest/gtest.h>

#include <mail/sendbernar/core/include/metrics.h>

using namespace testing;

namespace sendbernar {

TEST(MetricTest, shouldNotUpdateWithNonSendbernarOrMailSendErorr) {
    EXPECT_TRUE(RequestMertics::properErrorCategory(getSendbernarCategory()));
    EXPECT_TRUE(RequestMertics::properErrorCategory(getNwCategory()));
    EXPECT_TRUE(RequestMertics::properErrorCategory(getComposeCategory()));
    EXPECT_FALSE(RequestMertics::properErrorCategory(boost::asio::error::misc_category));
}

}
