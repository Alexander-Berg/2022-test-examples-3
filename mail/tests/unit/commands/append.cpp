#include "command_test_base.h"
#include "../src/backend/append/append.h"

using namespace yimap;
using namespace yimap::backend;

struct AppendTest : CommandTestBase
{
    string commandName = "APPEND"s;
    string inboxName = "Inbox"s;
    string inboxFid = "1"s;
    string notExistingFolder = "NotExistingFolder"s;
    string badEncodedFolder = "папка"s;
    string userFlag = "user_flag"s;
    string systemFlag = "\\Seen"s;
    string unparseableDate = "xxx@!#-x666"s;
    string testMessageBody = "Subject: test subject\r\n\r\ntest body"s;
    string mid = "12345"s;
    uint64_t imapId = 6789;
    uint64_t num = 100;

    AppendTest()
    {
        context->sessionState.setAuthenticated();
    }

    auto createCommand()
    {
        std::stringstream str;
        str << inboxName << " {" << testMessageBody.length() << "}\r\n" << testMessageBody;
        return CommandTestBase::createCommand(commandName, str.str());
    }

    auto createCommandWithNotExistingFolder()
    {
        std::stringstream str;
        str << notExistingFolder << " {" << testMessageBody.length() << "}\r\n" << testMessageBody;
        return CommandTestBase::createCommand(commandName, str.str());
    }

    auto createCommandWithBadEncodedFolder()
    {
        std::stringstream str;
        str << badEncodedFolder << " {" << testMessageBody.length() << "}\r\n" << testMessageBody;
        return CommandTestBase::createCommand(commandName, str.str());
    }

    auto createCommandWithFlags()
    {
        std::stringstream str;
        str << inboxName << " (" << userFlag << " " << systemFlag << ") {"
            << testMessageBody.length() << "}\r\n"
            << testMessageBody;
        return CommandTestBase::createCommand(commandName, str.str());
    }

    auto& getBackendRequest()
    {
        auto& requests = testAppend->getAppendRequests();
        if (requests.size() != 1)
        {
            throw std::runtime_error(
                "should be exactly one append request, but found " +
                std::to_string(requests.size()));
        }
        return requests.back().first;
    }

    auto& getBackendResponsePromise()
    {
        auto& requests = testAppend->getAppendRequests();
        if (requests.size() != 1)
        {
            throw std::runtime_error(
                "should be exactly one append request, but found " +
                std::to_string(requests.size()));
        }
        return requests.back().second;
    }

    void sendGoodAppendResponse()
    {
        getBackendResponsePromise().set({});
        runIO();
    }

    void sendBadAppendResponse()
    {
        getBackendResponsePromise().set_exception(std::runtime_error("backend error"));
        runIO();
    }

    void sendAppendResponseWithMidAndImapId()
    {
        AppendResult res;
        res.mid = mid;
        res.uid = std::to_string(imapId);
        getBackendResponsePromise().set(res);
        runIO();
    }

    void sendDuplicateAppendResponse()
    {
        AppendResult res;
        res.mid = mid;
        res.uid = "none";
        getBackendResponsePromise().set(res);
        runIO();
    }

    auto selectInbox()
    {
        auto folder = testMetaBackend->getFolder(DBFolderId(inboxName, inboxFid)).get();
        context->sessionState.selectFolderReadOnly(folder);
        return folder->copyFolderInfo();
    }

    void setStatusUpdateWithNewMessage()
    {
        auto inbox = testMetaBackend->getFolderInfo(DBFolderId(inboxName, inboxFid)).get();
        MessageData newMessage;
        newMessage.uid = imapId;
        newMessage.num = num;
        MessageVector newMessages;
        newMessages.push_back(newMessage);
        auto data =
            std::make_shared<MailboxDiff>(inbox, MessageVector{}, MessageVector{}, newMessages);
        testMetaBackend->setStatusUpdateData(data);
    }

    auto appendOkResponse()
    {
        return commandTag() + " OK " + commandName + " Completed";
    }

    auto appendOkWithImapIdResponse(uint64_t uidValidity, uint64_t imapId)
    {
        return commandTag() + " OK [APPENDUID " + std::to_string(uidValidity) + " " +
            std::to_string(imapId) + "] " + commandName + " Completed";
    }

    auto appendNoSuchFolderResponse()
    {
        return commandTag() + " NO [TRYCREATE] " + commandName + " No such folder";
    }

    auto appendBackendErrorResponse()
    {
        return commandTag() + " NO [UNAVAILABLE] " + commandName + " Backend error";
    }

    auto appendNoBodyErrorResponse()
    {
        return commandTag() + " BAD [SERVERBUG] " + commandName +
            " Invalid arguments, cannot find message part";
    }

    auto appendParseDateErrorResponse()
    {
        return commandTag() + " BAD [SERVERBUG] " + commandName +
            " Invalid arguments, cannot parse date-time";
    }

    auto appendEncodingErrorResponse()
    {
        return commandTag() + " BAD [CLIENTBUG] " + commandName + " Folder encoding error";
    }

    auto statusUpdateWithNewMessageResponse(std::size_t messagesCount)
    {
        return "* "s + std::to_string(messagesCount) + " EXISTS";
    }
};

TEST_F(AppendTest, createCommand)
{
    auto command = createCommand();
    ASSERT_TRUE(command != nullptr);
}

TEST_F(AppendTest, badEnconfing)
{
    startCommand(createCommandWithBadEncodedFolder());
    ASSERT_TRUE(beginsWith(session->outgoingData[0], appendEncodingErrorResponse()))
        << session->dumpOutput();
}

TEST_F(AppendTest, notExistingFolder)
{
    startCommand(createCommandWithNotExistingFolder());
    ASSERT_TRUE(beginsWith(session->outgoingData[0], appendNoSuchFolderResponse()))
        << session->dumpOutput();
}

TEST_F(AppendTest, storeToFolder)
{
    startCommand(createCommand());
    auto backendRequest = getBackendRequest();
    ASSERT_EQ(backendRequest.folder.fid, inboxFid);
    ASSERT_EQ(backendRequest.folder.name, inboxName);
}

TEST_F(AppendTest, storeMessageBody)
{
    startCommand(createCommand());
    auto backendRequest = getBackendRequest();
    ASSERT_EQ(backendRequest.messageBody, testMessageBody);
}

TEST_F(AppendTest, storeWithFlags)
{
    startCommand(createCommandWithFlags());
    auto backendRequest = getBackendRequest();
    ASSERT_EQ(backendRequest.userFlags, std::vector{ userFlag });
    ASSERT_EQ(backendRequest.systemFlags, std::vector{ systemFlag.substr(1) });
}

TEST_F(AppendTest, completeAfterBackendResponse)
{
    auto command = createCommand();
    auto future = command->getFuture();
    startCommand(command);
    sendGoodAppendResponse();
    ASSERT_TRUE(future->ready());
    ASSERT_EQ(future->get(), RET_OK);
}

TEST_F(AppendTest, successResponse)
{
    startCommand(createCommand());
    sendGoodAppendResponse();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], appendOkResponse())) << session->dumpOutput();
}

TEST_F(AppendTest, backendError)
{
    startCommand(createCommand());
    sendBadAppendResponse();
    ASSERT_TRUE(beginsWith(session->outgoingData[0], appendBackendErrorResponse()))
        << session->dumpOutput();
}

TEST_F(AppendTest, responseWithUidValidityAndImapId)
{
    auto inbox = testMetaBackend->getFolderInfo(DBFolderId(inboxName, inboxFid)).get(); // XXX
    startCommand(createCommand());
    sendAppendResponseWithMidAndImapId();
    ASSERT_TRUE(
        beginsWith(session->outgoingData[0], appendOkWithImapIdResponse(inbox.uidValidity, imapId)))
        << session->dumpOutput();
}

TEST_F(AppendTest, duplicated)
{
    auto inbox = testMetaBackend->getFolderInfo(DBFolderId(inboxName, inboxFid)).get(); // XXX
    auto nextImapId = testMetaBackend->getNextRegeneratedImapId();
    startCommand(createCommand());
    sendDuplicateAppendResponse();
    ASSERT_TRUE(beginsWith(
        session->outgoingData[0], appendOkWithImapIdResponse(inbox.uidValidity, nextImapId)))
        << session->dumpOutput();
}

TEST_F(AppendTest, statusUpdate)
{
    auto inbox = selectFolder(inboxName, inboxFid);
    startCommand(createCommand());
    setStatusUpdateWithNewMessage();
    sendGoodAppendResponse();
    ASSERT_TRUE(beginsWith(
        session->outgoingData[0], statusUpdateWithNewMessageResponse(inbox.messageCount + 1)))
        << session->outgoingData[0] << inbox.messageCount;
    ASSERT_TRUE(beginsWith(session->outgoingData[1], appendOkResponse()))
        << session->outgoingData[1];
}
