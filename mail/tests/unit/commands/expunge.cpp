#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct ExpungeBaseTest : CommandTestBase
{
    string inboxName = "Inbox"s;
    string inboxFid = "1"s;

    ExpungeBaseTest()
    {
        context->sessionState.setAuthenticated();
        auto folders = testMetaBackend->folderList();
        context->foldersCache.setFolders(folders);
        selectFolder(inboxName, inboxFid);
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
};

struct ExpungeTest : ExpungeBaseTest
{
    string commandName = "EXPUNGE";

    auto createCommand()
    {
        return CommandTestBase::createCommand(commandName, "");
    }

    auto okResponse()
    {
        return commandTag() + " OK EXPUNGE Completed";
    }
};

struct UidExpungeTest : ExpungeBaseTest
{
    string commandName = "UID EXPUNGE";

    auto createCommand(std::vector<string> uids)
    {
        return CommandTestBase::createCommand(commandName, boost::algorithm::join(uids, " "));
    }

    void setStatusUpdateWithNewMessage()
    {
        auto inbox = testMetaBackend->getFolderInfo(DBFolderId(inboxName, inboxFid)).get();
        MessageVector newMessages;
        newMessages.push_back(MessageData{});
        auto data =
            std::make_shared<MailboxDiff>(inbox, MessageVector{}, MessageVector{}, newMessages);
        testMetaBackend->setStatusUpdateData(data);
    }

    auto okResponse()
    {
        return commandTag() + " OK UID EXPUNGE Completed";
    }

    auto statusUpdateResponse(std::size_t messagesCount)
    {
        return "* "s + std::to_string(messagesCount) + " EXISTS\r\n" + okResponse();
    }
};

TEST_F(ExpungeTest, create)
{
    auto command = createCommand();
    ASSERT_TRUE(command != nullptr);
}

TEST_F(UidExpungeTest, create)
{
    auto command = createCommand({ "1" });
    ASSERT_TRUE(command != nullptr);
}

TEST_F(ExpungeTest, noDelete)
{
    startCommand(createCommand());
    auto deletedMids = testMetaBackend->expungedMids();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], okResponse())) << session->dumpOutput();
    ASSERT_EQ(deletedMids.size(), 0);
}

TEST_F(UidExpungeTest, noDelete)
{
    startCommand(createCommand({ "1" }));
    auto deletedMids = testMetaBackend->expungedMids();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], okResponse())) << session->dumpOutput();
    ASSERT_EQ(deletedMids.size(), 0);
}

TEST_F(ExpungeTest, dontDeleteFromSharedFolder)
{
    markMessageToDelete(inboxFid);
    markFolderShared(inboxFid);
    startCommand(createCommand());
    auto deletedMids = testMetaBackend->expungedMids();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], okResponse())) << session->dumpOutput();
    ASSERT_EQ(deletedMids.size(), 0);
}

TEST_F(UidExpungeTest, dontDeleteFromSharedFolder)
{
    auto uid = markMessageToDelete(inboxFid);
    markFolderShared(inboxFid);
    startCommand(createCommand({ std::to_string(uid) }));
    auto deletedMids = testMetaBackend->expungedMids();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], okResponse())) << session->dumpOutput();
    ASSERT_EQ(deletedMids.size(), 0);
}

TEST_F(ExpungeTest, dontDeleteFromReadOnlyFolder)
{
    markMessageToDelete(inboxFid);
    selectFolderReadOnly(inboxName, inboxFid);
    startCommand(createCommand());
    auto deletedMids = testMetaBackend->expungedMids();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], okResponse())) << session->dumpOutput();
    ASSERT_EQ(deletedMids.size(), 0);
}

TEST_F(UidExpungeTest, dontDeleteFromReadOnlyFolder)
{
    auto uid = markMessageToDelete(inboxFid);
    selectFolderReadOnly(inboxName, inboxFid);
    startCommand(createCommand({ std::to_string(uid) }));
    auto deletedMids = testMetaBackend->expungedMids();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], okResponse())) << session->dumpOutput();
    ASSERT_EQ(deletedMids.size(), 0);
}

TEST_F(ExpungeTest, deleteOne)
{
    auto uid = markMessageToDelete(inboxFid);
    startCommand(createCommand());
    auto deletedMids = testMetaBackend->expungedMids();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], okResponse())) << session->dumpOutput();
    ASSERT_EQ(deletedMids.size(), 1);
}

TEST_F(UidExpungeTest, deleteOne)
{
    auto uid = markMessageToDelete(inboxFid);
    startCommand(createCommand({ std::to_string(uid) }));
    auto deletedMids = testMetaBackend->expungedMids();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], okResponse())) << session->dumpOutput();
    ASSERT_EQ(deletedMids.size(), 1);
}

TEST_F(ExpungeTest, deleteAll)
{
    auto uids = markMessagesToDelete(inboxFid);
    startCommand(createCommand());
    auto deletedMids = testMetaBackend->expungedMids();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], okResponse())) << session->dumpOutput();
    ASSERT_EQ(deletedMids.size(), uids.size());
}

TEST_F(UidExpungeTest, sendStatusUpdate)
{
    auto inbox = selectFolder(inboxName, inboxFid);
    setStatusUpdateWithNewMessage();
    startCommand(createCommand({ "1" }));
    ASSERT_TRUE(beginsWith(session->dumpOutput(), statusUpdateResponse(inbox.messageCount + 1)))
        << session->dumpOutput();
}
