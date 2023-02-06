#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct LOGOUTTest : CommandTestBase
{
    string commandName = "LOGOUT"s;

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

    auto serverByeBeginning()
    {
        return "* BYE IMAP4rev1 Server logging out"s;
    }

    auto logoutOkResponse()
    {
        return commandTag() + " OK LOGOUT Completed."s;
    }
};

TEST_F(LOGOUTTest, createOK)
{
    auto command = createCommand();
    ASSERT_TRUE(command != nullptr);
}

TEST_F(LOGOUTTest, finishedOkWithServerBye)
{
    createAndStartCommand();

    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], serverByeBeginning()))
        << session->outgoingData[0];
    ASSERT_TRUE(beginsWith(session->outgoingData[1], logoutOkResponse()))
        << session->outgoingData[1];
}

TEST_F(LOGOUTTest, closeSession)
{
    createAndStartCommand();
    ASSERT_TRUE(session->shutdownCalled);
}
