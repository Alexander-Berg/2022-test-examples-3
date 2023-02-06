#include <tests/unit/logger_mock.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/log.hpp>

#include <sstream>

namespace {

using namespace testing;

using collie::logException;
using collie::tests::LoggerMock;

struct TestServerLogException : Test {
    StrictMock<LoggerMock> logger;
};

TEST_F(TestServerLogException, for_runtime_error_should_write_to_log_once) {
    const InSequence s;
    EXPECT_CALL(logger, applicable(logdog::error)).WillOnce(Return(true));
    EXPECT_CALL(logger, write(logdog::error, _)).WillOnce(Return());
    logException(logger, std::runtime_error(""));
}

TEST_F(TestServerLogException, for_nested_exception_should_write_to_log_for_each) {
    try {
        throw std::runtime_error("");
    } catch (const std::exception&) {
        try {
            std::throw_with_nested(std::runtime_error(""));
        } catch (const std::exception& e) {
            const InSequence s;
            EXPECT_CALL(logger, applicable(logdog::error)).WillOnce(Return(true));
            EXPECT_CALL(logger, write(logdog::error, _)).WillOnce(Return());
            EXPECT_CALL(logger, applicable(logdog::error)).WillOnce(Return(true));
            EXPECT_CALL(logger, write(logdog::error, _)).WillOnce(Return());
            logException(logger, e);
        }
    }
}

} // namespace
