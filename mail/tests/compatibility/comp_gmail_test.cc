#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <tests/common.hpp>
#include <src/imap_result_debug.hpp>

static std::vector<std::string> GMAIL_YAPOPTEST = { "yapoptest", "poptestqa" };
static std::set<std::string> GMAIL_FOLDERS = {
    "INBOX",           "[Gmail]/Важное",       "[Gmail]/Вся почта",
    "[Gmail]/Корзина", "[Gmail]/Отправленные", "[Gmail]/Помеченные",
    "[Gmail]/Спам"
};

TEST(COMPATIBILITY_TEST, GMAIL_STARTTLS)
{
    // Gmail IMAP server has no IMAP on 143 port.
}

TEST(COMPATIBILITY_TEST, GMAIL_SSL)
{
    try
    {
        auto clientModule = yplatform::find<ymod_imap_client::call>("imap_client");
        auto context = boost::make_shared<TestContext>();

        // Connecting...
        auto ipString = clientModule->connect(context, "imap.gmail.com", 993, true, true).get();
        L_(info) << "Connected to [" << *ipString << "] imap server.";

        // Logging in...
        auto loginOk =
            clientModule->login(context, GMAIL_YAPOPTEST[0], GMAIL_YAPOPTEST[1], true).get();
        EXPECT_TRUE(loginOk) << "Failed to login";

        auto resultFolders = clientModule->load_uidls(context, true).get();
        // L_(info) << "LIST — folders: " << debugString(*resultFolders) << std::endl;
        // EXPECT_TRUE(uidlsHasRequiredFolders(resultFolders, GMAIL_FOLDERS)) << "Failed to load
        // correct folder list";

        clientModule->quit(context, true).get();
    }
    catch (const std::exception& e)
    {
        EXPECT_TRUE(false) << e.what();
    }
}
