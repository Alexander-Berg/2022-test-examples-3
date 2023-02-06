#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct EnableTest : CommandTestBase
{
    string commandName = "ENABLE"s;
    string testExtension = "test-extension"s;

    EnableTest()
    {
        context->sessionState.setAuthenticated();
    }

    auto createCommand()
    {
        return CommandTestBase::createCommand(commandName, testExtension);
    }

    auto createAndStartCommand()
    {
        auto command = createCommand();
        startCommand(command);
        return command->getFuture();
    }

    auto enabledMessage()
    {
        return "* ENABLED"s;
    }

    auto enableOkResponse()
    {
        return commandTag() + " OK ENABLE Completed."s;
    }
};

TEST_F(EnableTest, createOK)
{
    auto command = createCommand();
    ASSERT_TRUE(command != nullptr);
}

TEST_F(EnableTest, finishedWithEnableResponse)
{
    createAndStartCommand();

    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], enabledMessage())) << session->outgoingData[0];
    ASSERT_TRUE(beginsWith(session->outgoingData[1], enableOkResponse()))
        << session->outgoingData[1];
}
