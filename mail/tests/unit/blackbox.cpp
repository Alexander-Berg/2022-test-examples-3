#include "http_client_mock.h"

#include <mail/furita/src/blackbox/http_client.h>

#include <gmock/gmock.h>
#include <gtest/gtest.h>

namespace {

using namespace ::testing;
using namespace furita;
using namespace furita::blackbox;

struct TCallbackMock {
    MOCK_METHOD(void, call, ());
};

struct TTestBlackBoxClient : public Test {
    TTestBlackBoxClient()
        : HttpMock(std::make_shared<ClusterClientMock>())
        , Context(new TContext)
        , Client(std::make_shared<TBlackBoxClient>(HttpMock, IoContext))
    {
    }

    std::shared_ptr<ClusterClientMock> HttpMock;
    TContextPtr Context;
    boost::asio::io_context IoContext;
    std::shared_ptr<TBlackBoxClient> Client;
    TCallbackMock CallbackMock;
};


TEST_F(TTestBlackBoxClient, for_empty_input_should_be_no_request) {
    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .Times(0);
    EXPECT_CALL(CallbackMock, call()).Times(1);
    Client->DoRequest(
        Context, {},
        [this](TErrorCode, TBlackBoxResponse){CallbackMock.call();}
    );
    IoContext.run();
}

TEST_F(TTestBlackBoxClient, for_nonempty_input_should_be_requests) {
    const auto okCode = ymod_httpclient::http_error::make_error_code(yhttp::errc::success);
    EXPECT_CALL(*HttpMock, async_run( _, _, _, _))
        .Times(2)
        .WillRepeatedly(InvokeArgument<3>(okCode, yhttp::response{}));
    EXPECT_CALL(CallbackMock, call()).Times(1);

    const std::vector<std::string> emails = {
        "somebody@example.com",
        "nobody@example.com",
    };
    Client->DoRequest(
        Context, TBlackBoxRequest{emails},
        [this](TErrorCode, TBlackBoxResponse){CallbackMock.call();}
    );
    IoContext.run();
}

TEST_F(TTestBlackBoxClient, for_nonempty_input_should_be_correct_request_url) {
    const std::string expectedUrl = "/blackbox?"
        "method=userinfo"
        "&login=somebody\%40example.com"
        "&attributes=1031"
        "&userip=127.0.0.1"
        "&format=json";
    EXPECT_CALL(*HttpMock, async_run(
        _,
        AllOf(
            Field(&yhttp::request::url, StrEq(expectedUrl))
        ),
        _,
        _)
    ).Times(1);

    const std::vector<std::string> emails = {
        "somebody@example.com",
    };
    Client->DoRequest(Context, TBlackBoxRequest{emails}, {});
    IoContext.run();
}

TEST_F(TTestBlackBoxClient, for_valid_response_should_return_answers) {
    const auto okCode = ymod_httpclient::http_error::make_error_code(yhttp::errc::success);
    auto callback = [&](TErrorCode, const TBlackBoxResponse response) {
        CallbackMock.call();
        EXPECT_THAT(response.OrgIds, UnorderedElementsAre("135", "468"));
    };
    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .Times(2)
        .WillOnce(InvokeArgument<3>(okCode, yhttp::response{200, {}, R"({"users":[{"attributes":{"1031":"135"}, "id": "54321"}]})", {}}))
        .WillOnce(InvokeArgument<3>(okCode, yhttp::response{200, {}, R"({"users":[{"attributes":{"1031":"468"}, "id": "54322"}]})", {}}));

    const std::vector<std::string> emails = {
        "somebody@example.com",
        "nobody@example.com",
    };
    EXPECT_CALL(CallbackMock, call()).Times(1);
    Client->DoRequest(Context, TBlackBoxRequest{emails}, callback);
    IoContext.run();
}

TEST_F(TTestBlackBoxClient, for_connect_error_should_return_error) {
    const auto expectedErrorCode{make_error_code(yhttp::errc::connect_error)};
    auto callback = [&](TErrorCode ec, const TBlackBoxResponse response) {
        EXPECT_EQ(ec, expectedErrorCode);
        EXPECT_THAT(response.OrgIds, IsEmpty());
        CallbackMock.call();
    };
    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .Times(1)
        .WillOnce(InvokeArgument<3>(expectedErrorCode, yhttp::response{}));
    EXPECT_CALL(CallbackMock, call()).Times(1);
    Client->DoRequest(Context, TBlackBoxRequest{{"somebody@example.com"}}, callback);
    IoContext.run();
}

TEST_F(TTestBlackBoxClient, for_any_empty_response_should_return_empty_answer) {
    const auto okCode = make_error_code(yhttp::errc::success);
    auto callback = [&](TErrorCode ec, const TBlackBoxResponse response) {
        EXPECT_EQ(ec, EError::NotFound);
        EXPECT_THAT(response.OrgIds, IsEmpty());
        EXPECT_THAT(response.ErrorMessage, IsEmpty());
        CallbackMock.call();
    };
    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .Times(2)
        .WillOnce(InvokeArgument<3>(okCode, yhttp::response{200, {}, R"({"users":[{"attributes":{"1031":"135"}, "id": "54321"}]})", {}}))
        .WillOnce(InvokeArgument<3>(okCode, yhttp::response{200, {}, R"({"users":[]})", {}}));
    EXPECT_CALL(CallbackMock, call()).Times(1);

    const std::vector<std::string> emails = {
        "somebody@example.com",
        "nobody@example.com",
    };
    Client->DoRequest(Context, TBlackBoxRequest{emails}, callback);
    IoContext.run();
}

TEST_F(TTestBlackBoxClient, for_invalid_response_should_return_empty_answer_and_error) {
    const auto okCode = make_error_code(yhttp::errc::success);
    auto callback = [&](TErrorCode ec, const TBlackBoxResponse response) {
        EXPECT_EQ(ec, EError::InvalidResponse);
        EXPECT_THAT(response.OrgIds, IsEmpty());
        EXPECT_EQ(response.ErrorMessage, "No such node (1031)");
        CallbackMock.call();
    };
    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(okCode, yhttp::response{200, {}, R"({"users":[{"attributes":{}}]})", {}}));
    EXPECT_CALL(CallbackMock, call()).Times(1);

    const std::vector<std::string> emails = {
        "somebody@example.com",
    };
    Client->DoRequest(Context, TBlackBoxRequest{emails}, callback);
    IoContext.run();
}

TEST_F(TTestBlackBoxClient, for_empty_org_id_should_return_empty_answer_and_error) {
    const auto okCode = ymod_httpclient::http_error::make_error_code(yhttp::errc::success);
    auto callback = [&](TErrorCode ec, const TBlackBoxResponse response) {
        EXPECT_EQ(ec, EError::EmptyOrgId);
        EXPECT_THAT(response.OrgIds, IsEmpty());
        CallbackMock.call();
    };
    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(okCode, yhttp::response{200, {}, R"({"users":[{"attributes":{"1031": ""}}]})", {}}));
    EXPECT_CALL(CallbackMock, call()).Times(1);

    const std::vector<std::string> emails = {
        "somebody@example.com",
    };
    Client->DoRequest(Context, TBlackBoxRequest{emails}, callback);
    IoContext.run();
}

}
