#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct UnselectTest : CommandTestBase
{
    string commandName = "UNSELECT"s;
    string inboxName = "Inbox"s;
    string inboxFid = "1"s;

    UnselectTest()
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

    auto selectInbox()
    {
        auto folder = testMetaBackend->getFolder(DBFolderId(inboxName, inboxFid)).get();
        context->sessionState.selectFolderReadOnly(folder);
        return folder->copyFolderInfo();
    }

    auto unselectOkResponse()
    {
        return commandTag() + " OK UNSELECT Completed."s;
    }

    auto unselectBadResponse()
    {
        return commandTag() + " BAD [CLIENTBUG] UNSELECT Wrong session state for command"s;
    }
};

TEST_F(UnselectTest, createOK)
{
    auto command = createCommand();
    ASSERT_TRUE(command != nullptr);
}

TEST_F(UnselectTest, finishedJustAfterStart)
{
    selectInbox();
    auto future = createAndStartCommand();
    ASSERT_TRUE(future->ready());
    ASSERT_FALSE(future->has_exception());
}

TEST_F(UnselectTest, finishedOkThenSelectedInbox)
{
    selectInbox();
    createAndStartCommand();

    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], unselectOkResponse()))
        << session->outgoingData[0];
}

TEST_F(UnselectTest, changesStateToAuthorized)
{
    selectInbox();
    createAndStartCommand();
    ASSERT_EQ(context->sessionState.state, ImapContext::SessionState::AUTH);
}

TEST_F(UnselectTest, finishedBadAtInitState)
{
    createAndStartCommand();

    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], unselectBadResponse()))
        << session->outgoingData[0];
}

TEST_F(UnselectTest, finishedBadThenAuthenticatedOnly)
{
    context->sessionState.setAuthenticated();
    createAndStartCommand();

    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], unselectBadResponse()))
        << session->outgoingData[0];
}
