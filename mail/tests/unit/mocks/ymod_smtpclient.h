#pragma once

#include <ymod_smtpclient/call.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

struct TYmodSmtpClientMock: public ymod_smtpclient::Call {
    using TTaskContextPtr = yplatform::task_context_ptr;
    using TCallback = std::function<void (ymod_smtpclient::error::Code, ymod_smtpclient::Response)>;

    MOCK_METHOD(void, asyncRun, (TTaskContextPtr ctx, ymod_smtpclient::Request req, TCallback cb), (override));
    MOCK_METHOD(void, asyncRun, (TTaskContextPtr ctx, ymod_smtpclient::Request req, ymod_smtpclient::Options opt, TCallback cb), (override));
    MOCK_METHOD(ymod_smtpclient::SmtpSessionPtr, createSmtpSession, (TTaskContextPtr ctx), (override));
    MOCK_METHOD(ymod_smtpclient::SmtpSessionPtr, createSmtpSession, (TTaskContextPtr ctx, const ymod_smtpclient::Timeouts& timeouts), (override));
};
