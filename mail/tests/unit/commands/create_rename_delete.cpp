#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct FoldersTest : CommandTestBase
{
    string testFolderName = "test-folder"s;
    string testFolder2Name = "test-folder-2"s;

    FoldersTest()
    {
        context->sessionState.setAuthenticated();
    }

    auto runCreate(string folderName)
    {
        auto command = createCommand("CREATE"s, folderName);
        startCommand(command);
        return command->getFuture();
    }

    auto runRename(string src, string dst)
    {
        auto command = createCommand("RENAME"s, src + " " + dst);
        startCommand(command);
        return command->getFuture();
    }

    auto runDelete(string name)
    {
        auto command = createCommand("DELETE"s, name);
        startCommand(command);
        return command->getFuture();
    }

    auto createOkCompleted()
    {
        return commandTag() + " OK CREATE Completed";
    }

    auto createNoFolderAlreadyExists()
    {
        return commandTag() + " NO [CLIENTBUG] CREATE folder already exists";
    }

    auto createBadCantApplyToInbox()
    {
        return commandTag() + " BAD [CLIENTBUG] CREATE cannot apply to INBOX";
    }

    auto renameOkCompleted()
    {
        return commandTag() + " OK RENAME Completed";
    }

    auto renameNoCantApplyToInbox()
    {
        return commandTag() + " NO [CLIENTBUG] RENAME Cannot apply to INBOX";
    }

    auto renameNoSuchFolder()
    {
        return commandTag() + " NO [CLIENTBUG] RENAME Cannot rename folder \"" + testFolderName +
            "\" to \"" + testFolder2Name + "\": no such folder";
    }

    auto renameNoToInbox()
    {
        return commandTag() + " NO [CLIENTBUG] RENAME Cannot apply to INBOX";
    }

    auto deleteOkCompleted()
    {
        return commandTag() + " OK DELETE Completed";
    }

    auto deleteNoCantApplyToInbox()
    {
        return commandTag() + " NO [CLIENTBUG] DELETE cannot apply to INBOX";
    }
};

TEST_F(FoldersTest, createFolderOk)
{
    auto future = runCreate(testFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], createOkCompleted())) << session->dumpOutput();
}

TEST_F(FoldersTest, createdFolderExists)
{
    runCreate(testFolderName);
    ASSERT_TRUE(testMetaBackend->folderList()->hasFolder(testFolderName));
}

TEST_F(FoldersTest, doubleCreateFolderFails)
{
    runCreate(testFolderName);
    runCreate(testFolderName);
    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[1], createNoFolderAlreadyExists()))
        << session->dumpOutput();
}

TEST_F(FoldersTest, createInboxFailed)
{
    runCreate("inbox");
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], createBadCantApplyToInbox()))
        << session->dumpOutput();
}

TEST_F(FoldersTest, renameCreatedSuccess)
{
    runCreate(testFolderName);
    runRename(testFolderName, testFolder2Name);
    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[1], renameOkCompleted())) << session->dumpOutput();
}

TEST_F(FoldersTest, renamedExists)
{
    runCreate(testFolderName);
    runRename(testFolderName, testFolder2Name);
    ASSERT_TRUE(testMetaBackend->folderList()->hasFolder(testFolder2Name));
    ASSERT_FALSE(testMetaBackend->folderList()->hasFolder(testFolderName));
}

TEST_F(FoldersTest, bidirectionalRenameCreatedSuccess)
{
    runCreate(testFolderName);
    runRename(testFolderName, testFolder2Name);
    runRename(testFolder2Name, testFolderName);
    ASSERT_EQ(session->outgoingData.size(), 3);
    ASSERT_TRUE(beginsWith(session->outgoingData[2], renameOkCompleted())) << session->dumpOutput();
}

TEST_F(FoldersTest, renameUnexsitedFails)
{
    runRename(testFolderName, testFolder2Name);
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], renameNoSuchFolder()))
        << session->dumpOutput();
}

TEST_F(FoldersTest, renameInboxFails)
{
    runRename("inbox", testFolderName);
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], renameNoCantApplyToInbox()))
        << session->dumpOutput();
}

TEST_F(FoldersTest, renameMeaninglessSuccess)
{
    runCreate(testFolderName);
    runRename('|' + testFolderName, testFolderName);
    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[1], renameOkCompleted())) << session->dumpOutput();
}

TEST_F(FoldersTest, renameMeaninglessInboxSuccess)
{
    runRename("|inbox", "inbox");
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], renameOkCompleted())) << session->dumpOutput();
}

TEST_F(FoldersTest, renameCreatedToInboxFails)
{
    runCreate(testFolderName);
    runRename(testFolderName, "inbox");
    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[1], renameNoToInbox())) << session->dumpOutput();
}

TEST_F(FoldersTest, deleteCreatedSuccess)
{
    runCreate(testFolderName);
    runDelete(testFolderName);
    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[1], deleteOkCompleted())) << session->dumpOutput();
}

TEST_F(FoldersTest, deletedDoesntExist)
{
    runCreate(testFolderName);
    runDelete(testFolderName);
    ASSERT_FALSE(testMetaBackend->folderList()->hasFolder(testFolderName));
}

TEST_F(FoldersTest, deleteInboxFails)
{
    runDelete("inbox");
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], deleteNoCantApplyToInbox()))
        << session->dumpOutput();
}

// exec command after mailbox background update
// test child folders