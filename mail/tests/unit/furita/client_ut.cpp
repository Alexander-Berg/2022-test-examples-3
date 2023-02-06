#include <mail/notsolitesrv/src/furita/client.h>

#include <mail/notsolitesrv/tests/unit/fakes/context.h>
#include <mail/notsolitesrv/tests/unit/mocks/cluster_call.h>
#include <mail/notsolitesrv/tests/unit/mocks/ymod_tvm.h>
#include <mail/notsolitesrv/tests/unit/util/furita.h>
#include <mail/notsolitesrv/tests/unit/util/ymod_httpclient.h>

#include <gtest/gtest.h>

#include <memory>
#include <string>
#include <utility>

namespace {

using namespace testing;

using NNotSoLiteSrv::EError;
using NNotSoLiteSrv::make_error_code;
using NNotSoLiteSrv::NFurita::TFuritaClient;
using NNotSoLiteSrv::NFurita::TFuritaGetResponse;
using NNotSoLiteSrv::NFurita::TFuritaListResponse;
using NNotSoLiteSrv::NFurita::TGetCallback;
using NNotSoLiteSrv::NFurita::TGetResult;
using NNotSoLiteSrv::NFurita::TListCallback;
using NNotSoLiteSrv::NFurita::TListResult;
using NNotSoLiteSrv::TContextPtr;
using NNotSoLiteSrv::THttpRequest;
using NNotSoLiteSrv::THttpResponse;
using NNotSoLiteSrv::TOrgId;
using NNotSoLiteSrv::TUid;

const int STATUS_OK{200};
const int STATUS_NONRETRYABLE{400};
const int STATUS_ORG_NOT_FOUND{404};
const int STATUS_RETRYABLE{500};

struct TTestFuritaClient : Test {
    TContextPtr MakeContext(bool useTvm = true) const {
        return GetContext({{"furita_use_tvm", std::to_string(useTvm)}});
    }

    THttpRequest MakeFuritaListHttpRequest() const {
        return THttpRequest::GET("/api/list.json?uid=1&detailed=1");
    }

    std::string MakeFuritaListResponseBody() const {
        return R"({
            "rules": [
                {
                    "id": "id",
                    "priority": 1,
                    "query": "query",
                    "enabled": true,
                    "stop": false,
                    "actions": [
                        {
                            "verified": true,
                            "parameter": "parameter",
                            "type": "type"
                        }
                    ]
                }
            ]
        })";
    }

    TFuritaListResponse MakeFuritaListResponse() const {
        return {{{.Id{"id"}, .Priority = 1, .Query{"query"}, .Enabled = true, .Stop = false,
            .Actions{{.Verified = true, .Parameter{"parameter"}, .Type{"type"}}}}}};
    }

    void TestList(TListCallback callback) {
        const TFuritaClient furitaClient{MakeContext(), ClusterCall, TvmModule};
        furitaClient.List(IoContext, TUid{1}, std::move(callback));
        IoContext.run();
    }

    THttpRequest MakeFuritaGetHttpRequest(bool useTvm = true) const {
        const std::string url{"/v1/domain/rules/get?orgid=4"};
        return useTvm ? THttpRequest::GET(url, "X-Ya-Service-Ticket: TvmServiceTicket\r\n") :
            THttpRequest::GET(url);
    }

    std::string MakeFuritaGetResponseBody() const {
        return R"({
            "rules": [
                {
                    "terminal": false,
                    "scope": {"direction": "inbound"},
                    "condition_query": "condition_query0",
                    "actions": [
                        {
                            "action": "forward",
                            "data": {
                                "email": "local00@domain00.ru"
                            }
                        }, {
                            "action": "duplicate",
                            "data": {
                                "email": "local01@domain01.ru"
                            }
                        }
                    ]
                }, {
                    "terminal": true,
                    "scope": {"direction": "inbound"},
                    "condition_query": "condition_query1",
                    "actions": [
                        {
                            "action": "forward",
                            "data": {
                                "email": "local10@domain10.ru"
                            }
                        }, {
                            "action": "duplicate",
                            "data": {
                                "email": "local11@domain11.ru"
                            }
                        }
                    ]
                }
            ],
            "revision": 1
        })";
    }

    TFuritaGetResponse MakeFuritaGetResponse() const {
        return {
            .Rules = {
                {
                    .Terminal = false,
                    .Scope{.Direction{"inbound"}},
                    .ConditionQuery{"condition_query0"},
                    .Actions {
                        {.Action{"forward"}, .Data{{.Email{"local00@domain00.ru"}}}},
                        {.Action{"duplicate"}, .Data{{.Email{"local01@domain01.ru"}}}}
                    }
                }, {
                    .Terminal = true,
                    .Scope{.Direction{"inbound"}},
                    .ConditionQuery{"condition_query1"},
                    .Actions {
                        {.Action{"forward"}, .Data{{.Email{"local10@domain10.ru"}}}},
                        {.Action{"duplicate"}, .Data{{.Email{"local11@domain11.ru"}}}}
                    }
                }
            },
            .Revision = 1
        };
    }

    void TestGet(TGetCallback callback, bool useTvm = true) {
        const TFuritaClient furitaClient{MakeContext(useTvm), ClusterCall, TvmModule};
        TOrgId orgId{"4"};
        furitaClient.Get(IoContext, std::move(orgId), std::move(callback));
        IoContext.run();
    }

    const std::shared_ptr<StrictMock<TClusterCallMock>> ClusterCall{std::make_shared<StrictMock<
        TClusterCallMock>>()};
    const std::shared_ptr<StrictMock<TYmodTvmMock>> TvmModule{std::make_shared<StrictMock<TYmodTvmMock>>()};
    boost::asio::io_context IoContext;
    const std::string ServiceName{"furita_service_name"};
    const std::string ServiceTicket{"TvmServiceTicket"};
};

TEST_F(TTestFuritaClient, for_cluster_call_error_list_must_return_error) {
    const auto expectedErrorCode{ymod_httpclient::http_error::make_error_code(yhttp::errc::connect_error)};
    EXPECT_CALL(*ClusterCall, async_run(_, MakeFuritaListHttpRequest(), _, _)).
        WillOnce(InvokeArgument<3>(expectedErrorCode, THttpResponse{}));
    TestList([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(expectedErrorCode, errorCode);
        EXPECT_EQ((TListResult{}), result);
    });
}

class TTestFuritaClientListWithPairParam
    : public TTestFuritaClient
    , public WithParamInterface<std::pair<int, EError>>
{
};

TEST_P(TTestFuritaClientListWithPairParam, for_furita_error_list_must_return_error) {
    const auto param{GetParam()};
    EXPECT_CALL(*ClusterCall, async_run(_, MakeFuritaListHttpRequest(), _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{param.first, {}, {}, {}}));
    TestList([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(make_error_code(param.second), errorCode);
        EXPECT_EQ((TListResult{}), result);
    });
}

INSTANTIATE_TEST_SUITE_P(UseStatusAndError, TTestFuritaClientListWithPairParam, Values (
    std::make_pair(STATUS_NONRETRYABLE, EError::HttpNonRetryableStatus),
    std::make_pair(STATUS_RETRYABLE, EError::HttpRetriesExceeded)
));

TEST_F(TTestFuritaClient, for_furita_response_parse_error_list_must_return_error) {
    EXPECT_CALL(*ClusterCall, async_run(_, MakeFuritaListHttpRequest(), _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{STATUS_OK, {}, {}, {}}));
    TestList([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(make_error_code(EError::FuritaResponseParseError), errorCode);
        EXPECT_EQ((TListResult{}), result);
    });
}

TEST_F(TTestFuritaClient, for_correct_furita_response_list_must_return_parsed_response) {
    EXPECT_CALL(*ClusterCall, async_run(_, MakeFuritaListHttpRequest(), _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{STATUS_OK, {}, MakeFuritaListResponseBody(), {}}));
    TestList([&](auto errorCode, auto result) {
        ASSERT_FALSE(errorCode);
        EXPECT_EQ((TListResult{MakeFuritaListResponse()}), result);
    });
}

TEST_F(TTestFuritaClient, for_tvm_module_error_get_must_return_error) {
    EXPECT_CALL(*TvmModule, get_service_ticket(ServiceName, _)).WillOnce(Return(
        ymod_tvm::error::make_error_code(ymod_tvm::error::tickets_not_loaded)));
    TestGet([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(make_error_code(EError::TvmServiceTicketError), errorCode);
        EXPECT_EQ((TGetResult{}), result);
    });
}

TEST_F(TTestFuritaClient, for_cluster_call_error_get_must_return_error) {
    const auto expectedErrorCode{ymod_httpclient::http_error::make_error_code(yhttp::errc::connect_error)};
    const InSequence sequence;
    EXPECT_CALL(*TvmModule, get_service_ticket(ServiceName, _)).WillOnce(DoAll(SetArgReferee<1>(
        ServiceTicket), Return(ymod_tvm::error::make_error_code(ymod_tvm::error::success))));
    EXPECT_CALL(*ClusterCall, async_run(_, MakeFuritaGetHttpRequest(), _, _)).
        WillOnce(InvokeArgument<3>(expectedErrorCode, THttpResponse{}));
    TestGet([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(expectedErrorCode, errorCode);
        EXPECT_EQ((TGetResult{}), result);
    });
}

class TTestFuritaClientGetWithPairParam
    : public TTestFuritaClient
    , public WithParamInterface<std::pair<int, EError>>
{
};

TEST_P(TTestFuritaClientGetWithPairParam, for_furita_error_get_must_return_error) {
    const InSequence sequence;
    EXPECT_CALL(*TvmModule, get_service_ticket(ServiceName, _)).WillOnce(DoAll(SetArgReferee<1>(
        ServiceTicket), Return(ymod_tvm::error::make_error_code(ymod_tvm::error::success))));
    const auto param{GetParam()};
    EXPECT_CALL(*ClusterCall, async_run(_, MakeFuritaGetHttpRequest(), _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{param.first, {}, {}, {}}));
    TestGet([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(make_error_code(param.second), errorCode);
        EXPECT_EQ((TGetResult{}), result);
    });
}

INSTANTIATE_TEST_SUITE_P(UseStatusAndError, TTestFuritaClientGetWithPairParam, Values (
    std::make_pair(STATUS_NONRETRYABLE, EError::HttpNonRetryableStatus),
    std::make_pair(STATUS_ORG_NOT_FOUND, EError::FuritaOrgNotFound),
    std::make_pair(STATUS_RETRYABLE, EError::HttpRetriesExceeded)
));

TEST_F(TTestFuritaClient, for_furita_response_parse_error_get_must_return_error) {
    const InSequence sequence;
    EXPECT_CALL(*TvmModule, get_service_ticket(ServiceName, _)).WillOnce(DoAll(SetArgReferee<1>(
        ServiceTicket), Return(ymod_tvm::error::make_error_code(ymod_tvm::error::success))));
    EXPECT_CALL(*ClusterCall, async_run(_, MakeFuritaGetHttpRequest(), _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{STATUS_OK, {}, {}, {}}));
    TestGet([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(make_error_code(EError::FuritaResponseParseError), errorCode);
        EXPECT_EQ((TGetResult{}), result);
    });
}

TEST_F(TTestFuritaClient, for_tvm_not_in_use_and_correct_furita_response_get_must_return_parsed_response) {
    const auto dontUseTvm{false};
    EXPECT_CALL(*ClusterCall, async_run(_, MakeFuritaGetHttpRequest(dontUseTvm), _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{STATUS_OK, {}, MakeFuritaGetResponseBody(), {}}));
    auto callback{[&](auto errorCode, auto result) {
        ASSERT_FALSE(errorCode);
        EXPECT_EQ((TGetResult{MakeFuritaGetResponse()}), result);
    }};

    TestGet(std::move(callback), dontUseTvm);
}

TEST_F(TTestFuritaClient, for_tvm_in_use_and_correct_furita_response_get_must_return_parsed_response) {
    const InSequence sequence;
    EXPECT_CALL(*TvmModule, get_service_ticket(ServiceName, _)).WillOnce(DoAll(SetArgReferee<1>(
        ServiceTicket), Return(ymod_tvm::error::make_error_code(ymod_tvm::error::success))));
    EXPECT_CALL(*ClusterCall, async_run(_, MakeFuritaGetHttpRequest(), _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{STATUS_OK, {}, MakeFuritaGetResponseBody(), {}}));
    TestGet([&](auto errorCode, auto result) {
        ASSERT_FALSE(errorCode);
        EXPECT_EQ((TGetResult{MakeFuritaGetResponse()}), result);
    });
}

}
