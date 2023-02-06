#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct CopyMoveTest
    : public CommandTestBase
    , public ::testing::WithParamInterface<string>
{
    string allSequence = "1:*"s;
    string emptySequence = "100:101"s;
    string inboxFolderFid = "1"s;
    string inboxFolderName = "Inbox"s;
    string testFolderFid = "3"s;
    string testFolderName = "Zombie"s;
    string badEncodingFolderName = "папка"s;
    string nonExistingFolderName = "no-such-folder"s;

    CopyMoveTest()
    {
        selectFolder(inboxFolderName, inboxFolderFid);
    }

    auto commandName()
    {
        return GetParam();
    }

    auto runTestCommand(const string& messageSequence, const string& folderName)
    {
        return createAndStartCommand(commandName(), messageSequence + " " + folderName);
    }

    auto runCopy(const string& messageSequence, const string& folderName)
    {
        return createAndStartCommand("COPY"s, messageSequence + " " + folderName);
    }

    auto runUidCopy(const string& messageSequence, const string& folderName)
    {
        return createAndStartCommand("UID COPY"s, messageSequence + " " + folderName);
    }

    auto runMove(const string& messageSequence, const string& folderName)
    {
        return createAndStartCommand("MOVE"s, messageSequence + " " + folderName);
    }

    auto runUidMove(const string& messageSequence, const string& folderName)
    {
        return createAndStartCommand("UID MOVE"s, messageSequence + " " + folderName);
    }

    auto makeCopyUidResponse(const FolderInfo& srcFolder, const FolderInfo& dstFolder)
    {
        auto uidvalidity = std::to_string(srcFolder.uidValidity);
        auto srcRange = uidSequence(srcFolder).to_string();
        auto dstRange = std::to_string(dstFolder.uidNext) + ":"s +
            std::to_string(dstFolder.uidNext + srcFolder.messageCount - 1);
        return "[COPYUID " + uidvalidity + " " + srcRange + " " + dstRange + "]";
    }

    UidSequence uidSequence(const FolderInfo& folderInfo)
    {
        auto folder = testMetaBackend->getFolder(DBFolderId(folderInfo.name, folderInfo.fid)).get();
        auto folderRef = FolderRef(folder, true);
        auto messages =
            testMetaBackend->loadMessages(folderRef, folderRef.fullUidRange(), false).get();
        return messages->toUidSequence();
    }

    auto makeExistsUntaggedResponse(const FolderInfo& folder)
    {
        std::vector<string> res;
        res.emplace_back("* " + std::to_string(folder.messageCount) + " EXISTS"s);
        return res;
    }

    auto makeExpungeUntaggedResponse(const FolderInfo& folder)
    {
        std::vector<string> res;
        for (int i = 0; i < folder.messageCount; ++i)
        {
            res.emplace_back("* 1 EXPUNGE"s);
        }
        return res;
    }

    auto makeUntaggedMoveResponse(const FolderInfo& srcFolder, const FolderInfo& dstFolder)
    {
        std::vector<string> res;
        res.emplace_back("* OK "s + makeCopyUidResponse(srcFolder, dstFolder));
        return res;
    }

    auto makeTaggedCopyResponse(const FolderInfo& srcFolder, const FolderInfo& dstFolder)
    {
        return commandTag() + " OK "s + makeCopyUidResponse(srcFolder, dstFolder) +
            " COPY Completed."s;
    }

    auto emptyTaggedMoveResponse()
    {
        return commandTag() + " OK MOVE Completed."s;
    }

    auto makeFullMoveResponse(const FolderInfo& srcFolder, const FolderInfo& dstFolder)
    {
        std::vector<string> res = makeUntaggedMoveResponse(srcFolder, dstFolder);
        auto expunge = makeExpungeUntaggedResponse(srcFolder);
        res.insert(res.end(), expunge.begin(), expunge.end());
        res.push_back(emptyTaggedMoveResponse());
        return res;
    }

    auto makeFullCopyResponse(const FolderInfo& srcFolder, const FolderInfo& dstFolder)
    {
        std::vector<string> res = makeExistsUntaggedResponse(dstFolder);
        res.push_back(makeTaggedCopyResponse(srcFolder, dstFolder));
        return res;
    }

    auto readOnlyMoveResponse()
    {
        return commandTag() + " NO [CLIENTBUG] MOVE Can not move from read-only folder"s;
    }

    auto copyNoMessagesResponse()
    {
        return commandTag() + " NO [CLIENTBUG] COPY Failed (no messages)"s;
    }

    auto uidCopyNoMessagesResponse()
    {
        return commandTag() + " OK [CLIENTBUG] UID COPY Completed (no messages)"s;
    }

    auto moveNoMessagesResponse()
    {
        return commandTag() + " NO [CLIENTBUG] MOVE Failed (no messages)"s;
    }

    auto uidMoveNoMessagesResponse()
    {
        return commandTag() + " OK [CLIENTBUG] UID MOVE Completed (no messages)"s;
    }

    auto wrongSessionStateResponse()
    {
        return commandTag() + " BAD [CLIENTBUG] "s + commandName() +
            " Wrong session state for command"s;
    }

    auto folderEncodingErrorResponse()
    {
        return commandTag() + " BAD [CLIENTBUG] "s + commandName() + " Folder encoding error"s;
    }

    auto noSuchFolderResponse()
    {
        return commandTag() + " NO [TRYCREATE] "s + commandName() + " No such folder"s;
    }
};

TEST_F(CopyMoveTest, copyCompletes)
{
    auto future = runCopy(allSequence, inboxFolderName);
    ASSERT_TRUE(future->ready());
}

TEST_F(CopyMoveTest, copySendsCorrectDatatoBackend)
{
    auto srcFolderInfo = getFolderInfo(inboxFolderName, inboxFolderFid);
    auto dstFolderInfo = srcFolderInfo;

    runCopy(allSequence, dstFolderInfo.name);

    auto copyRequests = testMetaBackend->getCopyRequests();
    ASSERT_EQ(copyRequests.size(), 1);

    auto [uidMap, requestedSrcFolderInfo, requestedDstFolderInfo] = copyRequests.front();
    ASSERT_EQ(srcFolderInfo, requestedSrcFolderInfo);
    ASSERT_EQ(dstFolderInfo, requestedDstFolderInfo);
    ASSERT_EQ(uidMap.size(), srcFolderInfo.messageCount);
}

TEST_F(CopyMoveTest, copySendCorrectResponse)
{
    auto srcFolderInfo = getFolderInfo(inboxFolderName, inboxFolderFid);
    auto dstFolderInfo = srcFolderInfo;

    runCopy(allSequence, dstFolderInfo.name);

    dstFolderInfo.messageCount += srcFolderInfo.messageCount;
    auto correctResponseData = makeFullCopyResponse(srcFolderInfo, dstFolderInfo);
    ASSERT_THAT(session->outgoingData, testing::UnorderedElementsAreArray(correctResponseData));
}

TEST_F(CopyMoveTest, moveCompletes)
{
    auto future = runMove(allSequence, testFolderName);
    ASSERT_TRUE(future->ready());
}

TEST_F(CopyMoveTest, moveSendsCorrectDatatoBackend)
{
    auto srcFolderInfo = getFolderInfo(inboxFolderName, inboxFolderFid);
    auto dstFolderInfo = getFolderInfo(testFolderName, testFolderFid);

    runMove(allSequence, testFolderName);

    auto moveRequests = testMetaBackend->getMoveRequests();
    ASSERT_EQ(moveRequests.size(), 1);

    auto [uidMap, requestedSrcFolderInfo, requestedDstFolderInfo] = moveRequests.front();
    ASSERT_EQ(srcFolderInfo, requestedSrcFolderInfo);
    ASSERT_EQ(dstFolderInfo, requestedDstFolderInfo);
    ASSERT_EQ(uidMap.size(), srcFolderInfo.messageCount);
}

TEST_F(CopyMoveTest, moveSendCorrectResponse)
{
    auto srcFolderInfo = getFolderInfo(inboxFolderName, inboxFolderFid);
    auto dstFolderInfo = getFolderInfo(testFolderName, testFolderFid);

    runMove(allSequence, testFolderName);

    auto correctResponseData = makeFullMoveResponse(srcFolderInfo, dstFolderInfo);
    ASSERT_THAT(session->outgoingData, testing::UnorderedElementsAreArray(correctResponseData));
}

TEST_F(CopyMoveTest, moveToItself)
{
    auto future = runMove(allSequence, inboxFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_TRUE(testMetaBackend->allRequestsEmpty());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_THAT(session->outgoingData[0], testing::StartsWith(emptyTaggedMoveResponse()));
}

TEST_F(CopyMoveTest, readOnlyMove)
{
    selectFolderReadOnly(inboxFolderName, inboxFolderFid);

    auto future = runMove(allSequence, testFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_TRUE(testMetaBackend->allRequestsEmpty());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_THAT(session->outgoingData[0], testing::StartsWith(readOnlyMoveResponse()));
}

TEST_P(CopyMoveTest, nonSelectedFolder)
{
    context->sessionState.unselect();

    auto future = runTestCommand(allSequence, testFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_TRUE(testMetaBackend->allRequestsEmpty());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_THAT(session->outgoingData[0], testing::StartsWith(wrongSessionStateResponse()));
}

TEST_P(CopyMoveTest, badEncoding)
{
    auto future = runTestCommand(allSequence, badEncodingFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_TRUE(testMetaBackend->allRequestsEmpty());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_THAT(session->outgoingData[0], testing::StartsWith(folderEncodingErrorResponse()));
}

TEST_P(CopyMoveTest, nonExistingFolder)
{
    auto future = runTestCommand(allSequence, nonExistingFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_TRUE(testMetaBackend->allRequestsEmpty());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_THAT(session->outgoingData[0], testing::StartsWith(noSuchFolderResponse()));
}

TEST_F(CopyMoveTest, copyEmptyMessages)
{
    auto future = runCopy(emptySequence, testFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_TRUE(testMetaBackend->allRequestsEmpty());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_THAT(session->outgoingData[0], testing::StartsWith(copyNoMessagesResponse()));
}

TEST_F(CopyMoveTest, uidCopyEmptyMessages)
{
    auto future = runUidCopy(emptySequence, testFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_TRUE(testMetaBackend->allRequestsEmpty());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_THAT(session->outgoingData[0], testing::StartsWith(uidCopyNoMessagesResponse()));
}

TEST_F(CopyMoveTest, moveEmptyMessages)
{
    auto future = runMove(emptySequence, testFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_TRUE(testMetaBackend->allRequestsEmpty());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_THAT(session->outgoingData[0], testing::StartsWith(moveNoMessagesResponse()));
}

TEST_F(CopyMoveTest, uidMoveEmptyMessages)
{
    auto future = runUidMove(emptySequence, testFolderName);
    ASSERT_TRUE(future->ready());
    ASSERT_TRUE(testMetaBackend->allRequestsEmpty());
    ASSERT_EQ(session->outgoingData.size(), 1);
    ASSERT_THAT(session->outgoingData[0], testing::StartsWith(uidMoveNoMessagesResponse()));
}

INSTANTIATE_TEST_SUITE_P(
    InstantiationName,
    CopyMoveTest,
    ::testing::Values("COPY"s, "UID COPY"s, "MOVE"s, "UID MOVE"s));
