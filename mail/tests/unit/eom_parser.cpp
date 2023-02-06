#include <gtest/gtest.h>

#include <parser/message.h>

using namespace ymod_smtpserver;
using namespace testing;

struct EOMTestBase: public Test {
    parser::EOMParser parser;
    std::string::const_iterator eomBeg;
    std::string::const_iterator eomEnd;
};

struct EOMTest: EOMTestBase {
    const std::string buf = "abcde\r\n.\r\nQUIT";
    std::string::const_iterator dot = std::find(buf.begin(), buf.end(), '.');
};

struct TestWithFakeEOM: EOMTestBase {
    const std::string buf = "some data ... [\n.\r] eom \r\n.\nEND";
    std::string::const_iterator dot = buf.begin() + buf.find(".\n");
    std::string::const_iterator fakeDot = buf.begin() + buf.find(".\r");
};

struct DotstuffedEOMTest: EOMTestBase {
    const std::string buf = "yandex\r\n..ru";
    std::string::const_iterator dot = std::find(buf.begin(), buf.end(), '.');
};


TEST_F(EOMTest, ParseEmptyChunk) {
    EXPECT_FALSE(parser.parse(buf.begin(), buf.begin(), eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, buf.begin());
    EXPECT_EQ(eomEnd, buf.begin());
}

TEST_F(EOMTest, ParseOneChunk) {
    // all data in one chunk
    EXPECT_TRUE(parser.parse(buf.begin(), buf.end(), eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, dot);
    EXPECT_EQ(eomEnd, dot + 3);
}

TEST_F(EOMTest, ParseTwoChunks_1) {
    // first chunk ends with "\r\n."
    EXPECT_FALSE(parser.parse(buf.begin(), dot + 1, eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, dot);
    EXPECT_EQ(eomEnd, dot + 1);

    EXPECT_TRUE(parser.parse(dot + 1, buf.end(), eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, dot);
    EXPECT_EQ(eomEnd, dot + 3);
}

TEST_F(EOMTest, ParseTwoChunks_2) {
    // first chunk ends with "\r\n.\r"
    EXPECT_FALSE(parser.parse(buf.begin(), dot + 2, eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, dot);
    EXPECT_EQ(eomEnd, dot + 2);

    EXPECT_TRUE(parser.parse(dot + 2, buf.end(), eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, dot);
    EXPECT_EQ(eomEnd, dot + 3);
}

TEST_F(EOMTest, ParseOneEOMChunk) {
    // first chunk ends before ".\r\n"
    EXPECT_FALSE(parser.parse(buf.begin(), dot, eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, dot);
    EXPECT_EQ(eomEnd, dot);

    EXPECT_TRUE(parser.parse(dot, buf.end(), eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, dot);
    EXPECT_EQ(eomEnd, dot + 3);
}

TEST_F(EOMTest, ParseTwoEOMChunks) {
    // first chunk ends before ".\r\n"
    EXPECT_FALSE(parser.parse(buf.begin(), dot, eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, dot);
    EXPECT_EQ(eomEnd, dot);

    // second chunk is ".\r"
    EXPECT_FALSE(parser.parse(dot, dot + 2, eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, dot);
    EXPECT_EQ(eomEnd, dot + 2);

    // third chunk is "\n" + anything what's left.
    EXPECT_TRUE(parser.parse(dot + 2, buf.end(), eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, dot);
    EXPECT_EQ(eomEnd, dot + 3);
}

TEST_F(TestWithFakeEOM, ParseRangeWithFakeEOMs_1) {
    // first chunk ends after ".\r"
    EXPECT_FALSE(parser.parse(buf.begin(), fakeDot + 2, eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, fakeDot);
    EXPECT_EQ(eomEnd, fakeDot + 2);

    // second chunk contains the real EOM marker.
    EXPECT_TRUE(parser.parse(fakeDot + 2, buf.end(), eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, dot);
    EXPECT_EQ(eomEnd, dot + 2);
}

TEST_F(TestWithFakeEOM, ParseRangeWithFakeEOMs_2) {
    // first chunk ends first "."
    EXPECT_FALSE(parser.parse(buf.begin(), fakeDot + 1, eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, fakeDot);
    EXPECT_EQ(eomEnd, fakeDot + 1);

    // second chunk contains the real EOM marker.
    EXPECT_TRUE(parser.parse(fakeDot + 1, buf.end(), eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, dot);
    EXPECT_EQ(eomEnd, dot + 2);
}

TEST_F(DotstuffedEOMTest, ParseTwoChunks) {
    // first chunk ends after "\r\n"
    EXPECT_FALSE(parser.parse(buf.begin(), dot, eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, dot);
    EXPECT_EQ(eomEnd, dot);

    // third chunk is the stuffed line
    EXPECT_FALSE(parser.parse(dot, buf.end(), eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, buf.end());
    EXPECT_EQ(eomEnd, buf.end());
}

TEST_F(DotstuffedEOMTest, ParseThreeChunks) {
    // first chunk ends after "\r\n"
    EXPECT_FALSE(parser.parse(buf.begin(), dot, eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, dot);
    EXPECT_EQ(eomEnd, dot);

    // second chunk is just the first dot
    EXPECT_FALSE(parser.parse(dot, dot + 1, eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, dot);
    EXPECT_EQ(eomEnd, dot + 1);

    // third chunk is the stuffed line
    EXPECT_FALSE(parser.parse(dot + 1, buf.end(), eomBeg, eomEnd));
    EXPECT_EQ(eomBeg, buf.end());
    EXPECT_EQ(eomEnd, buf.end());
}
