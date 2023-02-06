#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <tests/common.hpp>
#include <src/imap_result_debug.hpp>

using namespace ymod_imap_client;

TEST(CLIENT_TEST, MAILRU_TEST)
{
    try
    {
        static std::vector<std::string> YAPOPTEST = { "yapoptest@mail.ru", "poptestpoptest" };
        auto user = YAPOPTEST;

        auto context = boost::make_shared<TestContext>();
        auto imapClient = ImapClient::connect(context, "imap.mail.ru", 993, true, true);
        L_(info) << "CONNECTED — OK " << imapClient->serverIp();

        auto& capa = *imapClient->capability().get();
        L_(info) << "CAPABILITY — OK: " << debugString(capa) << std::endl;
        EXPECT_TRUE(capa.id && capa.move && capa.xlist && capa.xoauth2 && capa.authPlain);

        auto id = *imapClient->id().get();
        L_(info) << "ID — OK: " << debugString(id) << std::endl;

        auto authResult = imapClient->authPlain(user[0], user[1]).get();
        L_(info) << "LOGIN — OK" << std::endl;

        auto listResult = imapClient->list("", "*").get();
        std::vector<std::string> targetFolders = {
            "INBOX", "Спам", "Отправленные", "Черновики", "Корзина"
        };
        L_(info) << "LIST — folders: " << debugString(*listResult) << std::endl;
        EXPECT_TRUE(debugHas(*listResult, targetFolders));

        auto statusResult = imapClient->status("INBOX", "MESSAGES UIDVALIDITY").get();
        L_(info) << "STATUS — OK: " << debugString(*statusResult) << std::endl;

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
    }
    catch (const std::exception& e)
    {
        EXPECT_TRUE(false) << "Got exception: " << e.what();
    }
}
