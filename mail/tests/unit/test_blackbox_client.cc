#include "settings_tvm2_module_mocks.h"
#include "settings_cluster_client_mock.h"
#include "test_with_task_context.h"

#include <internal/blackbox/http.h>
#include <internal/common/error_code.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace ymod_httpclient {

static bool operator ==(const request& lhs, const request& rhs) {
    return lhs.method == rhs.method && lhs.url == rhs.url && lhs.headers == rhs.headers &&
            ((lhs.body && rhs.body && *lhs.body == *rhs.body) || (!lhs.body && !rhs.body));
}

}

namespace {

using namespace testing;
using namespace settings;
using namespace settings::blackbox;
using namespace settings::test;

using Request = yhttp::request;
using Response = yhttp::response;

struct TestBlackBoxHttpClient: public TestWithTaskContext {
    std::string requestUrl;
    const std::shared_ptr<StrictMock<ClusterClientMock>> httpClient = GetStrictMockedClusterClient();
    const std::shared_ptr<StrictMock<MockTvm2Module>> tvm2Module = std::make_shared<StrictMock<MockTvm2Module>>();
    const std::shared_ptr<Http> blackboxHttp = std::make_shared<Http>(httpClient, tvm2Module);

    const Request getRequest = Request::GET(
        "?method=userinfo&format=json&uid=42&userip=228&dbfields=subscription.suid.2&attributes=212",
        "X-Ya-Service-Ticket: service_ticket\r\n"
    );

    const std::string blackboxResponse =
        R"({"users":[{"uid":{"hosted":false,"value":"42"},)"
        R"("login":"ru","have_password":true},"karma_status":{"value":0},)"
        R"("karma":{"confirmed":false,"value":0},"have_hint":true,)"
        R"("dbfields":{"subscription.suid.2":"149"},"attributes":{"212":"HelloEng","213":"KittyEng"}]})";

    TestBlackBoxHttpClient() {
        requestUrl = ymod_httpclient::url_encode({
                {"method",      "userinfo"},
                {"format",      "json"},
                {"uid",         "42"},
                {"userip",      "228"},
                {"dbfields",    "subscription.suid.2"},
                {"attributes",  "212"}
        }, '?');
};

};

TEST_F(TestBlackBoxHttpClient, for_unsuccessful_getting_service_ticket_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        EXPECT_CALL(*tvm2Module, get_service_ticket(_, _))
            .WillOnce(DoAll(SetArgReferee<1>("service_ticket"), Return(error::no_ticket_for_service)));
        const auto result = blackboxHttp->infoRequest(context, requestUrl);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxError);
    });
}


TEST_F(TestBlackBoxHttpClient, for_successful_request_should_return_result) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const Response getResponse {200, {}, blackboxResponse, ""};
        EXPECT_CALL(*tvm2Module, get_service_ticket(_, _))
            .WillOnce(DoAll(SetArgReferee<1>("service_ticket"), Return(boost::system::error_code {})));
        EXPECT_CALL(*httpClient, async_run(_, getRequest, _, _))
            .WillOnce(InvokeArgument<3>(boost::system::error_code {}, getResponse));
        const auto result = blackboxHttp->infoRequest(context, requestUrl);
        ASSERT_TRUE(result);
        EXPECT_EQ(result.value(), blackboxResponse);
    });
}

TEST_F(TestBlackBoxHttpClient, for_request_ended_with_0_status_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        const Response getResponse {0, {}, {}, ""};
        EXPECT_CALL(*tvm2Module, get_service_ticket(_, _))
            .WillOnce(DoAll(SetArgReferee<1>("service_ticket"), Return(boost::system::error_code {})));
        EXPECT_CALL(*httpClient, async_run(_, getRequest, _, _))
            .WillOnce(InvokeArgument<3>(boost::system::error_code {}, getResponse));
        const auto result = blackboxHttp->infoRequest(context, requestUrl);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxError);
    });
}

TEST_F(TestBlackBoxHttpClient, for_request_ended_with_error_should_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        EXPECT_CALL(*tvm2Module, get_service_ticket(_, _))
            .WillOnce(DoAll(SetArgReferee<1>("service_ticket"), Return(boost::system::error_code {})));
        EXPECT_CALL(*httpClient, async_run(_, getRequest, _, _))
            .WillOnce(InvokeArgument<3>(make_error_code(Error::blackBoxError), Response {}));
        const auto result = blackboxHttp->infoRequest(context, requestUrl);
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxError);
    });
}

}
