#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <stdexcept>
#include <boost/lexical_cast.hpp>

#include <mail_getter/AttachShieldCrypto.h>

namespace {

using namespace testing;
using namespace mail_getter::attach_sid;

struct ParseSidTest: public Test {
    std::time_t ts;
    std::string mid;
    std::string hid;
};

TEST_F(ParseSidTest, parseSid_withAllFields_ReturnsTsMidHid) {
    std::string decryptedSid = "111:101.10001.100000001:1.5";
    parseSid(decryptedSid, ts, mid, hid);
    ASSERT_EQ(ts, 111);
    ASSERT_EQ(mid, "101.10001.100000001");
    ASSERT_EQ(hid, "1.5");
}

TEST_F(ParseSidTest, parseSid_withMidWithColon_ReturnsTsMidHid) {
    std::string decryptedSid = "111:125.doctmb:101.10001.100000001:1.5";
    parseSid(decryptedSid, ts, mid, hid);
    ASSERT_EQ(ts, 111);
    ASSERT_EQ(mid, "125.doctmb:101.10001.100000001");
    ASSERT_EQ(hid, "1.5");
}

TEST_F(ParseSidTest, parseSid_withAbsentField_ThrowsException) {
    std::string decryptedSid = "111:101.10001.100000001.1.5";
    ASSERT_THROW(parseSid(decryptedSid, ts, mid, hid), InvalidSid);
}

TEST_F(ParseSidTest, parseSid_withBadTsField_ThrowsException) {
    std::string decryptedSid = "111F:101.10001.100000001:1.5";
    ASSERT_THROW(parseSid(decryptedSid, ts, mid, hid), InvalidSid);
}

}
