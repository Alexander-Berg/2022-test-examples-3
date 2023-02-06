#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <tests/common.hpp>
#include <src/imap_result_debug.hpp>

using namespace ymod_imap_client;

// TEST(CLIENT_TEST, RESOLVE_COMPARE) {
//     try {
//         auto context = boost::make_shared<TestContext>();
//         auto newClientModule = yplatform::find<ymod_imap_client::call>("imap_client");
//         auto oldClientModule = yplatform::find<ymod_imap_client::call>("imap_client_old");

//         auto newIp = newClientModule->connect(context, "imap6-qa.mail.yandex.net", 993, true,
//         true).get(); auto oldIp = oldClientModule->connect(context, "imap6-qa.mail.yandex.net",
//         993, true, true).get();

//         EXPECT_TRUE(*newIp == *oldIp);
//     } catch (const std::exception& e) {
//         EXPECT_TRUE(false) << "Got exception: " << e.what();
//     }
// }

TEST(CLIENT_TEST, RESOLVE_NO_ZERO)
{
    try
    {
        auto context = boost::make_shared<TestContext>();
        auto clientModule = yplatform::find<ymod_imap_client::call>("imap_client");
        auto ip = clientModule->connect(context, "imap.yandex.ru", 993, true, true).get();
        EXPECT_FALSE(*ip == "0.0.0.0");
    }
    catch (const std::exception& e)
    {
        EXPECT_TRUE(false) << "Got exception: " << e.what();
    }
}