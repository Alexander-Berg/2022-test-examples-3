#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct LoginTest : CommandTestBase
{
    string commandName = "LOGIN"s;
    string testUid = "1234"s;
    string testLogin = "test-email"s;
    string testPassword = "test-password"s;

    LoginTest()
    {
        context->sessionInfo.trustedConnection = true;
    }

    auto createCommand()
    {
        return CommandTestBase::createCommand(commandName, testLogin + " " + testPassword);
    }

    auto createAndStartCommand()
    {
        auto command = createCommand();
        startCommand(command);
        return command->getFuture();
    }

    void completeLogin()
    {
        auto first = createAndStartCommand();
        sendGoodAuthResponse();
        sendGoodSettingsResponse();
        session->outgoingData.clear();
    }

    void sendGoodAuthResponse()
    {
        sendAuthResponse(goodAuthResponse(testUid, testLogin));
    }

    void sendGoodAuthResponseWithAppPasswordsOn()
    {
        auto response = goodAuthResponse(testUid, testLogin);
        response.authByApplicationPassword = true;
        sendAuthResponse(response);
    }

    void sendGoodAuthResponseWithAppPasswordsOff()
    {
        auto response = goodAuthResponse(testUid, testLogin);
        response.authByApplicationPassword = false;
        sendAuthResponse(response);
    }

    void sendBadAuthResponse()
    {
        sendAuthResponse(badAuthResponse(testUid, testLogin));
    }

    void sendBadKarmaAuthResponse()
    {
        sendAuthResponse(badKarmaAuthResponse(testUid, testLogin));
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

    UserSettings goodSettings()
    {
        UserSettings ret;
        ret.imapEnabled = true;
        ret.clientLogEnabled = false;
        ret.renameEnabled = false;
        ret.localizeImap = false;
        return ret;
    }

    auto capabilityResponseBeginning()
    {
        return "* CAPABILITY"s;
    }

    auto loginOkResponse()
    {
        return commandTag() + " OK LOGIN Completed."s;
    }

    auto loginNoAuthFailedResponseBeginning()
    {
        return commandTag() +
            " NO [AUTHENTICATIONFAILED] LOGIN invalid credentials or IMAP is disabled"s;
    }

    auto loginNoBadKarmaResponseBeginning()
    {
        return commandTag() + " NO [AUTHENTICATIONFAILED] LOGIN Ommm"s;
    }

    auto loginNoInternalError()
    {
        return commandTag() + " NO [UNAVAILABLE] LOGIN internal server error"s;
    }

    auto loginBadNotTrustedConnection()
    {
        return commandTag() +
            " BAD [PRIVACYREQUIRED] LOGIN Working without SSL/TLS encryption is not allowed"s;
    }

    auto loginBadDubleLogin()
    {
        return commandTag() + " BAD [CLIENTBUG] LOGIN wrong state for this command"s;
    }
};

TEST_F(LoginTest, createOK)
{
    auto command = createCommand();
    ASSERT_TRUE(command != nullptr);
}

TEST_F(LoginTest, execCallsAuthBackendOnce)
{
    createAndStartCommand();
    ASSERT_EQ(testAuthBackend->promises.size(), 1);
}

TEST_F(LoginTest, notFinishedWithoutAuthBackendResponse)
{
    auto future = createAndStartCommand();
    ASSERT_FALSE(future->ready());
}

TEST_F(LoginTest, notFinishedAfterGoodAuthResponse)
{
    auto future = createAndStartCommand();
    sendGoodAuthResponse();
    ASSERT_FALSE(future->ready());
}

TEST_F(LoginTest, requestsSettingsAfterGoodAuthResponse)
{
    auto future = createAndStartCommand();
    sendGoodAuthResponse();
    ASSERT_EQ(testUserSettingsBackend->promises.size(), 1);
}

TEST_F(LoginTest, finishedAfterGoodSettingsResponse)
{
    auto future = createAndStartCommand();
    sendGoodAuthResponse();
    sendGoodSettingsResponse();
    ASSERT_TRUE(future->ready());
}

TEST_F(LoginTest, sendsCapabilityAndOKOnFinish)
{
    auto future = createAndStartCommand();
    sendGoodAuthResponse();
    sendGoodSettingsResponse();
    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], capabilityResponseBeginning()))
        << session->outgoingData[0];
    ASSERT_EQ(session->outgoingData[1], loginOkResponse());
}

TEST_F(LoginTest, sendsCapabilityAndOKForEnabledPlainAuthAndAppPasswords)
{
    createAndStartCommand();
    sendGoodAuthResponseWithAppPasswordsOn();
    sendGoodSettingsResponseWithPlainAuthOn();

    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], capabilityResponseBeginning()))
        << session->outgoingData[0];
    ASSERT_EQ(session->outgoingData[1], loginOkResponse());
}

TEST_F(LoginTest, sendsCapabilityAndOKForEnabledPlainAuthAndAppPasswordsDisabled)
{
    createAndStartCommand();
    sendGoodAuthResponseWithAppPasswordsOff();
    sendGoodSettingsResponseWithPlainAuthOn();

    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], capabilityResponseBeginning()))
        << session->outgoingData[0];
    ASSERT_EQ(session->outgoingData[1], loginOkResponse());
}

TEST_F(LoginTest, sendsCapabilityAndOKForAppPasswords)
{
    createAndStartCommand();
    sendGoodAuthResponseWithAppPasswordsOn();
    sendGoodSettingsResponseWithPlainAuthOff();

    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], capabilityResponseBeginning()))
        << session->outgoingData[0];
    ASSERT_EQ(session->outgoingData[1], loginOkResponse());
}

TEST_F(LoginTest, finishedAfterBadAuthResponse)
{
    auto future = createAndStartCommand();
    sendBadAuthResponse();
    ASSERT_TRUE(future->ready());
}

TEST_F(LoginTest, sendsNoAfterBadAuthResponse)
{
    auto future = createAndStartCommand();
    sendBadAuthResponse();
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], loginNoAuthFailedResponseBeginning()))
        << session->outgoingData[0];
}

TEST_F(LoginTest, sendsNoIfPlainAuthDisabled)
{
    createAndStartCommand();
    sendGoodAuthResponseWithAppPasswordsOff();
    sendGoodSettingsResponseWithPlainAuthOff();

    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], loginNoAuthFailedResponseBeginning()))
        << session->outgoingData[0];
}

TEST_F(LoginTest, sendsNoAfterBadKarmaAuthResponse)
{
    auto future = createAndStartCommand();
    sendBadKarmaAuthResponse();
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], loginNoBadKarmaResponseBeginning()))
        << session->outgoingData[0];
}

TEST_F(LoginTest, finishedAfterAuthException)
{
    auto future = createAndStartCommand();
    simulateAuthException();
    ASSERT_TRUE(future->ready());
}

TEST_F(LoginTest, sendsNoAfterAuthException)
{
    auto future = createAndStartCommand();
    simulateAuthException();
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], loginNoInternalError()))
        << session->outgoingData[0];
}

TEST_F(LoginTest, finishedAfterLoadSettinsException)
{
    auto future = createAndStartCommand();
    sendGoodAuthResponse();
    simulateLoadSettingsException();
    ASSERT_TRUE(future->ready());
}

TEST_F(LoginTest, sendsNoAfterLoadSettinsException)
{
    auto future = createAndStartCommand();
    sendGoodAuthResponse();
    simulateLoadSettingsException();
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], loginNoInternalError()))
        << session->outgoingData[0];
}

TEST_F(LoginTest, earlyExitIfNonSecureConnectionDenied)
{
    context->sessionInfo.trustedConnection = false;
    auto future = createAndStartCommand();
    ASSERT_TRUE(future->ready());
}

TEST_F(LoginTest, sendsNoIfNonSecureConnectionDenied)
{
    context->sessionInfo.trustedConnection = false;
    auto future = createAndStartCommand();
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], loginBadNotTrustedConnection()))
        << session->outgoingData[0];
}

TEST_F(LoginTest, loginTwiceReturnsBad)
{
    completeLogin();

    createAndStartCommand();
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], loginBadDubleLogin()))
        << session->outgoingData[0];
}

// TODO
// meta backend errors
// xiva