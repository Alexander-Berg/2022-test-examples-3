#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct CloseTest : CommandTestBase
{
    string commandName = "CLOSE"s;
    string inboxName = "Inbox"s;
    string inboxFid = "1"s;

    CloseTest()
    {
        context->sessionState.setAuthenticated();
        auto folders = testMetaBackend->folderList();
        context->foldersCache.setFolders(folders);
        selectFolder(inboxName, inboxFid);
    }

    auto createCommand()
    {
        return CommandTestBase::createCommand(commandName, "");
    }

    unsigned int markMessageToDelete(const string& fid)
    {
        for (auto& [imapId, message] : testMetaBackend->getMailbox().messages[fid])
        {
            message.deleted = true;
            return message.uid;
        }
        throw std::runtime_error("no message to delete");
    }

    std::vector<unsigned int> markMessagesToDelete(const string& fid)
    {
        std::vector<unsigned int> ret;
        for (auto& [imapId, message] : testMetaBackend->getMailbox().messages[fid])
        {
            message.deleted = true;
            ret.emplace_back(message.uid);
        }
        return ret;
    }

    auto okResponse()
    {
        return commandTag() + " OK CLOSE Completed";
    }
};

TEST_F(CloseTest, create)
{
    auto command = createCommand();
    ASSERT_TRUE(command != nullptr);
}

TEST_F(CloseTest, responseOk)
{
    startCommand(createCommand());
    ASSERT_TRUE(beginsWith(session->outgoingData[0], okResponse())) << session->dumpOutput();
}

TEST_F(CloseTest, unselect)
{
    startCommand(createCommand());
    ASSERT_FALSE(context->sessionState.selected());
}

TEST_F(CloseTest, noDelete)
{
    startCommand(createCommand());
    auto deletedMids = testMetaBackend->expungedMids();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], okResponse())) << session->dumpOutput();
    ASSERT_EQ(deletedMids.size(), 0);
}

TEST_F(CloseTest, dontDeleteFromSharedFolder)
{
    markMessageToDelete(inboxFid);
    markFolderShared(inboxFid);
    startCommand(createCommand());
    auto deletedMids = testMetaBackend->expungedMids();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], okResponse())) << session->dumpOutput();
    ASSERT_EQ(deletedMids.size(), 0);
}

TEST_F(CloseTest, dontDeleteFromReadOnlyFolder)
{
    markMessageToDelete(inboxFid);
    selectFolderReadOnly(inboxName, inboxFid);
    startCommand(createCommand());
    auto deletedMids = testMetaBackend->expungedMids();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], okResponse())) << session->dumpOutput();
    ASSERT_EQ(deletedMids.size(), 0);
}

TEST_F(CloseTest, deleteOne)
{
    auto uid = markMessageToDelete(inboxFid);
    startCommand(createCommand());
    auto deletedMids = testMetaBackend->expungedMids();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], okResponse())) << session->dumpOutput();
    ASSERT_EQ(deletedMids.size(), 1);
}

TEST_F(CloseTest, deleteAll)
{
    auto uids = markMessagesToDelete(inboxFid);
    startCommand(createCommand());
    auto deletedMids = testMetaBackend->expungedMids();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], okResponse())) << session->dumpOutput();
    ASSERT_EQ(deletedMids.size(), uids.size());
}
