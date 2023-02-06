#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <tests/common.hpp>
#include <src/imap_result_debug.hpp>

using namespace ymod_imap_client;

TEST(CLIENT_TEST, GMAIL_TEST)
{
    try
    {
        std::vector<std::string> yapopTest = { "yapoptest", "poptestqa" };
        auto user = yapopTest;

        auto context = boost::make_shared<TestContext>();
        auto imapClient = ImapClient::connect(context, "imap.gmail.com", 993, true, true);
        L_(info) << "CONNECTED to GMAIL — OK " << imapClient->serverIp();

        auto& capa = *imapClient->capability().get();
        L_(info) << "CAPABILITY — OK: " << debugString(capa) << std::endl;
        EXPECT_TRUE(
            capa.id && capa.idle && capa.xoauth2 && capa.xlist && capa.gmailEx && capa.authPlain);

        auto id = *imapClient->id().get();
        L_(info) << "ID — OK: " << debugString(id) << std::endl;

        auto authResult = imapClient->authPlain(user[0], user[1]).get();
        L_(info) << "LOGIN — OK" << std::endl;

        std::vector<std::string> targetFolders{
            "INBOX",           "[Gmail]/Важное",       "[Gmail]/Вся почта",
            "[Gmail]/Корзина", "[Gmail]/Отправленные", "[Gmail]/Помеченные",
            "[Gmail]/Спам"
        };
        auto listResult = imapClient->list("", "*").get();
        L_(info) << "LIST — folders: " << debugString(*listResult) << std::endl;
        EXPECT_TRUE(debugHas(*listResult, targetFolders));

        auto inboxResult = imapClient->examine(Utf8MailboxName("[Gmail]/Вся почта", '/')).get();
        L_(info) << "EXAMINE INBOX — OK: " << debugString(*inboxResult) << std::endl;

        auto fetchResult = imapClient->fetch("1:*", "UID FLAGS X-GM-LABELS").get();
        L_(info) << "FETCH ALL — OK: " << fetchResult->messages.size() << " messages" << std::endl;
        L_(info) << "MESSAGES: " << debugString(*fetchResult) << std::endl;

        auto logoutResult = imapClient->logout().get();
    }
    catch (const std::exception& e)
    {
        EXPECT_TRUE(false) << "Got exception: " << e.what();
        L_(info) << "ImapTestFailed:" << e.what() << std::endl;
        L_(info) << std::endl << prefix << "GMAIL test — FAILED" << std::endl << std::endl;
    }
}
