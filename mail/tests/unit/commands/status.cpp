#include "command_test_base.h"
#include <boost/algorithm/string/join.hpp>

using namespace yimap;
using namespace yimap::backend;

struct StatusTest : CommandTestBase
{
    string commandName = "STATUS"s;
    string messagesCount = "MESSAGES"s;
    string recentCount = "RECENT"s;
    string unseenCount = "UNSEEN"s;
    string uidNext = "UIDNEXT"s;
    string uidValidity = "UIDVALIDITY"s;
    string inboxName = "Inbox"s;
    string inboxFid = "1"s;
    string notExistingFolder = "NotExistingFolder"s;
    string badEncodedFolder = "папка"s;
    string cachedNetMask = "6.6.6.0/19"s;
    string cachedIp = "6.6.6.6"s;
    size_t cacheTTL = 100500;
    size_t fakeMessageCount = 666;

    StatusTest()
    {
        settings->ipHints.emplace_back(IpHintsEntry(cachedNetMask, "nolog", cacheTTL));
        context->sessionState.setAuthenticated();
    }

    auto createCommand(const std::vector<string>& attrs)
    {
        return CommandTestBase::createCommand(
            commandName, inboxName + " (" + boost::algorithm::join(attrs, " ") + ")");
    }

    auto createCommandWithBadEncodingFolder()
    {
        return CommandTestBase::createCommand(
            commandName, badEncodedFolder + " (" + messagesCount + ")");
    }

    auto createCommandWithNotExistingFolder()
    {
        return CommandTestBase::createCommand(
            commandName, notExistingFolder + " (" + messagesCount + ")");
    }

    auto folderInfo(const string& displayName, const string& fid)
    {
        DBFolderId folder;
        folder.name = displayName;
        folder.fid = fid;
        return testMetaBackend->getFolderInfo(folder).get();
    }

    void setCachedIp()
    {
        context->sessionInfo.remoteAddress = cachedIp;
    }

    void prepareCache(const string& displayName, const string& fid, size_t messagesCount)
    {
        auto folders = testMetaBackend->folderList();
        context->foldersCache.setFolders(folders);

        auto info = folderInfo(displayName, fid);
        info.messageCount = messagesCount;
        context->statusCache->setFolder(displayName, info, cacheTTL);
    }

    auto messagesCountResponse(const string& folderName, const string& fid)
    {
        return "* " + commandName + " " + folderName + " (MESSAGES " +
            std::to_string(folderInfo(folderName, fid).messageCount) + ")";
    }

    auto messagesCountResponse(const string& folder, size_t count)
    {
        return "* " + commandName + " " + folder + " (MESSAGES " + std::to_string(count) + ")";
    }

    auto uidNextResponse(const string& folderName, const string& fid)
    {
        return "* " + commandName + " " + folderName + " (UIDNEXT " +
            std::to_string(folderInfo(folderName, fid).uidNext) + ")";
    }

    auto fullResponse(const string& folderName, const string& fid)
    {
        auto info = folderInfo(folderName, fid);
        std::stringstream str;
        str << "* " << commandName << " " << folderName << " (MESSAGES " << info.messageCount
            << " RECENT " << info.recentCount << " UNSEEN " << info.unseenCount << " UIDNEXT "
            << info.uidNext << " UIDVALIDITY " << info.uidValidity << ")";
        return str.str();
    }

    auto statusOkResponse()
    {
        return commandTag() + " OK " + commandName + " Completed";
    }

    auto badEncodingResponse()
    {
        return commandTag() + " BAD [CLIENTBUG] " + commandName + " Folder encoding error";
    }

    auto notExistingFolderResponse()
    {
        return commandTag() + " NO [TRYCREATE] " + commandName + " No such folder";
    }
};

TEST_F(StatusTest, createCommand)
{
    auto command = createCommand({ messagesCount });
    ASSERT_TRUE(command != nullptr);
}

TEST_F(StatusTest, badEncodingFolder)
{
    startCommand(createCommandWithBadEncodingFolder());
    ASSERT_TRUE(beginsWith(session->outgoingData[0], badEncodingResponse()))
        << session->outgoingData[0];
}

TEST_F(StatusTest, notExistingFolder)
{
    startCommand(createCommandWithNotExistingFolder());
    ASSERT_TRUE(beginsWith(session->outgoingData[0], notExistingFolderResponse()))
        << session->outgoingData[0];
}

TEST_F(StatusTest, messagesCount)
{
    startCommand(createCommand({ messagesCount }));
    ASSERT_TRUE(beginsWith(session->outgoingData[0], messagesCountResponse(inboxName, inboxFid)))
        << session->outgoingData[0];
    ASSERT_TRUE(beginsWith(session->outgoingData[1], statusOkResponse()))
        << session->outgoingData[1];
}

TEST_F(StatusTest, uidnext)
{
    startCommand(createCommand({ uidNext }));
    ASSERT_TRUE(beginsWith(session->outgoingData[0], uidNextResponse(inboxName, inboxFid)))
        << session->outgoingData[0];
    ASSERT_TRUE(beginsWith(session->outgoingData[1], statusOkResponse()))
        << session->outgoingData[1];
}

TEST_F(StatusTest, fullResponse)
{
    startCommand(createCommand({ messagesCount, recentCount, unseenCount, uidNext, uidValidity }));
    ASSERT_TRUE(beginsWith(session->outgoingData[0], fullResponse(inboxName, inboxFid)))
        << session->outgoingData[0];
    ASSERT_TRUE(beginsWith(session->outgoingData[1], statusOkResponse()))
        << session->outgoingData[1];
}

TEST_F(StatusTest, updateCache)
{
    setCachedIp();
    startCommand(createCommand({ messagesCount }));
    ASSERT_TRUE(context->statusCache->getFolder(inboxName));
}

TEST_F(StatusTest, useCache)
{
    setCachedIp();
    prepareCache(inboxName, inboxFid, fakeMessageCount);
    startCommand(createCommand({ messagesCount }));
    ASSERT_TRUE(
        beginsWith(session->outgoingData[0], messagesCountResponse(inboxName, fakeMessageCount)))
        << session->outgoingData[0];
    ASSERT_TRUE(beginsWith(session->outgoingData[1], statusOkResponse()))
        << session->outgoingData[1];
}
