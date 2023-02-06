#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <tests/common.hpp>
#include <src/imap_result_debug.hpp>

static std::vector<std::string> IMAP_PG = { "imap.pg", "testqa" };
static std::set<std::string> IMAP_PG_FOLDERS = { "Drafts", "INBOX", "Outbox",
                                                 "Sent",   "Spam",  "Trash" };

TEST(COMPATIBILITY_TEST, YANDEX_NO_SSL)
{
    try
    {
        auto clientModule = yplatform::find<ymod_imap_client::call>("imap_client");
        auto context = boost::make_shared<TestContext>();

        // Connecting...
        auto ipString = clientModule->connect(context, "imap.yandex.ru", 143, false, true).get();
        L_(info) << "Connected to [" << *ipString << "] imap server.";

        // Logging in...
        auto loginOk = clientModule->login(context, IMAP_PG[0], IMAP_PG[1], true).get();
        EXPECT_TRUE(loginOk) << "Failed to login";

        // Loading folders...
        auto resultFolders = clientModule->load_uidls(context, true).get();
        EXPECT_TRUE(uidlsHasRequiredFolders(resultFolders, IMAP_PG_FOLDERS))
            << "Failed to load correct folder list";

        clientModule->quit(context, true).get();
    }
    catch (const std::exception& e)
    {
        EXPECT_TRUE(false) << e.what();
    }
}

TEST(COMPATIBILITY_TEST, YANDEX_SSL)
{
    try
    {
        auto clientModule = yplatform::find<ymod_imap_client::call>("imap_client");
        auto context = boost::make_shared<TestContext>();

        // Connecting...
        auto ipString = clientModule->connect(context, "imap.yandex.ru", 993, true, true).get();
        L_(info) << "Connected to [" << *ipString << "] imap server.";

        // Logging in...
        auto loginOk = clientModule->login(context, IMAP_PG[0], IMAP_PG[1], true).get();
        EXPECT_TRUE(loginOk) << "Failed to login";

        auto resultFolders = clientModule->load_uidls(context, true).get();
        EXPECT_TRUE(uidlsHasRequiredFolders(resultFolders, IMAP_PG_FOLDERS))
            << "Failed to load correct folder list";

        clientModule->quit(context, true).get();
    }
    catch (const std::exception& e)
    {
        EXPECT_TRUE(false) << e.what();
    }
}
