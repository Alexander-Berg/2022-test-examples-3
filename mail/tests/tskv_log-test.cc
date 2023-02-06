#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <logdog/format/tskv.h>

namespace {

using namespace ::testing;
namespace log = ::logdog;
namespace tskv = ::logdog::tskv;

constexpr static auto fmt = tskv::make_formatter(BOOST_HANA_STRING("test-format"));

struct TskvLogTest : public Test {
};

TEST_F(TskvLogTest, log_errorWithMessage_returnsLevelErrorAndMessage) {
    EXPECT_EQ(
        fmt(std::make_tuple(log::level=log::error, log::message="message")),
        "tskv\ttskv_format=test-format\tlevel=error\tmessage=message");
}

TEST_F(TskvLogTest, log_warningWithMessage_returnsLevelWarningAndMessage) {
    EXPECT_EQ(
        fmt(std::make_tuple(log::level=log::warning, log::message="message")),
        "tskv\ttskv_format=test-format\tlevel=warning\tmessage=message");
}

TEST_F(TskvLogTest, log_noticeWithMessage_returnsLevelNoticeAndMessage) {
    EXPECT_EQ(
        fmt(std::make_tuple(log::level=log::notice, log::message="message")),
        "tskv\ttskv_format=test-format\tlevel=notice\tmessage=message");
}

TEST_F(TskvLogTest, log_noticeWithLiteralAttribute_returnsLevelNoticeAndMessage) {
    using namespace logdog::literals;
    EXPECT_EQ(
        fmt(std::make_tuple(log::level=log::notice, "message"_a="message")),
        "tskv\ttskv_format=test-format\tlevel=notice\tmessage=message");
}

TEST_F(TskvLogTest, log_debugWithMessage_returnsLevelDebugAndMessage) {
    EXPECT_EQ(
        fmt(std::make_tuple(log::level=log::debug, log::message="message")),
        "tskv\ttskv_format=test-format\tlevel=debug\tmessage=message");
}

TEST_F(TskvLogTest, log_errorWithErrorCode_returnsLevelErrorAndMessageAndErrorCode) {
    EXPECT_EQ(
        fmt(std::make_tuple(log::level=log::error, log::message="message",
            log::error_code=::mail_errors::error_code{})),
        "tskv\ttskv_format=test-format\tlevel=error\tmessage=message\t"
            "error_code.category=system\terror_code.value=0\terror_code.message=Success");
}

TEST_F(TskvLogTest, log_errorWithStdErrorCode_returnsLevelErrorAndMessageAndErrorCode) {
    EXPECT_EQ(
        fmt(std::make_tuple(log::level=log::error, log::message="message",
            log::error_code=::std::error_code{})),
        "tskv\ttskv_format=test-format\tlevel=error\tmessage=message\t"
            "error_code.category=system\terror_code.value=0\terror_code.message=Success");
}

TEST_F(TskvLogTest, log_errorWithBoostErrorCode_returnsLevelErrorAndMessageAndErrorCode) {
    EXPECT_EQ(
        fmt(std::make_tuple(log::level=log::error, log::message="message",
            log::error_code=::boost::system::error_code{})),
        "tskv\ttskv_format=test-format\tlevel=error\tmessage=message\t"
            "error_code.category=system\terror_code.value=0\terror_code.message=Success");
}

TEST_F(TskvLogTest, log_noticeWithStdException_returnsLevelNoticeExceptionTypeAndExceptionWhat) {
    const std::runtime_error e("what");
    EXPECT_EQ(
        fmt(std::make_tuple(log::level=log::notice, log::exception=e)),
        "tskv\ttskv_format=test-format\tlevel=notice\t"
            "exception.type=std::runtime_error\texception.what=what");
}

}
