#include "command_test_base.h"
#include <boost/format.hpp>
#include <macs/mime_part_factory.h>

using namespace yimap;
using namespace yimap::backend;
using namespace testing;

struct FetchTestBase : CommandTestBase
{
    string inboxName = "Inbox"s;
    string inboxFid = "1"s;
    string testSubject = "test subject";
    string testSenderLogin = "yapoptest";
    string testSenderDomain = "yandex.ru";
    string testReceiverLogin = "youpoptest";
    string testReceiverDomain = "yandex.com";
    std::vector<string> testFlags = { "\\Seen", "\\Draft", "\\Recent" };
    string testDateTime = "17-Feb-2020 19:21:17 +0000";
    time_t testTimestamp = 1581967277; // Corresponds to testDateTime.
    string testMessageId = "123456789";
    string testMimeType = "multipart";
    string testMimeSubType = "alternative";
    string testMimeSubPartType = "text";
    string testMimeSubPartSubType = "plain";
    string testBoundary = "====";
    string testStid = "1:stid:01234";
    std::size_t testMessageSize = 300;

    Promise<MailboxDiffPtr> statusUpdatePromise;

    FetchTestBase()
    {
        initMessage();
        initContext();
        selectFolder(inboxName, inboxFid);
    }

    auto& allMessages()
    {
        return testMetaBackend->getMailbox().messages[inboxFid];
    }

    MessageData& message()
    {
        return allMessages().begin()->second;
    }

    MessageData& lastMessage()
    {
        auto& messages = testMetaBackend->getMailbox().messages[inboxFid];
        return messages.rbegin()->second;
    }

    void initMessage()
    {
        message().flags = {};
        message().flags.addFlags(testFlags);
        message().time = testTimestamp;

        macs::MimeParts mimeParts{
            macs::MimePartFactory().hid("1").offsetEnd(testMessageSize).release()
        };
        testMetaBackend->setBodyMetadata(
            { { message().smid(), BodyMetadata{ testStid, mimeParts } } });
    }

    void initContext()
    {
        context->sessionState.setAuthenticated();
        auto folders = testMetaBackend->getFolders().get();
        context->foldersCache.setFolders(folders);
    }

    string testHeader()
    {
        auto ret = boost::format("Subject: %1%\r\n"
                                 "From: %2%@%3%\r\n"
                                 "To: %4%@%5%\r\n"
                                 "Date: %6%\r\n"
                                 "Message-Id: %7%\r\n"
                                 "Content-Transfer-Encoding: base64\r\n"
                                 "Content-Type: %8%/%9%; boundary=\"%10%\"\r\n"
                                 "\r\n") %
            testSubject % testSenderLogin % testSenderDomain % testReceiverLogin %
            testReceiverDomain % testDateTime % testMessageId % testMimeType % testMimeSubType %
            testBoundary;
        return ret.str();
    }

    string testHeaderPart1()
    {
        auto ret = boost::format("Content-Type: %1%/%2%;\r\n\r\n") % testMimeSubPartType %
            testMimeSubPartSubType;
        return ret.str();
    }

    string testBodyPart1()
    {
        return "test";
    }

    string testPart1()
    {
        return testHeaderPart1() + testBodyPart1();
    }

    string testHeaderPart2()
    {
        auto ret = boost::format("Content-Transfer-Encoding: base64\r\n"
                                 "Content-Type: %1%/%2%; charset=utf-8\r\n\r\n") %
            testMimeSubPartType % testMimeSubPartSubType;
        return ret.str();
    }

    string testBodyPart2()
    {
        return "0YLQtdGB0YI=";
    }

    string testPart2()
    {
        return testHeaderPart2() + testBodyPart2();
    }

    string testBody()
    {
        auto ret = boost::format("--%1%\r\n"
                                 "%2%\r\n"
                                 "--%1%\r\n"
                                 "%3%\r\n"
                                 "--%1%--") %
            testBoundary % testPart1() % testPart2();
        return ret.str();
    }

    string testMessage()
    {
        return testHeader() + testBody();
    }

    BodyStructure testBodyStructure()
    {
        BodyStructure part1;
        part1.mime_type = testMimeSubPartType;
        part1.mime_subtype = testMimeSubPartSubType;

        BodyStructure part2;
        part2.mime_type = testMimeSubPartType;
        part2.mime_subtype = testMimeSubPartSubType;

        BodyStructure ret;
        ret.mime_type = testMimeType;
        ret.mime_subtype = testMimeSubType;
        ret.mime_parts.emplace_back(part1);
        ret.mime_parts.emplace_back(part2);
        return ret;
    }

    void addFlag(MessageData& msg, const string& flag)
    {
        msg.flags.addFlags(std::vector<string>{ flag });
        selectFolder(inboxName, inboxFid);
    }

    void delFlag(MessageData& msg, const string& flag)
    {
        msg.flags.delFlags(std::vector<string>{ flag });
        selectFolder(inboxName, inboxFid);
    }

    void addNewMessage()
    {
        auto folder = getFolder(inboxName, inboxFid);
        auto newMessage = lastMessage();
        newMessage.uid = folder->folderInfo.uidNext++;
        newMessage.num = ++folder->folderInfo.messageCount;
        newMessage.mid++;
        auto& messages = testMetaBackend->getMailbox().messages[inboxFid];
        messages[newMessage.uid] = newMessage;
    }

    void interruptOnStatusUpdate()
    {
        testMetaBackend->setStatusUpdateFuture(statusUpdatePromise);
    }

    template <typename Requests>
    auto getRequest(Requests& requests)
    {
        if (requests.empty())
        {
            throw std::runtime_error("no requests");
        }
        auto request = std::move(requests.front());
        requests.pop_front();
        return request;
    }

    TestMbodyBackend::MessageRequest getLoadMessageRequest()
    {
        return getRequest(testMbodyBackend->loadRequests);
    }

    TestMbodyBackend::MessageRequest getLoadBodyRequest()
    {
        return getRequest(testMbodyBackend->loadBodyRequests);
    }

    TestMbodyBackend::MessageRequest getLoadHeaderRequest()
    {
        return getRequest(testMbodyBackend->loadHeaderRequests);
    }

    TestMbodyBackend::BodyStructureRequest getLoadBodyStructureRequest()
    {
        return getRequest(testMbodyBackend->loadBodyStructureRequests);
    }

    string literal(const string& s)
    {
        return "{" + std::to_string(s.size()) + "}\r\n" + s;
    }

    void respondStatusUpdate()
    {
        auto folder = getFolder(inboxName, inboxFid);
        auto mailboxDiff = std::make_shared<MailboxDiff>(folder->copyFolderInfo());
        statusUpdatePromise.set(mailboxDiff);
        runIO();
    }

    void respond(const TestMbodyBackend::MessageRequest& request, const string& response)
    {
        request.handler({}, std::make_shared<string>(response));
        runIO();
    }

    void respondWithError(const TestMbodyBackend::MessageRequest& request, const string& err)
    {
        request.handler(err, {});
        runIO();
    }

    void respond(TestMbodyBackend::BodyStructureRequest&& request, const BodyStructure& response)
    {
        request.promise.set(std::make_shared<BodyStructure>(response));
        runIO();
    }

    string addressResponse(const string& login, const string& domain)
    {
        auto ret = boost::format("((NIL NIL \"%1%\" \"%2%\"))") % login % domain;
        return ret.str();
    }

    string envelopeResponse()
    {
        auto ret = boost::format("ENVELOPE (\"%1%\" \"%2%\" %3% %3% %3% %4% NIL NIL NIL \"%5%\")") %
            testDateTime % testSubject % addressResponse(testSenderLogin, testSenderDomain) %
            addressResponse(testReceiverLogin, testReceiverDomain) % testMessageId;
        return ret.str();
    }

    string internalDateResponse()
    {
        auto ret = boost::format("INTERNALDATE \"%1%\"") % testDateTime;
        return ret.str();
    }

    string bodyStructureResponse()
    {
        string subPart = "\"text\" \"plain\" NIL NIL NIL \"7BIT\" 0 0";
        auto ret = boost::format("BODY ((%1%)(%2%) \"%3%\")") % subPart % subPart % testMimeSubType;
        return ret.str();
    }

    string flagsResponse()
    {
        auto ret = boost::format("FLAGS (%1%)") % boost::algorithm::join(testFlags, " ");
        return ret.str();
    }

    string bodyStructureExtendedResponse()
    {
        return "BODYSTRUCTURE (\"TEXT\" \"PLAIN\" NIL NIL NIL \"7BIT\" 0 0 NIL NIL NIL NIL)"s;
    }

    string headerFieldsMessageIdResponse()
    {
        auto messageId = boost::format("Message-Id: %1%\r\n\r\n") % testMessageId;
        auto ret = boost::format("BODY[HEADER.FIELDS (Message-Id)] %1%") % literal(messageId.str());
        return ret.str();
    }

    string macroFastResponse()
    {
        auto ret = boost::format("%1% %2% RFC822.SIZE %3%") % flagsResponse() %
            internalDateResponse() % message().details.size;
        return ret.str();
    }

    string macroAllResponse()
    {
        auto ret = boost::format("%1% %2% %3% RFC822.SIZE %4%") % envelopeResponse() %
            flagsResponse() % internalDateResponse() % message().details.size;
        return ret.str();
    }

    string macroFullResponse()
    {
        auto ret = boost::format("%1% %2% %3% %4% RFC822.SIZE %5%") % bodyStructureResponse() %
            envelopeResponse() % flagsResponse() % internalDateResponse() % message().details.size;
        return ret.str();
    }

    string untaggedResponse(const string& atts, size_t num = 1)
    {
        auto ret = boost::format("* %1% FETCH (%2%)\r\n") % num % atts;
        return ret.str();
    }

    string untaggedResponse(const boost::format& format, size_t num = 1)
    {
        return untaggedResponse(format.str(), num);
    }

    string untaggedErrorResponse()
    {
        auto ret =
            boost::format(
                "* NO [UNAVAILABLE] FETCH message %1% uid %2%: failure due to backend error\r\n") %
            message().num % message().uid;
        return ret.str();
    }

    string allUidsUntaggedResponse()
    {
        string res;
        for (auto&& [uid, message] : allMessages())
        {
            res += untaggedResponse(boost::format("UID %1%") % message.uid, message.num);
        }
        return res;
    }
};

struct FetchTest : FetchTestBase
{
    CommandPtr createCommand(const string& args)
    {
        return CommandTestBase::createCommand("FETCH", args);
    }

    CommandPtr createCommand(const boost::format& args)
    {
        return createCommand(args.str());
    }

    string okResponse()
    {
        return commandTag() + " OK FETCH Completed";
    }

    string backendErrorResponse()
    {
        return commandTag() + " NO [UNAVAILABLE] FETCH Completed";
    }
};

struct UidFetchTest : FetchTestBase
{
    CommandPtr createCommand(const string& args)
    {
        return CommandTestBase::createCommand("UID FETCH", args);
    }

    CommandPtr createCommand(const boost::format& args)
    {
        return createCommand(args.str());
    }

    string okResponse()
    {
        return commandTag() + " OK UID FETCH Completed";
    }
};

TEST_F(FetchTest, create)
{
    auto command = createCommand("1 UID");
    ASSERT_TRUE(command != nullptr);
}

TEST_F(UidFetchTest, create)
{
    auto command = createCommand("1 UID");
    ASSERT_TRUE(command != nullptr);
}

TEST_F(FetchTest, uid)
{
    startCommand(createCommand("1 UID"));
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(untaggedResponse(boost::format("UID %1%") % message().uid) + okResponse()));
}

TEST_F(UidFetchTest, uid)
{
    startCommand(createCommand(boost::format("%1% UID") % message().uid));
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(untaggedResponse(boost::format("UID %1%") % message().uid) + okResponse()));
}

TEST_F(FetchTest, flags)
{
    startCommand(createCommand("1 FLAGS"));
    ASSERT_THAT(
        session->dumpOutput(), StartsWith(untaggedResponse(flagsResponse()) + okResponse()));
}

TEST_F(FetchTest, internalDate)
{
    startCommand(createCommand("1 INTERNALDATE"));
    ASSERT_THAT(
        session->dumpOutput(), StartsWith(untaggedResponse(internalDateResponse()) + okResponse()));
}

TEST_F(FetchTest, bodyRequest)
{
    startCommand(createCommand("1 BODY[]"));
    auto request = getLoadMessageRequest();
    ASSERT_EQ(request.stid, testStid);
    ASSERT_EQ(request.part, "");
}

TEST_F(FetchTest, partRequest)
{
    startCommand(createCommand("1 BODY[1]"));
    auto request = getLoadBodyRequest();
    ASSERT_EQ(request.stid, testStid);
    ASSERT_EQ(request.part, "1");
}

TEST_F(FetchTest, textPartRequest)
{
    startCommand(createCommand("1 BODY[1.TEXT]"));
    auto request = getLoadBodyRequest();
    ASSERT_EQ(request.stid, testStid);
    ASSERT_EQ(request.part, "1");
}

TEST_F(FetchTest, envelope)
{
    startCommand(createCommand("1 ENVELOPE"));
    respond(getLoadHeaderRequest(), testHeader());
    ASSERT_THAT(
        session->dumpOutput(), StartsWith(untaggedResponse(envelopeResponse()) + okResponse()));
}

TEST_F(FetchTest, body)
{
    startCommand(createCommand("1 BODY[]"));
    respond(getLoadMessageRequest(), testMessage());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BODY[] %1%") % literal(testMessage())) + okResponse()));
}

TEST_F(FetchTest, markSeenOnFetchBody)
{
    delFlag(message(), "\\Seen");
    startCommand(createCommand("1 BODY[]"));
    respond(getLoadMessageRequest(), testMessage());
    ASSERT_TRUE(message().flags.hasFlag("\\Seen"));
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(
                boost::format("BODY[] %1% %2%") % literal(testMessage()) % flagsResponse()) +
            okResponse()));
}

TEST_F(FetchTest, bodyPeek)
{
    delFlag(message(), "\\Seen");
    startCommand(createCommand("1 BODY.PEEK[]"));
    respond(getLoadMessageRequest(), testMessage());
    ASSERT_FALSE(message().flags.hasFlag("\\Seen"));
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BODY[] %1%") % literal(testMessage())) + okResponse()));
}

TEST_F(FetchTest, bodyPartial)
{
    startCommand(createCommand("1 BODY[]<5.10>"));
    respond(getLoadMessageRequest(), testMessage());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(
                boost::format("BODY[]<5> %1%") % literal(testMessage().substr(5, 10))) +
            okResponse()));
}

TEST_F(FetchTest, bodyError)
{
    startCommand(createCommand("1 BODY[]"));
    respondWithError(getLoadMessageRequest(), "test error");
    ASSERT_THAT(
        session->dumpOutput(), StartsWith(untaggedErrorResponse() + backendErrorResponse()));
}

TEST_F(FetchTest, text)
{
    startCommand(createCommand("1 BODY[TEXT]"));
    respond(getLoadBodyRequest(), testBody());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BODY[TEXT] %1%") % literal(testBody())) +
            okResponse()));
}

TEST_F(FetchTest, textPart)
{
    startCommand(createCommand("1 BODY[1.TEXT]"));
    respond(getLoadBodyRequest(), testBodyPart1());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BODY[1.TEXT] %1%") % literal(testBodyPart1())) +
            okResponse()));
}

TEST_F(FetchTest, bodyPeekText)
{
    delFlag(message(), "\\Seen");
    startCommand(createCommand("1 BODY.PEEK[TEXT]"));
    respond(getLoadBodyRequest(), testBody());
    ASSERT_FALSE(message().flags.hasFlag("\\Seen"));
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BODY[TEXT] %1%") % literal(testBody())) +
            okResponse()));
}

TEST_F(FetchTest, header)
{
    startCommand(createCommand("1 BODY[HEADER]"));
    respond(getLoadHeaderRequest(), testHeader());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BODY[HEADER] %1%") % literal(testHeader())) +
            okResponse()));
}

TEST_F(FetchTest, headerPeek)
{
    delFlag(message(), "\\Seen");
    startCommand(createCommand("1 BODY.PEEK[HEADER]"));
    respond(getLoadHeaderRequest(), testHeader());
    ASSERT_FALSE(message().flags.hasFlag("\\Seen"));
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BODY[HEADER] %1%") % literal(testHeader())) +
            okResponse()));
}

TEST_F(FetchTest, headerPartial)
{
    startCommand(createCommand("1 BODY[HEADER]<3.5>"));
    respond(getLoadHeaderRequest(), testHeader());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(
                boost::format("BODY[HEADER]<3> %1%") % literal(testHeader().substr(3, 5))) +
            okResponse()));
}

TEST_F(FetchTest, headerFields)
{
    startCommand(createCommand("1 BODY[HEADER.FIELDS (Message-Id)]"));
    respond(getLoadHeaderRequest(), testHeader());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(untaggedResponse(headerFieldsMessageIdResponse()) + okResponse()));
}

TEST_F(FetchTest, part)
{
    startCommand(createCommand("1 BODY[1]"));
    respond(getLoadBodyRequest(), testBodyPart1());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BODY[1] %1%") % literal(testBodyPart1())) +
            okResponse()));
}

TEST_F(FetchTest, rfc822)
{
    startCommand(createCommand("1 RFC822"));
    respond(getLoadMessageRequest(), testMessage());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("RFC822 %1%") % literal(testMessage())) + okResponse()));
}

TEST_F(FetchTest, rfc822Header)
{
    startCommand(createCommand("1 RFC822.HEADER"));
    respond(getLoadHeaderRequest(), testHeader());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("RFC822.HEADER %1%") % literal(testHeader())) +
            okResponse()));
}

TEST_F(FetchTest, rfc822Text)
{
    startCommand(createCommand("1 RFC822.TEXT"));
    respond(getLoadBodyRequest(), testBody());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("RFC822.TEXT %1%") % literal(testBody())) +
            okResponse()));
}

TEST_F(FetchTest, rfc822size)
{
    startCommand(createCommand("1 RFC822.SIZE"));
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("RFC822.SIZE %1%") % message().details.size) +
            okResponse()));
}

TEST_F(FetchTest, bodyStructure)
{
    startCommand(createCommand("1 BODY"));
    respond(getLoadBodyStructureRequest(), testBodyStructure());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(untaggedResponse(bodyStructureResponse()) + okResponse()));
}

TEST_F(FetchTest, bodyStructureExtended)
{
    startCommand(createCommand("1 BODYSTRUCTURE"));
    respond(getLoadBodyStructureRequest(), {});
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(untaggedResponse(bodyStructureExtendedResponse()) + okResponse()));
}

TEST_F(FetchTest, binary)
{
    startCommand(createCommand("1 BINARY[]"));
    respond(getLoadMessageRequest(), testMessage());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BINARY[] %1%") % literal(testMessage())) +
            okResponse())); // Bug RTEC-3893
}

TEST_F(FetchTest, binaryPeek)
{
    delFlag(message(), "\\Seen");
    startCommand(createCommand("1 BINARY.PEEK[]"));
    respond(getLoadMessageRequest(), testMessage());
    ASSERT_FALSE(message().flags.hasFlag("\\Seen"));
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BINARY[] %1%") % literal(testMessage())) +
            okResponse())); // Bug RTEC-3893
}

TEST_F(FetchTest, binaryPart)
{
    startCommand(createCommand("1 BINARY[2]"));
    respond(getLoadMessageRequest(), testPart2());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BINARY[2] %1%") % literal(testBodyPart2())) +
            okResponse())); // Bug RTEC-3893
}

TEST_F(FetchTest, binaryPartPeek)
{
    delFlag(message(), "\\Seen");
    startCommand(createCommand("1 BINARY.PEEK[2]"));
    respond(getLoadMessageRequest(), testPart2());
    ASSERT_FALSE(message().flags.hasFlag("\\Seen"));
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BINARY[2] %1%") % literal(testBodyPart2())) +
            okResponse())); // Bug RTEC-3893
}

TEST_F(FetchTest, binaryHeader)
{
    startCommand(createCommand("1 BINARY[HEADER]"));
    respond(getLoadHeaderRequest(), testHeader());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BINARY[HEADER] %1%") % literal(testHeader())) +
            okResponse()));
}

TEST_F(FetchTest, binaryText)
{
    startCommand(createCommand("1 BINARY[TEXT]"));
    respond(getLoadBodyRequest(), testBody());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BINARY[TEXT] %1%") % literal(testBody())) +
            okResponse())); // Bug RTEC-3893
}

TEST_F(FetchTest, binarySize)
{
    startCommand(createCommand("1 BINARY.SIZE[]"));
    respond(getLoadMessageRequest(), testMessage());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BINARY.SIZE[] %1%") % testMessage().size()) +
            okResponse())); // Bug RTEC-3893
}

TEST_F(FetchTest, binarySizePart)
{
    startCommand(createCommand("1 BINARY.SIZE[2]"));
    respond(getLoadMessageRequest(), testPart2());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("BINARY.SIZE[2] %1%") % testBodyPart2().size()) +
            okResponse())); // Bug RTEC-3893
}

TEST_F(FetchTest, macroFast)
{
    startCommand(createCommand("1 FAST"));
    ASSERT_THAT(
        session->dumpOutput(), StartsWith(untaggedResponse(macroFastResponse()) + okResponse()));
}

TEST_F(FetchTest, macroAll)
{
    startCommand(createCommand("1 ALL"));
    respond(getLoadHeaderRequest(), testHeader());
    ASSERT_THAT(
        session->dumpOutput(), StartsWith(untaggedResponse(macroAllResponse()) + okResponse()));
}

TEST_F(FetchTest, macroFull)
{
    startCommand(createCommand("1 FULL"));
    respond(getLoadBodyStructureRequest(), testBodyStructure());
    respond(getLoadHeaderRequest(), testHeader());
    ASSERT_THAT(
        session->dumpOutput(), StartsWith(untaggedResponse(macroFullResponse()) + okResponse()));
}

TEST_F(UidFetchTest, macroFull)
{
    startCommand(createCommand(boost::format("%1% FULL") % message().uid));
    respond(getLoadBodyStructureRequest(), testBodyStructure());
    respond(getLoadHeaderRequest(), testHeader());
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("UID %1% %2%") % message().uid % macroFullResponse()) +
            okResponse()));
}

TEST_F(UidFetchTest, fetchStar)
{
    startCommand(createCommand("* UID"));
    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("UID %1%") % lastMessage().uid, lastMessage().num) +
            okResponse()));
}

TEST_F(UidFetchTest, fetchStarAfterStatusUpdate)
{
    interruptOnStatusUpdate();
    startCommand(createCommand("* UID"));
    addNewMessage();
    respondStatusUpdate();

    ASSERT_THAT(
        session->dumpOutput(),
        StartsWith(
            untaggedResponse(boost::format("UID %1%") % lastMessage().uid, lastMessage().num) +
            okResponse()));
}

TEST_F(FetchTest, allMessagesUid)
{
    startCommand(createCommand("1:* UID"));
    ASSERT_THAT(session->dumpOutput(), StartsWith(allUidsUntaggedResponse() + okResponse()));
}
