#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/unistat/cpp/include/signal_record.h>

using namespace ::testing;
using namespace ::unistat;

struct SignalRecordTest : public Test {
};

TEST_F(SignalRecordTest, invokeIdentityParserWithEmptyStringShouldNotMatch) {
    const std::string logLine = "";
    EXPECT_TRUE(IdentityParser::parse(logLine));
}

TEST_F(SignalRecordTest, invokeIdentityParserWithNonEmptyStringShouldMatch) {
    const std::string logLine = "abacaba";
    const auto parsedOpt = IdentityParser::parse(logLine);
    ASSERT_TRUE(parsedOpt);
    EXPECT_EQ(*parsedOpt, logLine);
}
