#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct NoopTest : CommandTestBase
{
    string commandName = "NOOP"s;

    NoopTest()
    {
        context->sessionState.setAuthenticated();
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

    auto noopOkResponse()
    {
        return commandTag() + " OK NOOP Completed."s;
    }
};

TEST_F(NoopTest, createOK)
{
    auto command = createCommand();
    ASSERT_TRUE(command != nullptr);
}

TEST_F(NoopTest, finishedJustAfterStart)
{
    auto future = createAndStartCommand();
    ASSERT_TRUE(future->ready());
    ASSERT_FALSE(future->has_exception());
}

TEST_F(NoopTest, finishedOk)
{
    createAndStartCommand();

    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], noopOkResponse())) << session->outgoingData[0];
}

// TODO test diff output