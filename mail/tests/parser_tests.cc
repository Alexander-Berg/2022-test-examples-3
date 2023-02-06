#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/webmail/tskv/parser.h>

using namespace ::testing;
using namespace ::webmail::tskv;

TEST(TskvParserTest, invokeTskvParserWithEmptyStringShouldNotMatch) {
    const std::string logLine = "";
    EXPECT_FALSE(parse(logLine));
}

TEST(TskvParserTest, invokeTskvParserWithIllformedStringShouldNotMatch) {
    const std::string logLine = "tskv_format=mail-hound-tskv-log\t139637111244544\tunixtime=1555923036";
    EXPECT_FALSE(parse(logLine));
}

TEST(TskvParserTest, invokeTskvParserWithWellformedStringShouldMatch) {
    const std::string logLine = "tskv\ttskv_format=mail-hound-tskv-log\tthread=139637111244544\tunixtime=1555923036\t"
                                "timestamp=2019-04-22T11:50:36.734430+0300\tlevel=warning\tuid=4002861327\t"
                                "request_id=071c2c5f65a67999fd84fef7dc209448\twhere_name=boost::optional<std::string> "
                                "hound::server::handlers::MacsHandler::getServiceDbUser(const "
                                "hound::server::Base::Request &) const\tmessage=unknown caller bar";
    const auto parsedOpt = parse(logLine);
    ASSERT_TRUE(parsedOpt);
    std::map<std::string, std::string> parsed = *parsedOpt;
    std::map<std::string, std::string> expected = {
            {"tskv_format", "mail-hound-tskv-log"}
            , {"thread", "139637111244544"}
            , {"unixtime", "1555923036"}
            , {"timestamp", "2019-04-22T11:50:36.734430+0300"}
            , {"level", "warning"}
            , {"uid", "4002861327"}
            , {"request_id", "071c2c5f65a67999fd84fef7dc209448"}
            , {"where_name", "boost::optional<std::string> hound::server::handlers::MacsHandler::"
                             "getServiceDbUser(const hound::server::Base::Request &) const"}
            , {"message", "unknown caller bar"}
    };

    EXPECT_THAT(parsed, ContainerEq(expected));
}