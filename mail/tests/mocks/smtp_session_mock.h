#pragma once

#include <gmock/gmock.h>

#include <ymod_smtpclient/smtp_session.h>

struct SmtpSessionMock : public ymod_smtpclient::SmtpSession {

    template <typename T>
    using Optional = ymod_smtpclient::Optional<T>;
    using ServerExtensions = ymod_smtpclient::ServerExtensions;
    using SmtpPoint = ymod_smtpclient::SmtpPoint;
    using AuthData = ymod_smtpclient::AuthData;
    using MailFrom = ymod_smtpclient::MailFrom;
    using RcptTo = ymod_smtpclient::RcptTo;

    MOCK_METHOD(void, cancel, (), (override));
    MOCK_METHOD(void, close, (), (override));
    MOCK_METHOD(void, shutdown, (bool gracefull), (override));
    MOCK_METHOD(bool, isOpen, (), (const, override));
    MOCK_METHOD(bool, isEncrypted, (), (const, override));
    MOCK_METHOD(ServerExtensions, getServerExtensions, (), (const, override));
    MOCK_METHOD(void, asyncConnect, (std::string host, uint16_t port, Optional<bool> useSsl, Handler handler), (override));
    MOCK_METHOD(void, asyncGreeting, (SmtpHandler handler), (override));
    MOCK_METHOD(void, asyncHelo, (SmtpPoint::Proto proto, SmtpHandler handler), (override));
    MOCK_METHOD(void, asyncHelo, (SmtpPoint::Proto proto, const std::string& hostname, SmtpHandler handler), (override));
    MOCK_METHOD(void, asyncAuth, (const AuthData& authData, SmtpHandler handler), (override));
    MOCK_METHOD(void, asyncStartTls, (SmtpHandler handler), (override));
    MOCK_METHOD(void, asyncMailFrom, (const MailFrom& mailFrom, SmtpHandler handler), (override));
    MOCK_METHOD(void, asyncRcptTo, (const RcptTo& rcptTo, bool enableDsn, SmtpHandler handler), (override));
    MOCK_METHOD(void, asyncDataStart, (SmtpHandler handler), (override));
    MOCK_METHOD(void, asyncRset, (SmtpHandler handler), (override));
    MOCK_METHOD(void, asyncQuit, (SmtpHandler handler), (override));
    MOCK_METHOD(void, asyncWriteMessage, (const std::string& msg, bool enableDotStuffing, SmtpHandler handler), (override));
};
