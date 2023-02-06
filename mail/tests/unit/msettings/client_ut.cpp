#include <mail/notsolitesrv/tests/unit/fakes/context.h>
#include <mail/notsolitesrv/tests/unit/mocks/ymod_tvm.h>
#include <mail/notsolitesrv/tests/unit/mocks/cluster_call.h>
#include <mail/notsolitesrv/tests/unit/util/ymod_httpclient.h>

#include <mail/notsolitesrv/src/config.h>
#include <mail/notsolitesrv/src/context.h>
#include <mail/notsolitesrv/src/errors.h>
#include <mail/notsolitesrv/src/http/types.h>
#include <mail/notsolitesrv/src/types/common.h>
#include <mail/notsolitesrv/src/msettings/client_impl.h>
#include <mail/notsolitesrv/src/msettings/types/request.h>
#include <ymod_tvm/error.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <boost/asio/io_context.hpp>

#include <string>
#include <memory>
#include <utility>

namespace {

using namespace testing;

using NNotSoLiteSrv::TUid;
using NNotSoLiteSrv::TContext;
using NNotSoLiteSrv::EError;
using NNotSoLiteSrv::THttpRequest;
using NNotSoLiteSrv::THttpResponse;
using NNotSoLiteSrv::NMSettings::TMSettingsClientImpl;
using NNotSoLiteSrv::NMSettings::TParamsRequest;
using NNotSoLiteSrv::NMSettings::TParamsCallback;

struct TTestMSettingsClient : Test {
    using TYmodTvmMock = StrictMock<TYmodTvmMock>;
    using TYmodTvmMockPtr = std::shared_ptr<TYmodTvmMock>;

    using TClusterCallMock = StrictMock<TClusterCallMock>;
    using TClusterCallMockPtr = std::shared_ptr<TClusterCallMock>;

    void TestParamsCall(const TParamsRequest& request, bool useTvm, TParamsCallback callback) {
        TestCall([&](auto msettingsClient, auto&& context){
            msettingsClient->GetParams(std::move(context), request, std::move(callback));
        }, useTvm);
    }

    void TestProfileCall(const TParamsRequest& request, bool useTvm, TParamsCallback callback) {
        TestCall([&](auto msettingsClient, auto&& context){
            msettingsClient->GetProfile(std::move(context), request, std::move(callback));
        }, useTvm);
    }

    void TestCall(auto caller, bool useTvm) {
        auto config = GetConfig({{"msettings_use_tvm", std::string(useTvm ? "1" : "0")}});
        auto context = std::make_shared<TContext>(std::move(config), ConnectionId, EnvelopeId);
        auto msettingsClient = std::make_shared<TMSettingsClientImpl>(IoContext, context->GetConfig(), ClusterCall, TvmModule);
        caller(msettingsClient, std::move(context));
        IoContext.run();
    }

    const std::string ConnectionId = "msettings-test-ConnectionId";
    const std::string EnvelopeId = "msettings-test-EnvelopeId";
    const std::string ServiceName = "msettings_service_name";
    const std::string ServiceTicket = "msettings-tvm-service-ticket";
    TYmodTvmMockPtr TvmModule = std::make_shared<TYmodTvmMock>();
    TClusterCallMockPtr ClusterCall = std::make_shared<TClusterCallMock>();

    boost::asio::io_context IoContext;
};

TEST_F(TTestMSettingsClient, for_tvm_module_error_msettings_must_return_error) {
    const auto request = TParamsRequest{ .Uid = 1, .Params = { "any_param" } };
    const bool useTvm = true;

    EXPECT_CALL(*TvmModule, get_service_ticket(ServiceName, _))
        .WillOnce(Return(ymod_tvm::error::make_error_code(ymod_tvm::error::tickets_not_loaded)));

    TestParamsCall(request, useTvm, [](auto ec, auto result) {
        EXPECT_EQ(make_error_code(EError::TvmServiceTicketError), ec);
        EXPECT_FALSE(result);
    });
}

TEST_F(TTestMSettingsClient, for_cluster_call_error_msettings_must_return_error) {
    const auto request = TParamsRequest{ .Uid = 1, .Params = { "any_param" } };
    const bool useTvm = true;

    const auto url = "/get_params?uid=1&settings_list=any_param&service=nsls";
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

    TestParamsCall(request, useTvm, [&](auto ec, auto result) {
        EXPECT_EQ(expectedErrorCode, ec);
        EXPECT_FALSE(result);
    });
}

TEST_F(TTestMSettingsClient, for_response_parse_error_msettings_must_return_error) {
    const auto request = TParamsRequest{ .Uid = 1, .Params = { "some_param" } };
    const bool useTvm = false;

    const auto url = "/get_params?uid=1&settings_list=some_param&service=nsls";
    const auto expectedHttpRequest = THttpRequest::GET(url);

    EXPECT_CALL(*ClusterCall, async_run(_, expectedHttpRequest, _, _))
        .WillOnce(InvokeArgument<3>(
            ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
            THttpResponse{ .status = 200, .headers = {}, .body = "{some [corrupted data...", .reason = {} }
        ));

    TestParamsCall(request, useTvm, [](auto ec, auto result) {
        EXPECT_EQ(ec, make_error_code(EError::MSettingsResponseParseError));
        EXPECT_FALSE(result);
    });
}

TEST_F(TTestMSettingsClient, for_correct_response_msettings_must_return_parsed_response) {
    const auto request = TParamsRequest{ .Uid = 1, .Params = { "mail_b2c_can_use_opt_in_subs", "opt_in_subs_enabled" } };
    const bool useTvm = false;

    const auto url = "/get_params?uid=1&settings_list=mail_b2c_can_use_opt_in_subs%0dopt_in_subs_enabled&service=nsls";
    const auto expectedHttpRequest = THttpRequest::GET(url);

    EXPECT_CALL(*ClusterCall, async_run(_, expectedHttpRequest, _, _))
        .WillOnce(InvokeArgument<3>(
            ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
            THttpResponse{
                .status = 200,
                .headers = {},
                .body = R"({
                    "settings": {
                        "single_settings": {
                            "mail_b2c_can_use_opt_in_subs": "on",
                            "opt_in_subs_enabled": ""
                        }
                    }
                })",
                .reason = {}
            }
        ));

    TestParamsCall(request, useTvm, [](auto ec, auto result) {
        EXPECT_FALSE(ec);
        ASSERT_TRUE(result);
        ASSERT_TRUE(result->CanUseOptInSubs);
        EXPECT_TRUE(*result->CanUseOptInSubs);
        EXPECT_FALSE(result->OptInSubsEnabled);
    });
}

TEST_F(TTestMSettingsClient, for_correct_profile_response_msettings_must_return_parsed_response) {
    const auto request = TParamsRequest{ .Uid = 1, .Params = { "from_name" } };
    const bool useTvm = false;

    const auto url = "/get_profile?uid=1&settings_list=from_name&service=nsls&ask_validator=y";
    const auto expectedHttpRequest = THttpRequest::GET(url);

    EXPECT_CALL(*ClusterCall, async_run(_, expectedHttpRequest, _, _))
        .WillOnce(InvokeArgument<3>(
            ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
            THttpResponse{
                .status = 200,
                .headers = {},
                .body = R"({
                    "settings": {
                        "single_settings": {
                            "from_name": "Display Name"
                        }
                    }
                })",
                .reason = {}
            }
        ));

    TestProfileCall(request, useTvm, [](auto ec, auto result) {
        EXPECT_FALSE(ec);
        ASSERT_TRUE(result);
        ASSERT_TRUE(result->FromName);
        EXPECT_EQ(*result->FromName, "Display Name");
    });
}

} // namespace
