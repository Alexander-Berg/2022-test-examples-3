#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/service_control/service_control.h>
#include <src/service_control/command_source_impl.h>
#include "log_mock.h"
#include <boost/range/adaptors.hpp>

using namespace testing;
using namespace doberman::service_control;
using namespace doberman::testing;
using namespace boost::asio;

struct CmdSourceMock {
    MOCK_METHOD(OptCommandType, getCommand, (yield_context), ());
    MOCK_METHOD(void, sendOkResponse, (yield_context), ());
    MOCK_METHOD(void, sendErrorResponse, (yield_context, const std::string&), ());
    MOCK_METHOD(void, sendResponse, (yield_context, const std::string&), ());
};

struct LogSinkMock {
    MOCK_METHOD(void, reopen, (), ());
};

struct ServiceControlTest : Test {
    LogSinkMock logSink;
    NiceMock<LogMock> logMock;
    CmdSourceMock cmdSource;
    io_service service;
    RunStatus runStatus;

    void run() {
        EXPECT_CALL(cmdSource, getCommand(_)).WillOnce(Return(CommandType::stop));
        EXPECT_CALL(cmdSource, sendOkResponse(_));

        auto log = doberman::make_log(logdog::none, &logMock);
        auto serviceControl = makeServiceControl(service, log, logSink, cmdSource, runStatus);
        boost::asio::spawn(service, [&](auto yield) {
            serviceControl.run(yield);
        });
        service.run();
    }
};

TEST_F(ServiceControlTest, stoppingServiceTest) {
    InSequence s;
    run();
    EXPECT_FALSE(runStatus);
}

TEST_F(ServiceControlTest, logRotationTest) {
    InSequence s;
    EXPECT_CALL(cmdSource, getCommand(_)).WillOnce(Return(CommandType::reopenLog));
    EXPECT_CALL(logSink, reopen()).Times(Exactly(1));
    EXPECT_CALL(cmdSource, sendOkResponse(_));
    run();
}

TEST_F(ServiceControlTest, logRotationFailTest) {
    InSequence s;
    EXPECT_CALL(cmdSource, getCommand(_)).WillOnce(Return(CommandType::reopenLog));
    EXPECT_CALL(logSink, reopen()).WillOnce(Throw(std::runtime_error("can't rotate")));
    EXPECT_CALL(cmdSource, sendErrorResponse(_, _));
    run();
}

TEST_F(ServiceControlTest, handleInvalidCommandTest) {
    InSequence s;
    EXPECT_CALL(cmdSource, getCommand(_)).WillOnce(Return(CommandType::invalid));
    EXPECT_CALL(cmdSource, sendErrorResponse(_, _));
    run();
}
