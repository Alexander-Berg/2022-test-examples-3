#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <tests/common.hpp>
#include <src/imap_result_debug.hpp>
#include <src/client_session.h>

using namespace ymod_imap_client;

static std::vector<std::string> IMAP_PG = { "imap.pg", "testqa" };
static std::vector<std::string> YAPOPTEST = { "yapoptest", "poptest" };

TEST(TestNewImapClient, testNewImapClient)
{
    try
    {
        auto user = YAPOPTEST;

        L_(info) << prefix;
        L_(info) << "Starting IMAP New Client test.";
        L_(info) << prefix << std::endl;

        auto context = boost::make_shared<TestContext>();
        auto imapClient = ImapClient::connect(context, "imap.yandex.ru", 993, true, true);
        L_(info) << "CONNECTED — OK " << imapClient->serverIp();

        auto& capa = *imapClient->capability().get();
        L_(info) << "CAPABILITY — OK: " << debugString(capa) << std::endl;
        EXPECT_TRUE(capa.id && capa.idle && capa.move && capa.xlist && capa.authPlain);

        auto id = *imapClient->id().get();
        L_(info) << "ID — OK: " << debugString(id) << std::endl;

        auto authResult = imapClient->authPlain(user[0], user[1]).get();
        L_(info) << "LOGIN — OK" << std::endl;

        auto listResult = imapClient->list("", "*").get();
        std::vector<std::string> targetFolders = { "INBOX", "Sent",  "Outbox",
                                                   "Spam",  "Trash", "Drafts" };
        L_(info) << "LIST — folders: " << debugString(*listResult) << std::endl;
        EXPECT_TRUE(debugHas(*listResult, targetFolders));

        auto statusResult = imapClient->status("INBOX", "MESSAGES UIDVALIDITY").get();
        L_(info) << "STATUS — OK: " << debugString(*statusResult) << std::endl;

        auto spamResult = imapClient->examine(Utf8MailboxName("Spam", '|')).get();
        EXPECT_TRUE(spamResult->exists_ == 0);

        auto inboxResult = imapClient->examine(Utf8MailboxName("INBOX", '|')).get();
        L_(info) << "EXAMINE INBOX — OK: " << debugString(*inboxResult) << std::endl;

        auto fetchAllResult = imapClient->fetch("1:*", "UID FLAGS").get();
        L_(info) << "FETCH ALL — OK: " << fetchAllResult->messages.size() << " messages"
                 << std::endl;

        auto fetchFlagsResult = imapClient->uidFetchFlags("1:*").get();
        L_(info) << "FETCH ALL — OK: " << fetchFlagsResult->messages.size() << " messages"
                 << std::endl;

        EXPECT_TRUE(fetchFlagsResult->messages == fetchAllResult->messages);

        auto count = 0;
        auto messages = fetchFlagsResult->messages;
        for (auto message : messages)
        {
            if (++count > 5) break;

            auto messageBody = imapClient->uidFetchBody(message.uid).get();
            L_(info) << "UID FETCH " << message.uid << " — OK: " << messageBody->body.size()
                     << " bytes" << std::endl;
        }

        auto logoutResult = imapClient->logout().get();

        L_(info) << prefix;
        L_(info) << "New client test — ok" << std::endl << std::endl;
    }
    catch (const std::exception& e)
    {
        EXPECT_TRUE(false) << "Got exception: " << e.what();
        L_(info) << "ImapTestFailed:" << e.what() << std::endl;
        L_(info) << std::endl << prefix << "New client test — FAILED" << std::endl << std::endl;
    }
}

TEST(TestStartTls, STARTTLS_TEST)
{
    try
    {
        auto context = boost::make_shared<TestContext>();
        auto imapClient = ImapClient::connect(context, "imap.yandex.ru", 143, false, true);
        L_(info) << "CONNECTED — OK " << imapClient->serverIp();

        auto tls = *imapClient->startTls().get();
        L_(info) << "STARTTLS — OK";

        auto user = YAPOPTEST;
        auto authResult = imapClient->login(user[0], user[1]).get();
        L_(info) << "LOGIN — OK" << std::endl;

        auto logoutResult = imapClient->logout().get();
    }
    catch (const std::exception& e)
    {
        EXPECT_TRUE(false) << "Got exception: " << e.what();
        L_(info) << "ImapTestFailed:" << e.what() << std::endl;
    }
}

TEST(TestAuthPlain, AUTHPLAIN_TEST)
{
    auto debugCounter = ClientSession::debugSessionCounter();
    try
    {
        auto user = YAPOPTEST;

        L_(info) << prefix;
        L_(info) << "Starting IMAP New Client test.";
        L_(info) << prefix << std::endl;

        auto context = boost::make_shared<TestContext>();
        auto imapClient = ImapClient::connect(context, "imap.yandex.ru", 993, true, true);
        L_(info) << "CONNECTED — OK " << imapClient->serverIp();

        auto& capa = *imapClient->capability().get();
        L_(info) << "CAPABILITY — OK: " << debugString(capa) << std::endl;
        EXPECT_TRUE(capa.id && capa.idle && capa.move && capa.xlist);

        auto authResult = imapClient->authPlain(user[0], user[1]).get();
        L_(info) << "LOGIN — OK" << std::endl;
        auto logoutResult = imapClient->logout().get();
    }
    catch (const std::exception& e)
    {
        EXPECT_TRUE(debugCounter == ClientSession::debugSessionCounter())
            << "SESSION LEAKAGE!!!111 Now:" << ClientSession::debugSessionCounter();
        EXPECT_TRUE(false) << "Got exception: " << e.what();
        L_(info) << "ImapTestFailed:" << e.what() << std::endl;
    }
    EXPECT_TRUE(debugCounter == ClientSession::debugSessionCounter())
        << "SESSION LEAKAGE!!!111 Now:" << ClientSession::debugSessionCounter();
}
