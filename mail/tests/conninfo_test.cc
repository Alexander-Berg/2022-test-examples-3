#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <internal/db/conn_info.h>

namespace {

using namespace testing;
using namespace sharpei::db;

TEST(ConnectionInfoTest, allParamsWithoutSslModeToString) {
    auto authInfo = AuthInfo().user("root").password("pwd");
    ConnectionInfo connInfo("mdb666.yandex.ru", 1234, "testdb", authInfo);
    EXPECT_EQ("host=mdb666.yandex.ru port=1234 dbname=testdb user=root password=pwd", connInfo.toString());
}

TEST(ConnectionInfoTest, allParamsWithoutAuthInfoToString) {
    ConnectionInfo connInfo("mdb666.yandex.ru", 1234, "testdb", {});
    EXPECT_EQ("host=mdb666.yandex.ru port=1234 dbname=testdb", connInfo.toString());
}

TEST(ConnectionInfoTest, allParamsToString) {
    auto authInfo = AuthInfo().user("root").password("pwd").sslmode("enable");
    ConnectionInfo connInfo("mdb666.yandex.ru", 1234, "testdb", authInfo);
    EXPECT_EQ("host=mdb666.yandex.ru port=1234 dbname=testdb user=root password=pwd sslmode=enable",
              connInfo.toString());
}

TEST(ConnectionInfoWOHostTest, fillingInAllFields) {
    auto authInfo = AuthInfo().user("root").password("pwd").sslmode("enable");
    auto connInfo = ConnectionInfoWOHost("testdb", 1234, authInfo);
    EXPECT_EQ(connInfo.authInfo.password().value(), "pwd");
    EXPECT_EQ(connInfo.authInfo.sslmode().value(), "enable");
    EXPECT_EQ(connInfo.authInfo.user().value(), "root");
    EXPECT_EQ(connInfo.dbname, "testdb");
    EXPECT_EQ(connInfo.port, 1234u);
}

}  // namespace
