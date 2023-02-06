#include "command_test_base.h"
#include <yplatform/encoding/base64.h>

using namespace yimap;
using namespace yimap::backend;

struct AuthenticateTest : CommandTestBase
{
    string commandName = "AUTHENTICATE"s;
    string testUid = "1234"s;
    string testLogin = "test-email"s;
    string testOAuthToken = "AA=="s;
    string testPlainCredentials = yplatform::base64_encode_str("a\0b\0c"s);

    AuthenticateTest()
    {
        context->sessionInfo.trustedConnection = true;
    }

    auto createCommandWithTokenAuth()
    {
        return createCommand(commandName, "xoauth "s + testOAuthToken);
    }

    auto createCommandWithPlainAuth()
    {
        return createCommand(commandName, "plain"s);
    }

    auto createCommandRequiredContinuationData()
    {
        return createCommand(commandName, "xoauth"s);
    }

    auto createAndStartCommandWithTokenAuth()
    {
        auto command = createCommandWithTokenAuth();
        startCommand(command);
        return command->getFuture();
    }

    auto createAndStartCommandWithPlainAuth()
    {
        auto command = createCommandWithPlainAuth();
        startCommand(command);
        provideContinuationData(testPlainCredentials);
        provideContinuationData("\r\n");
        return command->getFuture();
    }

    auto createAndStartCommandWithoutData()
    {
        auto command = createCommandRequiredContinuationData();
        startCommand(command);
        return command->getFuture();
    }

    void completeAuthenticate()
    {
        auto first = createAndStartCommandWithTokenAuth();
        sendGoodAuthResponse();
        sendGoodSettingsResponse();
        session->outgoingData.clear();
    }

    void sendGoodAuthResponse()
    {
        sendAuthResponse(goodAuthResponse());
    }

    void sendGoodAuthResponseWithAppPasswordsOn()
    {
        auto response = goodAuthResponse();
        response.authByApplicationPassword = true;
        sendAuthResponse(response);
    }

    void sendGoodAuthResponseWithAppPasswordsOff()
    {
        auto response = goodAuthResponse();
        response.authByApplicationPassword = false;
        sendAuthResponse(response);
    }

    void sendBadAuthResponse()
    {
        sendAuthResponse(badAuthResponse());
    }

    void sendBadKarmaAuthResponse()
    {
        sendAuthResponse(badKarmaAuthResponse());
    }

    void sendAuthResponse(AuthResult response)
    {
        if (testAuthBackend->promises.empty())
        {
            throw std::runtime_error("no promises");
        }
        testAuthBackend->promises.back().set(response);
        runIO();
    }

    void simulateAuthException()
    {
        if (testAuthBackend->promises.empty())
        {
            throw std::runtime_error("no promises");
        }
        testAuthBackend->promises.back().set_exception(std::domain_error("fail"));
        runIO();
    }

    void provideContinuationData(const string& data)
    {
        if (!session->readActive())
        {
            throw std::runtime_error("no read callbacks");
        }
        session->fakeReadStream << data;
        session->readCallback(ErrorCode{});
        runIO();
    }

    void simulateDataReadFailed()
    {
        if (!session->readActive())
        {
            throw std::runtime_error("no read callbacks");
        }
        session->readCallback(boost::asio::error::shut_down);
        runIO();
    }

    AuthResult goodAuthResponse()
    {
        AuthResult ret;
        ret.loginFail = false;
        ret.uid = testUid;
        ret.login = testLogin;
        return ret;
    }

    AuthResult badAuthResponse()
    {
        AuthResult ret;
        ret.loginFail = true;
        ret.uid = testUid;
        ret.login = testLogin;
        return ret;
    }

    AuthResult badKarmaAuthResponse()
    {
        AuthResult ret;
        ret.loginFail = false;
        ret.karmaFail = true;
        ret.uid = testUid;
        ret.login = testLogin;
        return ret;
    }

    void sendGoodSettingsResponse()
    {
        sendSettingsResponse(goodSettings());
    }

    void sendGoodSettingsResponseWithPlainAuthOn()
    {
        auto settings = goodSettings();
        settings.enableAuthPlain = true;
        sendSettingsResponse(settings);
    }

    void sendGoodSettingsResponseWithPlainAuthOff()
    {
        auto settings = goodSettings();
        settings.enableAuthPlain = false;
        sendSettingsResponse(settings);
    }

    void sendSettingsResponse(UserSettings response)
    {
        if (testUserSettingsBackend->promises.empty())
        {
            throw std::runtime_error("no promises");
        }
        testUserSettingsBackend->promises.back().set(response);
        runIO();
    }

    UserSettings goodSettings()
    {
        UserSettings ret;
        ret.imapEnabled = true;
        ret.clientLogEnabled = false;
        ret.renameEnabled = false;
        ret.localizeImap = false;
        return ret;
    }

    void simulateLoadSettingsException()
    {
        if (testUserSettingsBackend->promises.empty())
        {
            throw std::runtime_error("no promises");
        }
        testUserSettingsBackend->promises.back().set_exception(std::domain_error("fail"));
        runIO();
    }

    auto capabilityResponseBeginning()
    {
        return "* CAPABILITY"s;
    }

    auto authenticateOkResponse()
    {
        return commandTag() + " OK AUTHENTICATE Completed."s;
    }

    auto authenticateNoAuthFailedResponseBeginning()
    {
        return commandTag() +
            " NO [AUTHENTICATIONFAILED] AUTHENTICATE invalid credentials or IMAP is disabled"s;
    }

    auto authenticateNoBadKarmaResponseBeginning()
    {
        return commandTag() + " NO [AUTHENTICATIONFAILED] AUTHENTICATE Ommm"s;
    }

    auto authenticateNoInternalError()
    {
        return commandTag() + " NO [UNAVAILABLE] AUTHENTICATE internal server error"s;
    }

    auto authenticateBadNotTrustedConnection()
    {
        return commandTag() +
            " BAD [PRIVACYREQUIRED] AUTHENTICATE Working without SSL/TLS encryption is not allowed"s;
    }

    auto authenticateBadDubleLogin()
    {
        return commandTag() + " BAD [CLIENTBUG] AUTHENTICATE wrong state for this command"s;
    }

    auto continuationPrompt()
    {
        return "+"s;
    }
};

TEST_F(AuthenticateTest, createOK)
{
    auto command = createCommandWithTokenAuth();
    ASSERT_TRUE(command != nullptr);
}

TEST_F(AuthenticateTest, execCallsAuthBackendOnce)
{
    createAndStartCommandWithTokenAuth();
    ASSERT_EQ(testAuthBackend->promises.size(), 1);
}

TEST_F(AuthenticateTest, notFinishedWithoutAuthBackendResponse)
{
    auto future = createAndStartCommandWithTokenAuth();
    ASSERT_FALSE(future->ready());
}

TEST_F(AuthenticateTest, notFinishedAfterGoodAuthResponse)
{
    auto future = createAndStartCommandWithTokenAuth();
    sendGoodAuthResponse();

    ASSERT_FALSE(future->ready());
}

TEST_F(AuthenticateTest, requestsSettingsAfterGoodAuthResponse)
{
    createAndStartCommandWithTokenAuth();
    sendGoodAuthResponse();

    ASSERT_EQ(testUserSettingsBackend->promises.size(), 1);
}

TEST_F(AuthenticateTest, finishedAfterGoodSettingsResponse)
{
    auto future = createAndStartCommandWithTokenAuth();
    sendGoodAuthResponse();
    sendGoodSettingsResponse();

    ASSERT_TRUE(future->ready());
}

TEST_F(AuthenticateTest, sendsCapabilityAndOKOnFinish)
{
    createAndStartCommandWithTokenAuth();
    sendGoodAuthResponse();
    sendGoodSettingsResponse();

    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], capabilityResponseBeginning()))
        << session->outgoingData[0];
    ASSERT_EQ(session->outgoingData[1], authenticateOkResponse());
}

TEST_F(AuthenticateTest, sendsCapabilityAndOKForEnabledPlainAuthAndAppPasswords)
{
    createAndStartCommandWithTokenAuth();
    sendGoodAuthResponseWithAppPasswordsOn();
    sendGoodSettingsResponseWithPlainAuthOn();

    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], capabilityResponseBeginning()))
        << session->outgoingData[0];
    ASSERT_EQ(session->outgoingData[1], authenticateOkResponse());
}

TEST_F(AuthenticateTest, sendsCapabilityAndOKForEnabledPlainAuthAndAppPasswordsDisabled)
{
    createAndStartCommandWithTokenAuth();
    sendGoodAuthResponseWithAppPasswordsOff();
    sendGoodSettingsResponseWithPlainAuthOn();

    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], capabilityResponseBeginning()))
        << session->outgoingData[0];
    ASSERT_EQ(session->outgoingData[1], authenticateOkResponse());
}

TEST_F(AuthenticateTest, sendsCapabilityAndOKForAppPasswords)
{
    createAndStartCommandWithTokenAuth();
    sendGoodAuthResponseWithAppPasswordsOn();
    sendGoodSettingsResponseWithPlainAuthOff();

    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], capabilityResponseBeginning()))
        << session->outgoingData[0];
    ASSERT_EQ(session->outgoingData[1], authenticateOkResponse());
}

TEST_F(AuthenticateTest, finishedAfterBadAuthResponse)
{
    auto future = createAndStartCommandWithTokenAuth();
    sendBadAuthResponse();

    ASSERT_TRUE(future->ready());
}

TEST_F(AuthenticateTest, sendsNoAfterBadAuthResponse)
{
    createAndStartCommandWithTokenAuth();
    sendBadAuthResponse();

    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], authenticateNoAuthFailedResponseBeginning()))
        << session->outgoingData[0];
}

TEST_F(AuthenticateTest, sendsNoIfPlainAuthDisabled)
{
    createAndStartCommandWithPlainAuth();
    sendGoodAuthResponseWithAppPasswordsOff();
    sendGoodSettingsResponseWithPlainAuthOff();

    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[1], authenticateNoAuthFailedResponseBeginning()))
        << session->dumpOutput();
}

TEST_F(AuthenticateTest, sendsNoAfterBadKarmaAuthResponse)
{
    createAndStartCommandWithTokenAuth();
    sendBadKarmaAuthResponse();

    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], authenticateNoBadKarmaResponseBeginning()))
        << session->outgoingData[0];
}

TEST_F(AuthenticateTest, finishedAfterAuthException)
{
    auto future = createAndStartCommandWithTokenAuth();
    simulateAuthException();

    ASSERT_TRUE(future->ready());
}

TEST_F(AuthenticateTest, sendsNoAfterAuthException)
{
    createAndStartCommandWithTokenAuth();
    simulateAuthException();

    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], authenticateNoInternalError()))
        << session->outgoingData[0];
}

TEST_F(AuthenticateTest, finishedAfterLoadSettinsException)
{
    auto future = createAndStartCommandWithTokenAuth();
    sendGoodAuthResponse();
    simulateLoadSettingsException();

    ASSERT_TRUE(future->ready());
}

TEST_F(AuthenticateTest, sendsNoAfterLoadSettinsException)
{
    createAndStartCommandWithTokenAuth();
    sendGoodAuthResponse();
    simulateLoadSettingsException();

    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], authenticateNoInternalError()))
        << session->outgoingData[0];
}

TEST_F(AuthenticateTest, earlyExitIfNonSecureConnectionDenied)
{
    context->sessionInfo.trustedConnection = false;
    auto future = createAndStartCommandWithTokenAuth();

    ASSERT_TRUE(future->ready());
}

TEST_F(AuthenticateTest, sendsNoIfNonSecureConnectionDenied)
{
    context->sessionInfo.trustedConnection = false;
    createAndStartCommandWithTokenAuth();

    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], authenticateBadNotTrustedConnection()))
        << session->outgoingData[0];
}

TEST_F(AuthenticateTest, authenticateTwiceReturnsBad)
{
    completeAuthenticate();
    createAndStartCommandWithTokenAuth();

    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], authenticateBadDubleLogin()))
        << session->outgoingData[0];
}

TEST_F(AuthenticateTest, notFinishedIfNeedContinuationData)
{
    auto future = createAndStartCommandWithoutData();
    ASSERT_FALSE(future->ready());
}

TEST_F(AuthenticateTest, requestsContinuationData)
{
    createAndStartCommandWithoutData();

    ASSERT_TRUE(session->readActive());
    ASSERT_TRUE(beginsWith(session->outgoingData[0], continuationPrompt()))
        << session->outgoingData[0];
}

TEST_F(AuthenticateTest, readContinuationDataIfNoEOL)
{
    createAndStartCommandWithoutData();
    provideContinuationData(testOAuthToken);

    ASSERT_TRUE(session->readActive());
}

TEST_F(AuthenticateTest, readContinuationDataUntilEOL)
{
    createAndStartCommandWithoutData();
    provideContinuationData(testOAuthToken);
    provideContinuationData("\r\n");

    ASSERT_TRUE(session->readActive());
}

TEST_F(AuthenticateTest, finishAfterContinuationDataReceived)
{
    auto future = createAndStartCommandWithoutData();
    provideContinuationData(testOAuthToken + "\r\n");
    sendGoodAuthResponse();
    sendGoodSettingsResponse();
    ASSERT_TRUE(future->ready());
}

TEST_F(AuthenticateTest, closesSessionIfContinuationReadFailed)
{
    createAndStartCommandWithoutData();
    simulateDataReadFailed();

    ASSERT_TRUE(session->shutdownCalled);
}

TEST_F(AuthenticateTest, finishedContinuationReadFailed)
{
    auto future = createAndStartCommandWithoutData();
    simulateDataReadFailed();

    ASSERT_TRUE(future->ready());
}

TEST_F(AuthenticateTest, noResponseIfContinuationReadFailed)
{
    createAndStartCommandWithoutData();
    simulateDataReadFailed();

    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], continuationPrompt()))
        << session->outgoingData[0];
}

// TODO
// meta backend errors
// xiva