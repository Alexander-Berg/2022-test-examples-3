#include <tests/unit/http_client_mock.hpp>
#include <tests/unit/tvm2_module_mocks.hpp>
#include <tests/unit/test_with_task_context.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/services/settings/settings_client_impl.hpp>
#include <src/services/settings/error.hpp>

#include <boost/system/system_error.hpp>
#include <boost/system/error_code.hpp>

namespace {

using namespace testing;
using Options = ymod_httpclient::cluster_call::options;
using Request = yhttp::request;
using Response = yhttp::response;
using Timeouts = ymod_httpclient::timeouts;

using collie::logic::Uid;
using collie::services::settings::SettingsClientImpl;
using collie::services::settings::Error;
using collie::services::GetTvm2Module;
using collie::tests::GetClusterClientMockWrapper;
using collie::tests::ClusterClientMock;
using collie::tests::GetStrictMockedClusterClient;
using collie::tests::MockTvm2Module;
using yplatform::task_context_ptr;
using collie::error_code;

struct TestServiceSettingsClientImpl : TestWithTaskContext {
    const std::shared_ptr<MockTvm2Module> tvm2Module {std::make_shared<MockTvm2Module>()};

    const GetTvm2Module makeGetTvm2Module {
        [=]() { return tvm2Module; }
    };

    const std::shared_ptr<testing::StrictMock<ClusterClientMock>> httpClient = GetStrictMockedClusterClient();
    GetClusterClientMockWrapper getHttpClient {};
    const SettingsClientImpl impl {getHttpClient, makeGetTvm2Module};
    const Options options {{{}, {}, {}, {}}, {}};
    const Uid uid {"42"};

    const Request getCollectAddressesFieldRequest = Request::GET(
        "/get_profile?settings_list=collect_addresses&uid=42",
        "X-Request-Id: request_id\r\n"
        "X-Ya-Service-Ticket: service_ticket\r\n"
    );

    std::string goodResponseWithCollectAddressesField = R"({"settings" : {"single_settings" : {"collect_addresses" : "on"}}})";
    std::string goodResponseWithEmptyCollectAddressesField = R"({"settings" : {"single_settings" : {"collect_addresses" : ""}}})";
    std::string responseWithoutCollectAddressesField = R"({"settings" : {"single_settings" : {}}})";
    std::string responseWithoutSingleSettingsField = R"({"settings" : {}})";
    std::string responseWithoutSettingsField = R"({"another_field" : {}})";
};

TEST_F(TestServiceSettingsClientImpl, for_good_response_with_collect_addresses_field_should_return_true) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*tvm2Module, get_service_ticket(_, _))
            .WillOnce(DoAll(SetArgReferee<1>("service_ticket"), Return(boost::system::error_code {})));
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(task_context_ptr(context), getCollectAddressesFieldRequest, options, _))
            .WillOnce(InvokeArgument<3>(
                yhttp::errc::success,
                Response {200, {}, goodResponseWithCollectAddressesField, {}}
            ));
        const auto result = impl.getCollectAddressesField(context, uid);

        ASSERT_TRUE(result);
        ASSERT_TRUE(result.value());
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(httpClient.get()));
}

TEST_F(TestServiceSettingsClientImpl, for_good_response_with_empty_collect_addresses_field_should_return_false) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*tvm2Module, get_service_ticket(_, _))
            .WillOnce(DoAll(SetArgReferee<1>("service_ticket"), Return(boost::system::error_code {})));
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(task_context_ptr(context), getCollectAddressesFieldRequest, options, _))
            .WillOnce(InvokeArgument<3>(
                yhttp::errc::success,
                Response {200, {}, goodResponseWithEmptyCollectAddressesField, {}}
            ));
        const auto result = impl.getCollectAddressesField(context, uid);

        ASSERT_TRUE(result);
        ASSERT_FALSE(result.value());
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(httpClient.get()));
}

TEST_F(TestServiceSettingsClientImpl, for_response_without_collect_addresses_field_should_return_badResponse_error) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*tvm2Module, get_service_ticket(_, _))
            .WillOnce(DoAll(SetArgReferee<1>("service_ticket"), Return(boost::system::error_code {})));
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(task_context_ptr(context), getCollectAddressesFieldRequest, options, _))
            .WillOnce(InvokeArgument<3>(
                yhttp::errc::success,
                Response {200, {}, responseWithoutCollectAddressesField, {}}
            ));
        const auto result = impl.getCollectAddressesField(context, uid);

        EXPECT_EQ(result.error(), error_code {Error::badResponse});
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(httpClient.get()));
}

TEST_F(TestServiceSettingsClientImpl, for_response_without_single_settings_field_should_return_badResponse_error) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*tvm2Module, get_service_ticket(_, _))
            .WillOnce(DoAll(SetArgReferee<1>("service_ticket"), Return(boost::system::error_code {})));
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(task_context_ptr(context), getCollectAddressesFieldRequest, options, _))
            .WillOnce(InvokeArgument<3>(
                yhttp::errc::success,
                Response {200, {}, responseWithoutSingleSettingsField, {}}
            ));
        const auto result = impl.getCollectAddressesField(context, uid);

        EXPECT_EQ(result.error(), error_code {Error::badResponse});
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(httpClient.get()));
}

TEST_F(TestServiceSettingsClientImpl, for_response_without_settings_field_should_return_badResponse_error) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*tvm2Module, get_service_ticket(_, _))
            .WillOnce(DoAll(SetArgReferee<1>("service_ticket"), Return(boost::system::error_code {})));
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(task_context_ptr(context), getCollectAddressesFieldRequest, options, _))
            .WillOnce(InvokeArgument<3>(
                yhttp::errc::success,
                Response {200, {}, responseWithoutSettingsField, {}}
            ));
        const auto result = impl.getCollectAddressesField(context, uid);

        EXPECT_EQ(result.error(), error_code {Error::badResponse});
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(httpClient.get()));
}

TEST_F(TestServiceSettingsClientImpl, for_unsuccessful_getting_service_ticket_should_return_tvmServiceTicketError_error) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*tvm2Module, get_service_ticket(_, _))
            .WillOnce(DoAll(SetArgReferee<1>("service_ticket"), Return(ymod_tvm::error::no_ticket_for_service)));

        const auto result = impl.getCollectAddressesField(context, uid);
        EXPECT_EQ(result.error(), error_code {Error::tvmServiceTicketError});
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(httpClient.get()));
}

TEST_F(TestServiceSettingsClientImpl, for_response_which_ended_with_status_4xx_should_return_nonRetryableStatus_error) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*tvm2Module, get_service_ticket(_, _))
            .WillOnce(DoAll(SetArgReferee<1>("service_ticket"), Return(boost::system::error_code {})));
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(task_context_ptr(context), getCollectAddressesFieldRequest, options, _))
            .WillOnce(InvokeArgument<3>(
                yhttp::errc::success,
                Response {400, {}, {}, {}}
            ));
        const auto result = impl.getCollectAddressesField(context, uid);

        EXPECT_EQ(result.error(), error_code {Error::nonRetryableStatus});
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(httpClient.get()));
}

TEST_F(TestServiceSettingsClientImpl, for_response_which_ended_with_status_5xx_should_return_retriesExceeded_error) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*tvm2Module, get_service_ticket(_, _))
            .WillOnce(DoAll(SetArgReferee<1>("service_ticket"), Return(boost::system::error_code {})));
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(task_context_ptr(context), getCollectAddressesFieldRequest, options, _))
            .WillOnce(InvokeArgument<3>(
                yhttp::errc::success,
                Response {500, {}, {}, {}}
            ));
        const auto result = impl.getCollectAddressesField(context, uid);

        EXPECT_EQ(result.error(), error_code {Error::retriesExceeded});
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(httpClient.get()));
}

}
