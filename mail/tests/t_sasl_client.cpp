#include <gtest/gtest.h>

#include <ymod_smtpclient/auth.h>
#include <sasl/client.h>

using namespace ymod_smtpclient;
using namespace ymod_smtpclient::sasl;
using namespace testing;

template <Mechanism M>
struct ClientTest: public Test {
    Command command;
    ClientEngine engine;

    ClientTest() {
        AuthData auth;
        auth.mechanism = M;
        auth.login = "login";
        auth.password = "password";
        auth.token = "token";
        engine.reset(auth);
    }
};

template <Mechanism M>
struct ClientWithLongCredentialsTest: public Test {
    Command command;
    ClientEngine engine;

    ClientWithLongCredentialsTest(): engine(20) {
        AuthData auth;
        auth.mechanism = M;
        auth.login = "loginloginloginlogin"; // 20 symbols
        auth.password = "passwordpasswordpass"; // 20 symbols
        auth.token = "tokentokentokentoken"; // 20 symbols
        engine.reset(auth);
    }
};

using LoginClientTest = ClientTest<Mechanism::Login>;
using PlainClientTest = ClientTest<Mechanism::Plain>;
using OauthClientTest = ClientTest<Mechanism::Xoauth2>;

using PlainClientWithLongCredentialsTest = ClientWithLongCredentialsTest<Mechanism::Plain>;
using OauthClientWithLongCredentialsTest = ClientWithLongCredentialsTest<Mechanism::Xoauth2>;


// test login auth

TEST_F(LoginClientTest, AuthorizationSuccess) {
    MultiLineResponse response;

    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "AUTH LOGIN\r\n");

    response.replyCode = 334;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "bG9naW4=\r\n"); // login

    response.replyCode = 334;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "cGFzc3dvcmQ=\r\n"); // password

    response.replyCode = 235;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Done);
    EXPECT_TRUE(command.name.empty() && command.args.empty());

    // check that it stays in that state
    response.replyCode = 123;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Done);
    EXPECT_TRUE(command.name.empty() && command.args.empty());
}

TEST_F(LoginClientTest, AuthorizationUnexpectedResponse) {
    MultiLineResponse response;

    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "AUTH LOGIN\r\n");

    response.replyCode = 334;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "bG9naW4=\r\n"); // login

    response.replyCode = 550;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Form);
    EXPECT_TRUE(command.name.empty() && command.args.empty()); // password

    // check that it stays in that state
    response.replyCode = 334;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Form);
    EXPECT_TRUE(command.name.empty() && command.args.empty());
}

TEST_F(LoginClientTest, AuthorizationInvalidProtocol) {
    MultiLineResponse response;

    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "AUTH LOGIN\r\n");

    response.replyCode = 334;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "bG9naW4=\r\n"); // login

    response.replyCode = 334;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "cGFzc3dvcmQ=\r\n"); // password

    response.replyCode = 334;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Form);
    EXPECT_TRUE(command.name.empty() && command.args.empty());

    // check that it stays in that state
    response.replyCode = 235;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Form);
    EXPECT_TRUE(command.name.empty() && command.args.empty());
}

TEST_F(LoginClientTest, ResetCredentials) {
    MultiLineResponse response;

    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "AUTH LOGIN\r\n");

    AuthData auth;
    auth.login = "login_2";
    auth.password = "password_2";
    auth.mechanism = Mechanism::Plain;
    engine.reset(auth);

    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "AUTH PLAIN bG9naW5fMgBsb2dpbl8yAHBhc3N3b3JkXzI=\r\n");

    auth.token = "token_2";
    auth.mechanism = Mechanism::Xoauth2;
    engine.reset(auth);

    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "AUTH XOAUTH2 dXNlcj1sb2dpbl8yAWF1dGg9QmVhcmVyIHRva2VuXzIBAQ==\r\n");
}

// test plain auth

TEST_F(PlainClientTest, AuthorizationSuccess) {
    MultiLineResponse response;

    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "AUTH PLAIN bG9naW4AbG9naW4AcGFzc3dvcmQ=\r\n");

    response.replyCode = 235;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Done);
    EXPECT_TRUE(command.name.empty() && command.args.empty());

    // check that it stays in that state
    response.replyCode = 334;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Done);
    EXPECT_TRUE(command.name.empty() && command.args.empty());
}

TEST_F(PlainClientWithLongCredentialsTest, AuthorizationSuccess) {
    MultiLineResponse response;

    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "AUTH PLAIN\r\n");

    response.replyCode = 334;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(),
        "bG9naW5sb2dpbmxvZ2lubG9naW4AbG9naW5sb2dpbmxvZ2lubG9naW4AcGFzc3dvcmRwYXNzd29yZHBhc3M=\r\n");

    response.replyCode = 235;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Done);
    EXPECT_TRUE(command.name.empty() && command.args.empty());

    // check that it stays in that state
    response.replyCode = 334;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Done);
    EXPECT_TRUE(command.name.empty() && command.args.empty());
}

TEST_F(PlainClientTest, AuthorizationInvalidProtocol) {
    MultiLineResponse response;

    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "AUTH PLAIN bG9naW4AbG9naW4AcGFzc3dvcmQ=\r\n");

    response.replyCode = 334;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Form);
    EXPECT_TRUE(command.name.empty() && command.args.empty());

    // check that it stays in that state
    response.replyCode = 235;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Form);
    EXPECT_TRUE(command.name.empty() && command.args.empty());
}

TEST_F(PlainClientWithLongCredentialsTest, AuthorizationUnexpectedResponse) {
    MultiLineResponse response;

    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "AUTH PLAIN\r\n");

    response.replyCode = 235;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Form);
    EXPECT_TRUE(command.name.empty() && command.args.empty());

    // check that it stays in that state
    response.replyCode = 235;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Form);
    EXPECT_TRUE(command.name.empty() && command.args.empty());
}

// test oauth auth

TEST_F(OauthClientTest, AuthorizationSuccess) {
    MultiLineResponse response;

    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "AUTH XOAUTH2 dXNlcj1sb2dpbgFhdXRoPUJlYXJlciB0b2tlbgEB\r\n");

    response.replyCode = 235;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Done);
    EXPECT_TRUE(command.name.empty() && command.args.empty());

    // check that it stays in that state
    response.replyCode = 334;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Done);
    EXPECT_TRUE(command.name.empty() && command.args.empty());
}

TEST_F(OauthClientWithLongCredentialsTest, AuthorizationSuccess) {
    MultiLineResponse response;

    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "AUTH XOAUTH2\r\n");

    response.replyCode = 334;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(),
        "dXNlcj1sb2dpbmxvZ2lubG9naW5sb2dpbgFhdXRoPUJlYXJlciB0b2tlbnRva2VudG9rZW50b2tlbgEB\r\n");

    response.replyCode = 535;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Done);
    EXPECT_TRUE(command.name.empty() && command.args.empty());

    // check that it stays in that state
    response.replyCode = 235;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Done);
    EXPECT_TRUE(command.name.empty() && command.args.empty());
}

TEST_F(OauthClientTest, AuthorizationInvalidProtocol) {
    MultiLineResponse response;

    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "AUTH XOAUTH2 dXNlcj1sb2dpbgFhdXRoPUJlYXJlciB0b2tlbgEB\r\n");

    response.replyCode = 334;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Form);
    EXPECT_TRUE(command.name.empty() && command.args.empty());

    // check that it stays in that state
    response.replyCode = 235;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Form);
    EXPECT_TRUE(command.name.empty() && command.args.empty());
}

TEST_F(OauthClientWithLongCredentialsTest, AuthorizationUnexpectedResponse) {
    MultiLineResponse response;

    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::More);
    EXPECT_EQ(command.str(), "AUTH XOAUTH2\r\n");

    response.replyCode = 235;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Form);
    EXPECT_TRUE(command.name.empty() && command.args.empty());

    // check that it stays in that state
    response.replyCode = 334;
    ASSERT_EQ(engine.next(response, command), ClientEngine::Status::Form);
    EXPECT_TRUE(command.name.empty() && command.args.empty());
}
