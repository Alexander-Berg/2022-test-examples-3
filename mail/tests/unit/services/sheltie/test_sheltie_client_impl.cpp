#include <tests/unit/http_client_mock.hpp>
#include <tests/unit/test_with_task_context.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/services/sheltie/sheltie_client_impl.hpp>
#include <src/logic/interface/types/reflection/vcard.hpp>

#include <vector>

#include <yamail/data/deserialization/yajl.h>
#include <yamail/data/serialization/yajl.h>

namespace {

using namespace testing;
using Options = ymod_httpclient::cluster_call::options;
using Request = yhttp::request;
using Response = yhttp::response;
using Timeouts = ymod_httpclient::timeouts;

using collie::logic::Vcard;
using collie::logic::Uid;
using collie::logic::MapUriVcardJson;
using collie::logic::MapUriVcardRfc;
using collie::services::sheltie::SheltieClientImpl;
using collie::tests::GetClusterClientMockWrapper;
using collie::tests::ClusterClientMock;
using collie::tests::GetStrictMockedClusterClient;
using yamail::data::deserialization::fromJson;
using yamail::data::serialization::toJson;
using yplatform::task_context_ptr;

MapUriVcardJson getMapUriVcardsAsJson() {
    Vcard  vcard1;
    vcard1.names = {
        {"John", "Do", "Johnson", "-", "-"},
        {"Alex", "Di", "Alexon", "[", "]"}
    };
    vcard1.emails = {
        {"yapoptest@rambler.ru", std::vector<std::string> {"HOME", "WORK"}, std::nullopt},
        {"yapoptest@gmail.com", std::vector<std::string> {"WORK"}, std::nullopt}
    };

    Vcard  vcard2;
    vcard2.names = {
        {"Anna", "Do", "Johnson", "<", ">"},
        {"Sandra", "Di", "Alexon", "[", "]"}
    };
    vcard2.emails = {
        {"yapoptest@rambler.ru", std::vector<std::string> {"HOME"}, std::nullopt},
        {"yapoptest@gmail.com", std::vector<std::string> {"WORK"}, std::nullopt}
    };

    return {{"YA-1", vcard1}, {"YA-2", vcard1}};
}

MapUriVcardRfc getMapUriVcardsAsRfc() {
    return {
        {
            "YA-1",
            "BEGIN:VCARD\r\nVERSION:3.0\r\nEMAIL;TYPE=WORK:yapoptest@rambler.ru"
            "\r\nEMAIL;TYPE=WORK:yapoptest@gmail.com\r\nFN:- John Do Johnson -\r\n"
            "N:Johnson;John;Do;-;-\r\nEND:VCARD\r\n"
        },
        {
            "YA-2",
            "BEGIN:VCARD\r\nVERSION:3.0\r\nEMAIL;TYPE=HOME:yapoptest@rambler.ru\r\nEMAIL;TYPE=WORK:"
            "yapoptest@gmail.com\r\nFN:> Anna Do Johnson <\r\nN:Johnson;Anna;Do;>;<\r\nEND:VCARD\r\n"
        }
    };
}

struct TestServicesSheltieClientImpl : TestWithTaskContext {
    const std::shared_ptr<testing::StrictMock<ClusterClientMock>> httpClient = GetStrictMockedClusterClient();
    GetClusterClientMockWrapper getHttpClient {};
    const SheltieClientImpl impl {getHttpClient};
    const Options options {{{}, {}, {}, {}}, {}};

    const std::string vcard = R"(BEGIN:VCARD\r\nVERSION:3.0\r\nUID:YAAB-123-YA2\r\nEMAIL;TYPE=WORK)"
    R"(:yapoptest@rambler.ru\r\nEMAIL;TYPE=WORK:yapoptest@gmail.com\r\nFN:- John Do Johnson -\r\nN:)"
    R"(Johnson;John;Do;-;-\r\nEND:VCARD\r\n)";

    const Uid uid {"42"};
    const Request postToVcardRequest = Request::POST(
        "/v1/to_vcard?uid=42",
        "Content-Type: application/json\r\n"
        "X-Request-Id: request_id\r\n",
        toJson(getMapUriVcardsAsJson())
    );
    const Request postFromVcardRequest = Request::POST(
        "/v1/from_vcard?uid=42",
        "Content-Type: application/text\r\n"
        "X-Request-Id: request_id\r\n",
        std::string(vcard)
    );

    std::string goodPostFromVcardResponse = R"({"vcard_uids": ["YAAB-123-YA2"], "names":)"
    R"([{"middle": "Do", "prefix": "-", "last": "Johnson", "suffix": "-", "first": "John"}],)"
    R"("emails": [{"type": "WORK", "email": "yapoptest@rambler.ru"}, {"type": "WORK", "email": "yapoptest@gmail.com"}]})";

    std::string goodPostToVcardResponse = R"({"YA-1": "BEGIN:VCARD\r\nVERSION:3.0\r\nEMAIL;TYPE=WORK:)"
    R"(yapoptest@rambler.ru\r\nEMAIL;TYPE=WORK:yapoptest@gmail.com\r\nFN:- John Do Johnson -\r\nN:)"
    R"(Johnson;John;Do;-;-\r\nEND:VCARD\r\n", "YA-2": "BEGIN:VCARD\r\nVERSION:3.0\r\nEMAIL;TYPE=HOME)"
    R"(:yapoptest@rambler.ru\r\nEMAIL;TYPE=WORK:yapoptest@gmail.com\r\nFN:> Anna Do Johnson <\r\n)"
    R"(N:Johnson;Anna;Do;>;<\r\nEND:VCARD\r\n"})";
};

TEST_F(TestServicesSheltieClientImpl, for_good_response_on_toVcard_request_should_return_vcard_as_rfc) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(task_context_ptr(context), postToVcardRequest, options, _))
            .WillOnce(InvokeArgument<3>(
                yhttp::errc::success,
                Response {200, {}, goodPostToVcardResponse, {}}
            ));
        EXPECT_EQ(impl.toVcard(context, uid, getMapUriVcardsAsJson()), getMapUriVcardsAsRfc());
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(httpClient.get()));
}

TEST_F(TestServicesSheltieClientImpl, for_bad_data_in_response_on_toVcard_request_should_return_throw_exception) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(task_context_ptr(context), postToVcardRequest, options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {200, {}, "", {}}));
        EXPECT_THROW(impl.toVcard(context, uid, getMapUriVcardsAsJson()), std::runtime_error);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(httpClient.get()));
}

TEST_F(TestServicesSheltieClientImpl, for_throw_bad_url_error_on_toVcard_request_should_throw_exception) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(task_context_ptr(context), postToVcardRequest, options, _))
            .WillOnce(Throw(ymod_httpclient::bad_url_error()));
        EXPECT_THROW(impl.toVcard(context, uid, getMapUriVcardsAsJson()), std::runtime_error);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(httpClient.get()));
}

TEST_F(TestServicesSheltieClientImpl, for_response_on_toVcard_request_status_is_4xx_should_throw_exception) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(task_context_ptr(context), postToVcardRequest, options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {400, {}, {}, {}}));
        EXPECT_THROW(impl.toVcard(context, uid, getMapUriVcardsAsJson()), std::runtime_error);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(httpClient.get()));
}

TEST_F(TestServicesSheltieClientImpl,
        for_response_on_toVcard_request_status_is_5xx_should_throw_exception) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(task_context_ptr(context), postToVcardRequest, options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {500, {}, {}, {}}));
        EXPECT_THROW(impl.toVcard(context, uid, getMapUriVcardsAsJson()), std::runtime_error);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(httpClient.get()));
}

TEST_F(TestServicesSheltieClientImpl, for_good_response_on_fromVcard_request_should_return_vcard_as_json) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
        EXPECT_CALL(*httpClient, async_run(task_context_ptr(context), postFromVcardRequest, options, _))
            .WillOnce(InvokeArgument<3>(
                yhttp::errc::success,
                Response {200, {}, goodPostFromVcardResponse, {}}
            ));
        EXPECT_EQ(impl.fromVcard(context, uid, vcard), goodPostFromVcardResponse);
    });
    EXPECT_TRUE(Mock::VerifyAndClearExpectations(httpClient.get()));
}

}
