#include <gtest/gtest.h>
#include <session_info.h>
#include <session_pool.h>
#include <ymod_smtpclient/client.h>
#include <ymod_smtpclient/auth.h>

using namespace ymod_smtpclient;
using namespace testing;

inline bool isAuthEqual(const AuthData& a, const AuthData& b) {
    return std::tie(a.mechanism, a.login, a.password, a.token) ==
        std::tie(b.mechanism, b.login, b.password, b.token);
}

using ClientPtr = std::shared_ptr<Client>;

struct SessionPoolTest: public Test {
    using SessionPoolPtr = std::shared_ptr<SessionPool>;    // from Client::Impl
    using Connections = std::map<SessionInfo, SessionPoolPtr>; // from Client::Impl

    Connections connections;
    boost::asio::io_service ios;
    Settings settings;

    SessionPoolTest() {
        // without auth data
        auto point = SmtpPoint::fromString("lmtp://127.0.0.1:1234");
        connections.insert({SessionInfo{point, {}, {}}, SessionPoolPtr()});
        point = SmtpPoint::fromString("smtp://127.0.0.1:5252");
        connections.insert({SessionInfo{point, {}, {}}, SessionPoolPtr()});
        point = SmtpPoint::fromString("smtp://forwards.mail.yandex.net:25");
        connections.insert({SessionInfo{point, {}, {}}, SessionPoolPtr()});
        // with authdata
        point = SmtpPoint::fromString("lmtp://127.0.0.1:1234");
        connections.insert({SessionInfo{point, AuthData::BEST("login", "password"), {}}, SessionPoolPtr()});
        point = SmtpPoint::fromString("mx.yandex.ru");
        connections.insert({SessionInfo{point, AuthData::XOAUTH2("login", "token"), {}}, SessionPoolPtr()});
    }
};

TEST_F(SessionPoolTest, FindWithoutAuthIfExists) {
    auto point = SmtpPoint::fromString("lmtp://127.0.0.1:1234");
    auto info = SessionInfo{point, {}, {}};
    auto it = connections.find(info);

    ASSERT_NE(it, connections.end());
    EXPECT_EQ(info.point, it->first.point);
    EXPECT_FALSE(it->first.authdata.is_initialized());

    point = SmtpPoint::fromString("smtp://forwards.mail.yandex.net:25");
    info = SessionInfo{point, {}, {}};
    it = connections.find(info);

    ASSERT_NE(it, connections.end());
    EXPECT_EQ(info.point, it->first.point);
    EXPECT_FALSE(it->first.authdata.is_initialized());
}

TEST_F(SessionPoolTest, FindWithAuthIfExists) {
    auto point = SmtpPoint::fromString("lmtp://127.0.0.1:1234");
    auto info = SessionInfo{point, AuthData::BEST("login", "password"), {}};
    auto it = connections.find(info);

    ASSERT_NE(it, connections.end());
    EXPECT_EQ(info.point, it->first.point);
    EXPECT_TRUE(isAuthEqual(info.authdata.get(), it->first.authdata.get()));

    point = SmtpPoint::fromString("mx.yandex.ru");
    info = SessionInfo{point, AuthData::XOAUTH2("login", "token"), {}};
    it = connections.find(info);

    ASSERT_NE(it, connections.end());
    EXPECT_EQ(info.point, it->first.point);
    EXPECT_TRUE(isAuthEqual(info.authdata.get(), it->first.authdata.get()));
}

TEST_F(SessionPoolTest, FindWithoutAuthIfNotExists) {
    auto point = SmtpPoint::fromString("lmtp://no-such-host.com");
    auto it = connections.find(SessionInfo{point, {}, {}});

    EXPECT_EQ(it, connections.end());

    point = SmtpPoint::fromString("mx.yandex.ru");
    it = connections.find(SessionInfo{point, {}, {}});

    EXPECT_EQ(it, connections.end());
}

TEST_F(SessionPoolTest, FindWithAuthIfNotExists) {
    auto point = SmtpPoint::fromString("lmtp://127.0.0.1:1234");
    auto it = connections.find(SessionInfo{point, AuthData::BEST("no-such-login", "password"), {}});

    EXPECT_EQ(it, connections.end());

    point = SmtpPoint::fromString("smtp://127.0.0.1:5252");
    it = connections.find(SessionInfo{point, AuthData::LOGIN("login", "password"), {}});

    EXPECT_EQ(it, connections.end());
}

TEST_F(SessionPoolTest, FindWithUseSslIfExists) {
    auto point = SmtpPoint::fromString("smtp://127.0.0.1:7777");
    connections.insert({SessionInfo{point, {}, true}, SessionPoolPtr()});

    ASSERT_NE(connections.find(SessionInfo{point, {}, true}), connections.end());
}

TEST_F(SessionPoolTest, FindWithUseSslIfNotExists) {
    auto point = SmtpPoint::fromString("smtp://127.0.0.1:7777");
    connections.insert({SessionInfo{point, {}, true}, SessionPoolPtr()});
    connections.insert({SessionInfo{point, {}, {}}, SessionPoolPtr()});

    ASSERT_EQ(connections.find(SessionInfo{point, {}, false}), connections.end());
}

TEST_F(SessionPoolTest, FindWithoutUseSslIfExists) {
    auto point = SmtpPoint::fromString("smtp://127.0.0.1:7777");
    connections.insert({SessionInfo{point, {}, {}}, SessionPoolPtr()});

    ASSERT_NE(connections.find(SessionInfo{point, {}, {}}), connections.end());
}

TEST_F(SessionPoolTest, FindWithoutUseSslIfNotExists) {
    auto point = SmtpPoint::fromString("smtp://127.0.0.1:7777");
    connections.insert({SessionInfo{point, {}, true}, SessionPoolPtr()});
    connections.insert({SessionInfo{point, {}, false}, SessionPoolPtr()});

    ASSERT_EQ(connections.find(SessionInfo{point, {}, {}}), connections.end());
}
