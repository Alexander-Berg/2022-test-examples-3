#include <gtest/gtest.h>

#include <parser/message.h>

using namespace ymod_smtpserver;
using namespace testing;

struct DotStuffingTestBase: public Test {
    parser::DotStuffingParser parser;
    std::string::const_iterator dot;
};

struct DotStuffingTest: DotStuffingTestBase {
    const std::string buf = "abcde\r\n... three dots";
    std::string::const_iterator realDot = std::find(buf.begin(), buf.end(), '.');
};

struct DotStuffingTestWithFakeDots: DotStuffingTestBase {
    const std::string buf = "abcde\r... three dots \n.. two dots";
    std::string::const_iterator fakeDot = std::find(buf.begin(), buf.end(), '.');
    std::string::const_iterator realDot = buf.begin() + buf.find("\n.") + 1;
};

TEST_F(DotStuffingTest, ParseEmptyChunk) {
    EXPECT_FALSE(parser.parse(buf.begin(), buf.begin(), dot));
    EXPECT_EQ(dot, buf.begin());
}

TEST_F(DotStuffingTest, ParseOneChunk) {
    // all data in one chunk
    EXPECT_TRUE(parser.parse(buf.begin(), buf.end(), dot));
    EXPECT_EQ(dot, realDot);
}

TEST_F(DotStuffingTest, ParseTwoChunks) {
    // first chunk ends with "\n"
    EXPECT_FALSE(parser.parse(buf.begin(), realDot, dot));
    EXPECT_EQ(dot, realDot);

    // second chunk contains the real "^." marker
    EXPECT_TRUE(parser.parse(realDot, buf.end(), dot));
    EXPECT_EQ(dot, realDot);
}

TEST_F(DotStuffingTestWithFakeDots, ParseRangeWithFakeDots) {
    // first chunk ends after "\r."
    EXPECT_FALSE(parser.parse(buf.begin(), fakeDot + 1, dot));
    EXPECT_EQ(dot, fakeDot + 1);

    // second chunk contains the real "^." marker
    EXPECT_TRUE(parser.parse(fakeDot + 1, buf.end(), dot));
    EXPECT_EQ(dot, realDot);
}
