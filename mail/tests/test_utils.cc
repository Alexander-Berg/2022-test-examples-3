#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mimeparser/rfc2822date.h>

namespace {

using namespace testing;

TEST(TestUtils, for_rfc2822_date_from_meta_header_mail_should_return_unix_time) {
    rfc2822::rfc2822date dt("Thu, 05 Feb 2019 11:11:11 +0000");
    EXPECT_EQ(dt.unixtime(), 1549365071);
}

TEST(TestUtils, for_rfc2822_date_from_meta_header_mail_should_return_time_zone_offset) {
    rfc2822::rfc2822date dtFirst("Thu, 05 Feb 2019 11:11:11 +0350");
    EXPECT_EQ(dtFirst.offset(), 13800);

    rfc2822::rfc2822date dtSecond("Thu, 05 Feb 2019 11:11:11 -0100");
    EXPECT_EQ(dtSecond.offset(), -3600);
}

}
