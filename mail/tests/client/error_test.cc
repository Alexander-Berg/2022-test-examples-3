#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <tests/common.hpp>
#include <src/imap_result_debug.hpp>
#include <src/client_session.h>

using namespace ymod_imap_client;

TEST(ERROR_TEST, YANDEX_WRONG_PORT_TEST)
{
    auto debugCounter = ClientSession::debugSessionCounter();
    try
    {
        auto imapClient = ImapClient::connect(
            boost::make_shared<TestContext>(), "imap.yandex.ru", 497, false, true);
    }
    catch (const std::exception& e)
    {
    }
    EXPECT_TRUE(debugCounter == ClientSession::debugSessionCounter())
        << "SESSION LEAKAGE!!!111 Now:" << ClientSession::debugSessionCounter();
}

TEST(ERROR_TEST, QIP_TEST)
{
    auto debugCounter = ClientSession::debugSessionCounter();
    try
    {
        auto imapClient =
            ImapClient::connect(boost::make_shared<TestContext>(), "imap.qip.com", 993, true, true);

        auto& capa = *imapClient->capability().get();
        L_(info) << "CAPABILITY — OK: " << debugString(capa) << std::endl;
    }
    catch (const std::exception& e)
    {
    }
    EXPECT_TRUE(debugCounter == ClientSession::debugSessionCounter())
        << "SESSION LEAKAGE!!!111 Now:" << ClientSession::debugSessionCounter();
}

TEST(ERROR_TEST, QIP_TEST_PASS)
{
    auto debugCounter = ClientSession::debugSessionCounter();
    try
    {
        auto imapClient =
            ImapClient::connect(boost::make_shared<TestContext>(), "imap.qip.com", 993, true, true);
    }
    catch (const std::exception& e)
    {
    }
    EXPECT_TRUE(debugCounter == ClientSession::debugSessionCounter())
        << "SESSION LEAKAGE!!!111 Now:" << ClientSession::debugSessionCounter();
}

TEST(ERROR_TEST, QIP_TEST_PASS_OLD)
{
    auto debugCounter = ClientSession::debugSessionCounter();
    try
    {
        auto clientModule = yplatform::find<ymod_imap_client::call>("imap_client");
        auto context = boost::make_shared<TestContext>();

        // Connecting...
        auto ipString = clientModule->connect(context, "imap.qip.com", 993, true, true).get();
    }
    catch (const std::exception& e)
    {
    }
    EXPECT_TRUE(debugCounter == ClientSession::debugSessionCounter())
        << "SESSION LEAKAGE!!!111 Now:" << ClientSession::debugSessionCounter();
}

TEST(ERROR_TEST, YANDEX_WRONG_PORT_OLD)
{
    auto debugCounter = ClientSession::debugSessionCounter();
    try
    {
        auto clientModule = yplatform::find<ymod_imap_client::call>("imap_client");
        auto context = boost::make_shared<TestContext>();

        // Connecting...
        auto ipString = clientModule->connect(context, "imap.yandex.ru", 541, false, true).get();
    }
    catch (const std::exception& e)
    {
    }
    EXPECT_TRUE(debugCounter == ClientSession::debugSessionCounter())
        << "SESSION LEAKAGE!!!111 Now:" << ClientSession::debugSessionCounter();
}