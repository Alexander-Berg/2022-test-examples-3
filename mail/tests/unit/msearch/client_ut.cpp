#include <boost/fusion/adapted.hpp>

#include <mail/notsolitesrv/tests/unit/fakes/context.h>
#include <mail/notsolitesrv/tests/unit/mocks/ymod_tvm.h>
#include <mail/notsolitesrv/tests/unit/mocks/cluster_call.h>
#include <mail/notsolitesrv/tests/unit/util/ymod_httpclient.h>

#include <mail/notsolitesrv/src/config.h>
#include <mail/notsolitesrv/src/context.h>
#include <mail/notsolitesrv/src/errors.h>
#include <mail/notsolitesrv/src/http/types.h>
#include <mail/notsolitesrv/src/types/common.h>
#include <mail/notsolitesrv/src/msearch/client_impl.h>
#include <mail/notsolitesrv/src/msearch/types/request.h>
#include <mail/notsolitesrv/src/msearch/types/reflection/response.h>
#include <yamail/data/serialization/json_writer.h>
#include <ymod_tvm/error.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <boost/asio/io_context.hpp>

#include <string>
#include <memory>
#include <utility>
#include <iostream>

namespace NNotSoLiteSrv::NMSearch {

using boost::fusion::operators::operator==;

static std::ostream& operator<< (std::ostream& s, const TSubscriptionStatusResponse::TSubscription& subscription) {
    return s << yamail::data::serialization::JsonWriter(subscription).result();
}

} // NNotSoLiteSrv::NMSearch

namespace {

using namespace testing;

using NNotSoLiteSrv::TUid;
using NNotSoLiteSrv::TConfigPtr;
using NNotSoLiteSrv::TContext;
using NNotSoLiteSrv::EError;
using NNotSoLiteSrv::THttpRequest;
using NNotSoLiteSrv::THttpResponse;
using NNotSoLiteSrv::NMSearch::TMSearchClientImpl;
using NNotSoLiteSrv::NMSearch::TSubscriptionStatusRequest;
using NNotSoLiteSrv::NMSearch::TSubscriptionStatusResponse;
using NNotSoLiteSrv::NMSearch::TSubscriptionStatusCallback;
using NNotSoLiteSrv::NMSearch::ESubscriptionStatus;

struct TTestMSearchClient : Test {
    using TYmodTvmMock = StrictMock<TYmodTvmMock>;
    using TYmodTvmMockPtr = std::shared_ptr<TYmodTvmMock>;

    using TClusterCallMock = StrictMock<TClusterCallMock>;
    using TClusterCallMockPtr = std::shared_ptr<TClusterCallMock>;

    void TestSubscriptionStatusCall(const TSubscriptionStatusRequest &request, bool useTvm, TSubscriptionStatusCallback callback) {
        auto config = GetConfig({{"msearch_use_tvm", std::string(useTvm ? "1" : "0")}});
        auto context = std::make_shared<TContext>(std::move(config), ConnectionId, EnvelopeId);
        auto msearchClient = std::make_shared<TMSearchClientImpl>(std::move(context), ClusterCall, TvmModule);
        msearchClient->SubscriptionStatus(IoContext, request, std::move(callback));
        IoContext.run();
    }

    const std::string ConnectionId = "msearch-test-ConnectionId";
    const std::string EnvelopeId = "msearch-test-EnvelopeId";
    const std::string ServiceName = "msearch_service_name";
    const std::string ServiceTicket = "msearch-tvm-service-ticket";
    TYmodTvmMockPtr TvmModule = std::make_shared<TYmodTvmMock>();
    TClusterCallMockPtr ClusterCall = std::make_shared<TClusterCallMock>();

    boost::asio::io_context IoContext;
};

TEST_F(TTestMSearchClient, for_tvm_module_error_msearch_must_return_error) {
    const auto request = TSubscriptionStatusRequest{ .Uids = { 1 }, .SubscriptionEmail = "some@email.net" };
    const bool useTvm = true;

    EXPECT_CALL(*TvmModule, get_service_ticket(ServiceName, _))
        .WillOnce(Return(ymod_tvm::error::make_error_code(ymod_tvm::error::tickets_not_loaded)));

    TestSubscriptionStatusCall(request, useTvm, [](auto ec, auto result) {
        EXPECT_TRUE(ec);
        EXPECT_EQ(make_error_code(EError::TvmServiceTicketError), ec);
        EXPECT_FALSE(result);
    });
}

TEST_F(TTestMSearchClient, for_cluster_call_error_msearch_must_return_error) {
    const auto request = TSubscriptionStatusRequest{ .Uids = { 1 }, .SubscriptionEmail = "some@email.net" };
    const bool useTvm = true;

    const auto url = "/api/async/mail/subscriptions/status?uid=1&opt_in_subs_uid=&email=some%40email.net";
    const auto headers = "X-Ya-Service-Ticket: " + ServiceTicket + "\r\n";
    const auto expectedHttpRequest = THttpRequest::GET(url, headers);
    const auto expectedErrorCode = ymod_httpclient::http_error::make_error_code(yhttp::errc::connect_error);

    const InSequence sequence;

    EXPECT_CALL(*TvmModule, get_service_ticket(ServiceName, _)).WillOnce(DoAll(
        SetArgReferee<1>(ServiceTicket),
        Return(ymod_tvm::error::make_error_code(ymod_tvm::error::success))
    ));
    EXPECT_CALL(*ClusterCall, async_run(_, expectedHttpRequest, _, _))
        .WillOnce(InvokeArgument<3>(expectedErrorCode, THttpResponse{}));

    TestSubscriptionStatusCall(request, useTvm, [&](auto ec, auto result) {
        EXPECT_TRUE(ec);
        EXPECT_EQ(expectedErrorCode, ec);
        EXPECT_FALSE(result);
    });
}

TEST_F(TTestMSearchClient, for_response_parse_error_msearch_must_return_error) {
    const auto request = TSubscriptionStatusRequest{ .Uids = { 1 }, .SubscriptionEmail = "some@email.net" };
    const bool useTvm = false;

    const auto url = "/api/async/mail/subscriptions/status?uid=1&opt_in_subs_uid=&email=some%40email.net";
    const auto expectedHttpRequest = THttpRequest::GET(url);

    EXPECT_CALL(*ClusterCall, async_run(_, expectedHttpRequest, _, _)).WillOnce(InvokeArgument<3>(
        ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{ .status = 200, .headers = {}, .body = "{some [corrupted data...", .reason = {} }
    ));

    TestSubscriptionStatusCall(request, useTvm, [](auto ec, auto result) {
        EXPECT_TRUE(ec);
        EXPECT_EQ(ec, make_error_code(EError::MSearchResponseParseError));
        EXPECT_FALSE(result);
    });
}

TEST_F(TTestMSearchClient, for_correct_response_msearch_must_return_parsed_response) {
    const auto request = TSubscriptionStatusRequest{ .Uids = { 1, 2 }, .SubscriptionEmail = "some@email.net" };
    const bool useTvm = false;

    const auto url = "/api/async/mail/subscriptions/status?uid=1%2c2&opt_in_subs_uid=&email=some%40email.net";
    const auto expectedHttpRequest = THttpRequest::GET(url);

    EXPECT_CALL(*ClusterCall, async_run(_, expectedHttpRequest, _, _)).WillOnce(InvokeArgument<3>(
        ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{
            .status = 200,
            .headers = {},
            .body = R"({
                "subscriptions" : [
                    {
                        "uid":      1,
                        "email":    "some@email.net",
                        "status":   "active"
                    },
                    {
                        "uid":      2,
                        "email":    "some@email.net",
                        "status":   "hidden"
                    }
                ]
            })",
            .reason = {}
        }
    ));

    TestSubscriptionStatusCall(request, useTvm, [](auto ec, auto result) {
        EXPECT_FALSE(ec);
        ASSERT_TRUE(result);
        EXPECT_THAT(result->Subscriptions, UnorderedElementsAreArray({TSubscriptionStatusResponse::TSubscription
            { .Uid = 1, .Email = "some@email.net", .Status = ESubscriptionStatus::active },
            { .Uid = 2, .Email = "some@email.net", .Status = ESubscriptionStatus::hidden }
        }));
    });
}

} // namespace
