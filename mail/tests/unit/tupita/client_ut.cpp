#include <mail/notsolitesrv/src/tupita/client.h>

#include <mail/notsolitesrv/tests/unit/mocks/cluster_call.h>
#include <mail/notsolitesrv/tests/unit/util/tupita.h>
#include <mail/notsolitesrv/tests/unit/util/ymod_httpclient.h>

#include <gtest/gtest.h>

namespace {

using namespace testing;

using NNotSoLiteSrv::EError;
using NNotSoLiteSrv::make_error_code;
using NNotSoLiteSrv::NTupita::TCheckCallback;
using NNotSoLiteSrv::NTupita::TCheckResult;
using NNotSoLiteSrv::NTupita::TTupitaCheckRequest;
using NNotSoLiteSrv::NTupita::TTupitaCheckResponse;
using NNotSoLiteSrv::NTupita::TTupitaClient;
using NNotSoLiteSrv::NTupita::TTupitaMessage;
using NNotSoLiteSrv::NTupita::TTupitaMessageLabel;
using NNotSoLiteSrv::NTupita::TTupitaMessageLabelType;
using NNotSoLiteSrv::NTupita::TTupitaQuery;
using NNotSoLiteSrv::NTupita::TTupitaUser;
using NNotSoLiteSrv::NTupita::TTupitaUserWithMatchedQueries;
using NNotSoLiteSrv::TConfigPtr;
using NNotSoLiteSrv::TContext;
using NNotSoLiteSrv::TContextPtr;
using NNotSoLiteSrv::THttpRequest;
using NNotSoLiteSrv::THttpResponse;
using NNotSoLiteSrv::TUid;

struct TTestTupitaClient : Test {
    TTupitaCheckRequest MakeTupitaCheckRequest() const {
        return {
            .Message {
                .Subject{"Subject"},
                .From{{"local0", "domain0.net", "DisplayName0"}},
                .To{{"local1", "domain1.net", "DisplayName1"}},
                .Cc{{{"local2", "domain2.net", "DisplayName2"}}},
                .Stid{"Stid"},
                .Spam = false,
                .Types{0, 1, 2},
                .AttachmentsCount = 2,
                .LabelsInfo{{{
                    "label0",
                    {
                        .Name{"Name"},
                        .IsSystem = false,
                        .IsUser = true,
                        .Type{.Title{"Title"}}
                    }
                }}},
                .Firstline{"Firstline"}
            },
            .Users{{
                .Uid = 1,
                .Queries {
                    {.Id{"1"}, .Query{"Query1"}, .Stop = false},
                    {.Id{"2"}, .Query{"Query2"}, .Stop = false}
                },
                .Spam = false
            }}
        };
    }

    std::string MakeTupitaCheckRequestBody() const {
        return
            R"({"message":{"subject":"Subject","from":[{"local":"local0","domain":"domain0.net",)"
            R"("display_name":"DisplayName0"}],"to":[{"local":"local1","domain":"domain1.net",)"
            R"("display_name":"DisplayName1"}],"cc":[{"local":"local2","domain":"domain2.net",)"
            R"("display_name":"DisplayName2"}],"stid":"Stid","spam":false,"types":[0,1,2],)"
            R"("attachmentsCount":2,"labelsInfo":{"label0":{"name":"Name","isSystem":false,"isUser":true,)"
            R"("type":{"title":"Title"}}},"firstline":"Firstline"},"users":[{"uid":1,"queries":[{"id":"1",)"
            R"("query":"Query1","stop":false},{"id":"2","query":"Query2","stop":false}],"spam":false}]})";
    }

    TTupitaCheckResponse MakeTupitaCheckResponse() const {
        return {{{{"1", "2"}}}};
    }

    std::string MakeTupitaCheckResponseBody() const {
        return R"({
            "result": [
                {
                    "uid": 1,
                    "matched_queries": ["1", "2"]
                }
            ]
        })";
    }

    void TestCheck(TCheckCallback callback) {
        const TTupitaClient tupitaClient{Context, ClusterCall};
        tupitaClient.Check(IoContext, Uid, RequestId, MakeTupitaCheckRequest(), std::move(callback));
        IoContext.run();
    }

    const std::string ConnectionId{"ConnectionId"};
    const std::string EnvelopeId{"EnvelopeId"};
    const TContextPtr Context{std::make_shared<TContext>(TConfigPtr{}, ConnectionId, EnvelopeId)};
    const std::shared_ptr<StrictMock<TClusterCallMock>> ClusterCall{std::make_shared<StrictMock<
        TClusterCallMock>>()};
    const TUid Uid{1};
    const std::string RequestId{"RequestId"};
    boost::asio::io_context IoContext;
    const THttpRequest RequestToTupita{THttpRequest::POST("/check?uid=1&reqid=" + RequestId,
        MakeTupitaCheckRequestBody())};
    const int StatusOk{200};
    const int StatusNonRetryable{404};
    const int StatusRetryable{500};
};

TEST_F(TTestTupitaClient, for_cluster_call_error_check_must_return_error) {
    const auto expectedErrorCode{ymod_httpclient::http_error::make_error_code(yhttp::errc::connect_error)};
    EXPECT_CALL(*ClusterCall, async_run(_, RequestToTupita, _, _)).
        WillOnce(InvokeArgument<3>(expectedErrorCode, THttpResponse{}));
    TestCheck([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(expectedErrorCode, errorCode);
        EXPECT_EQ((TCheckResult{}), result);
    });
}

TEST_F(TTestTupitaClient, for_tupita_nonretryable_error_check_must_return_error) {
    EXPECT_CALL(*ClusterCall, async_run(_, RequestToTupita, _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{StatusNonRetryable, {}, {}, {}}));
    TestCheck([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(make_error_code(EError::HttpNonRetryableStatus), errorCode);
        EXPECT_EQ((TCheckResult{}), result);
    });
}

TEST_F(TTestTupitaClient, for_tupita_retries_exceeded_error_check_must_return_error) {
    EXPECT_CALL(*ClusterCall, async_run(_, RequestToTupita, _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{StatusRetryable, {}, {}, {}}));
    TestCheck([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(make_error_code(EError::HttpRetriesExceeded), errorCode);
        EXPECT_EQ((TCheckResult{}), result);
    });
}

TEST_F(TTestTupitaClient, for_tupita_response_parse_error_check_must_return_error) {
    EXPECT_CALL(*ClusterCall, async_run(_, RequestToTupita, _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{StatusOk, {}, {}, {}}));
    TestCheck([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(make_error_code(EError::TupitaResponseParseError), errorCode);
        EXPECT_EQ((TCheckResult{}), result);
    });
}

TEST_F(TTestTupitaClient, for_correct_tupita_response_check_must_return_parsed_response) {
    EXPECT_CALL(*ClusterCall, async_run(_, RequestToTupita, _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{StatusOk, {}, MakeTupitaCheckResponseBody(), {}}));
    TestCheck([&](auto errorCode, auto result) {
        ASSERT_FALSE(errorCode);
        EXPECT_EQ((TCheckResult{MakeTupitaCheckResponse()}), result);
    });
}

}
