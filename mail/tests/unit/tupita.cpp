#include "http_client_mock.h"
#include "tvm_client_mock.h"

#include <mail/furita/src/tupita/http_client.h>

#include <gtest/gtest.h>


namespace {

using namespace ::testing;
using namespace furita;
using namespace furita::tupita;

struct TTestTupitaClient : public Test {
    TTestTupitaClient()
        : HttpMock(std::make_shared<ClusterClientMock>())
        , Context(new TContext)
        , TvmMock(std::make_shared<TTvmClientMock>())
        , Client(std::make_shared<TTupitaClientImpl>(HttpMock, TvmMock, IoContext))
    {
        ON_CALL(*TvmMock, get_service_ticket)
            .WillByDefault(DoAll(
                SetArgReferee<1>("TICKET"),
                Return(TErrorCode{})
            ));
    }

    void TestResponse(std::function<void(TErrorCode, const TTupitaResponse&)> cb) {
        Client->DoRequest(Context, {{json_value{}}, {}, {}}, cb);
    }

    std::shared_ptr<ClusterClientMock> HttpMock;
    TContextPtr Context;
    boost::asio::io_context IoContext;
    std::shared_ptr<TTvmClientMock> TvmMock;
    std::shared_ptr<TTupitaClientImpl> Client;
};

struct TTestRequestBody : public TTestTupitaClient, 
    public WithParamInterface<std::tuple<TJsonVector /* conditions */, std::string /* expected */>>
{
};

struct TTestRequestUrl : public TTestTupitaClient, 
    public WithParamInterface<std::tuple<
        std::optional<std::string> /* domain */,
        std::optional<std::string> /* orgId */,
        std::string /* expectedUrl */ >>
{
};

TEST_F(TTestTupitaClient, for_empty_conditions_should_be_no_request) {
    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .Times(0);
    Client->DoRequest(Context, {}, [](TErrorCode, TTupitaResponse){});
    IoContext.run();
}

TEST_P(TTestRequestBody, for_nonempty_conditions_should_request_with_expected_body_and_header) {
    const auto& [conditions, expectedRequest] = GetParam();
    const std::string expectedHeaders = "X-Ya-Service-Ticket: TICKET\r\n";

    EXPECT_CALL(*HttpMock, async_run(
        _,
        AllOf(
            Field(&yhttp::request::body, Pointee(StrEq(expectedRequest))),
            Field(&yhttp::request::headers, VariantWith<std::string>(expectedHeaders))
        ),
        _,
        _)
    ).Times(1);
    Client->DoRequest(Context, {conditions, {}, {}}, {});
    IoContext.run();
}

TEST_P(TTestRequestUrl, for_given_extra_parameters_url_should_contain_them) {
    const auto& [domain, orgId, expectedUrl] = GetParam();

    EXPECT_CALL(*HttpMock, async_run(
        _,
        Field(&yhttp::request::url, StrEq(expectedUrl)),
        _,
        _)
    ).Times(1);
    Client->DoRequest(Context, {{json_value{}}, domain, orgId}, {});
    IoContext.run();
}

json_value PrepareCondition(const std::string& conditionStr) {
    json_value condition;
    auto error = condition.parse(conditionStr);
    EXPECT_FALSE(error);
    return condition;
}

const std::string conditionStr1 = R"({"header:x-spam-flag": { "$exists": true }})";
const std::string conditionStr2 = R"({"$and": [{"from": "friend@mail.ru"}, {"subject": {"$contains": "Fw:"}}]})";
const std::string expected1 = R"({"conditions":[{"header:x-spam-flag":{"$exists":true}}]})";
const std::string expected2 = R"({"conditions":[{"header:x-spam-flag":{"$exists":true}},)"
                        R"({"$and":[{"from":"friend@mail.ru"},{"subject":{"$contains":"Fw:"}}]}]})";

INSTANTIATE_TEST_SUITE_P(test_request_with_conditions, TTestRequestBody,
    Values(
        std::make_tuple<TJsonVector, std::string>(
            {PrepareCondition(conditionStr1)}, std::string{expected1}
        ),
        std::make_tuple<TJsonVector, std::string>(
            {PrepareCondition(conditionStr1), PrepareCondition(conditionStr2)}, std::string{expected2}
        )
    )
);

auto makeTestUrlValues = [](std::optional<std::string>&& domain, std::optional<std::string>&& orgId, std::string&& expected) {
    return std::make_tuple<std::optional<std::string>, std::optional<std::string>, std::string>(
        std::move(domain), std::move(orgId), std::move(expected)
    );
};

INSTANTIATE_TEST_SUITE_P(test_url_params_combos, TTestRequestUrl,
    Values(
        makeTestUrlValues({}, {}, "/api/mail/conditions/convert?domain=&org_id="),
        makeTestUrlValues("example.com", {}, "/api/mail/conditions/convert?domain=example.com&org_id="),
        makeTestUrlValues({}, "xyz741", "/api/mail/conditions/convert?domain=&org_id=xyz741"),
        makeTestUrlValues("example.com", "xyz741", "/api/mail/conditions/convert?domain=example.com&org_id=xyz741")
    )
);

const std::string responseOk = R"(
    {
        "status": "ok",
        "conditions": [
            {"query": "headers:x_spam_flag AND NOT hid:0"},
            {"query": "hdr_from_normalized:xxx AND NOT hid:0"}
        ]
    }
)";

const std::string responseNotOk = R"(
    {
        "status": "error",
        "error": {
            "error_type": "parse_error",
            "conditions": [
                {
                    "condition": {"header:x-spam-flag": { "$exists": true }},
                    "message": "Unknown header x-spam-flag",
                    "orig_index": 1
                },
                {
                    "condition": {"header:x-hacker-flag": { "$exists": true }},
                    "message": "Unknown header x-hacker-flag",
                    "orig_index": 4
                }
            ]
        }
    }
)";

TEST_F(TTestTupitaClient, for_correct_response_should_parse_ok) {
    const auto okCode = ymod_httpclient::http_error::make_error_code(yhttp::errc::success);
    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(
            okCode,
            yhttp::response{200, {}, responseOk, {}}
        ));
    TestResponse([&](TErrorCode ec, const TTupitaResponse& response) {
        EXPECT_EQ(ec, EError::Ok);
        EXPECT_THAT(response.Queries, ElementsAre(
            "headers:x_spam_flag AND NOT hid:0",
            "hdr_from_normalized:xxx AND NOT hid:0"
        ));
        EXPECT_THAT(response.ErrorMessage, IsEmpty());
    });
    IoContext.run();
}

TEST_F(TTestTupitaClient, for_error_400_should_be_parse_error_and_pass_message) {
    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(
            ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
            yhttp::response{400, {}, responseNotOk, {}}
        ));
    TestResponse([](TErrorCode ec, const TTupitaResponse& response) {
        EXPECT_EQ(ec, EError::RequestParseError);
        EXPECT_THAT(response.Queries, IsEmpty());
        EXPECT_EQ(
            response.ErrorMessage,
            "parse_error: [1] Unknown header x-spam-flag [4] Unknown header x-hacker-flag"
        );
    });
    IoContext.run();
}

TEST_F(TTestTupitaClient, for_incorrect_json_should_be_invalid_response) {
    const auto errorCode = ymod_httpclient::http_error::make_error_code(yhttp::errc::unknown_error);

    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(
            errorCode,
            yhttp::response{404, {}, "Error 404", {}}
        ));
    TestResponse([&](TErrorCode ec, const TTupitaResponse& response) {
        EXPECT_EQ(ec, errorCode);
        EXPECT_THAT(response.Queries, IsEmpty());
    });
    IoContext.run();
}

TEST_F(TTestTupitaClient, for_valid_but_empty_json_should_be_invalid_response) {
    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(
            ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
            yhttp::response{200, {}, "{}", {}}
        ));
    TestResponse([](TErrorCode ec, const TTupitaResponse& response) {
        EXPECT_EQ(ec, EError::InvalidResponse);
        EXPECT_THAT(response.Queries, IsEmpty());
    });
    IoContext.run();
}

TEST_F(TTestTupitaClient, for_empty_queries_should_be_invalid_response) {
    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(
            ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
            yhttp::response{200, {}, R"({"status": "ok", "conditions": []})", {}}
        ));
    TestResponse([](TErrorCode ec, const TTupitaResponse& response) {
        EXPECT_EQ(ec, EError::InvalidResponse);
        EXPECT_THAT(response.Queries, IsEmpty());
    });
    IoContext.run();
}

TEST_F(TTestTupitaClient, for_sequence_of_calls_should_be_correct_codes) {
    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(
            ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
            yhttp::response{200, {}, responseOk, {}}
        ));
    TestResponse([](TErrorCode ec, const TTupitaResponse&) {
        EXPECT_EQ(ec, EError::Ok);
    });

    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(
            ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
            yhttp::response{200, {}, "{}", {}}
        ));
    TestResponse([](TErrorCode ec, const TTupitaResponse& response) {
        EXPECT_EQ(ec, EError::InvalidResponse);
        EXPECT_THAT(response.Queries, IsEmpty());
    });

    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(
            ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
            yhttp::response{400, {}, responseNotOk, {}}
        ));
    TestResponse([](TErrorCode ec, const TTupitaResponse& response) {
        EXPECT_EQ(ec, EError::RequestParseError);
        EXPECT_THAT(response.Queries, IsEmpty());
    });

    EXPECT_CALL(*HttpMock, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(
            ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
            yhttp::response{200, {}, responseOk, {}}
        ));
    TestResponse([](TErrorCode ec, const TTupitaResponse& response) {
        EXPECT_EQ(ec, EError::Ok);
        EXPECT_THAT(response.Queries, SizeIs(2));
        EXPECT_THAT(response.ErrorMessage, IsEmpty());
    });
    IoContext.run();
}

}
