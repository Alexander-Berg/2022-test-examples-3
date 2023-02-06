#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct IdleTest : CommandTestBase
{
    string commandName = "IDLE"s;
    string testLogin = "test-email"s;

    IdleTest()
    {
        settings->idleExtension = true;
        settings->idleUpdateTimeout = 0;
        selectFolder("Inbox", "1");
    }

    auto createCommand()
    {
        return CommandTestBase::createCommand(commandName, "");
    }

    auto createAndStartCommand()
    {
        auto command = createCommand();
        startCommand(command);
        return command->getFuture();
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

    void sendNotification()
    {
        if (testNotifications->subscriptions.empty())
        {
            throw std::runtime_error("no subscriptions");
        }
        auto subscription = testNotifications->subscriptions[0].lock();
        subscription->onEvent(boost::make_shared<ImapUnconditionalEvent>("test-event"));
        runIO();
    }

    void simulateIdleTimeout()
    {
        session->cancelRunningOperations();
        runIO();
    }

    auto continuationRequest()
    {
        return "+ idling"s;
    }

    auto doneResponse()
    {
        return "done\r\n";
    }

    auto idleBadSyntaxErrorResponse()
    {
        return commandTag() + " BAD IDLE Command syntax error"s;
    }

    auto idleBadIncorrectStateResponse()
    {
        return commandTag() + " BAD [CLIENTBUG] IDLE Wrong session state for command"s;
    }

    auto byeAutologoutResponse()
    {
        return "* BYE Autologout; idle for too long"s;
    }

    auto idleTerminatedResponse()
    {
        return commandTag() + " OK IDLE Terminated";
    }

    void runIO()
    {
        io.reset();
        io.run_for(std::chrono::milliseconds(10));
    }
};

TEST_F(IdleTest, create)
{
    auto command = createCommand();
    ASSERT_TRUE(command != nullptr);
}

TEST_F(IdleTest, sendsBadIfDisabled)
{
    settings->idleExtension = false;
    auto future = createAndStartCommand();
    ASSERT_TRUE(future->ready());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], idleBadSyntaxErrorResponse()))
        << session->dumpOutput();
}

TEST_F(IdleTest, sendsBadInInitialState)
{
    context->sessionState = {};
    auto future = createAndStartCommand();
    ASSERT_TRUE(future->ready());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], idleBadIncorrectStateResponse()))
        << session->dumpOutput();
}

TEST_F(IdleTest, notFinishedAfterStart)
{
    auto future = createAndStartCommand();
    ASSERT_FALSE(future->ready());
}

TEST_F(IdleTest, subscribesAtStart)
{
    createAndStartCommand();
    ASSERT_EQ(testNotifications->subscriptions.size(), 1);
}

TEST_F(IdleTest, sendsContinuationRequest)
{
    createAndStartCommand();
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_EQ(session->outgoingData[0], continuationRequest()) << session->dumpOutput();
}

TEST_F(IdleTest, setsIdleTimeouts)
{
    createAndStartCommand();
    ASSERT_EQ(session->timeouts, "idle"s);
}

TEST_F(IdleTest, waitsForContinuationData)
{
    createAndStartCommand();
    ASSERT_TRUE(session->readActive());
}

TEST_F(IdleTest, logoutOnIdleTimeout)
{
    createAndStartCommand();
    simulateIdleTimeout();
    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[1], byeAutologoutResponse()))
        << session->dumpOutput();
}

TEST_F(IdleTest, finishIfDoneReceived)
{
    auto future = createAndStartCommand();
    provideContinuationData(doneResponse());
    ASSERT_TRUE(future->ready());
}

TEST_F(IdleTest, sendsOKIfDoneReceived)
{
    auto future = createAndStartCommand();
    provideContinuationData(doneResponse());
    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[1], idleTerminatedResponse()))
        << session->dumpOutput();
}

// TODO test notifications