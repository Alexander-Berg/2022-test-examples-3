#include "../test_with_context.h"
#include "../../mocks.h"

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/services/yc/yc_client.h>

namespace sharpei::services::yc {

static bool operator ==(const Host& lhs, const Host& rhs) {
    return std::tie(lhs.name, lhs.zoneId) == std::tie(rhs.name, rhs.zoneId);
}

} // namespace sharpei::services::yc

namespace {

using namespace std::string_view_literals;
using namespace testing;
using namespace sharpei;
using namespace sharpei::services;
using namespace sharpei::services::yc;

using sharpei::tests::TestWithContext;

YcClientConfig makeYcClientConfig() {
    YcClientConfig result;
    result.clusterId = "cluster_id";
    result.http.options = makeClusterClientOptions();
    return result;
}

struct TestServicesYcClient : TestWithContext {
    const YcClientConfig config = makeYcClientConfig();
    const std::shared_ptr<StrictMock<ClusterClientMock>> httpClient = std::make_shared<StrictMock<ClusterClientMock>>();
    const IamToken iamToken {"iam_token"sv};
    const yhttp::request request = yhttp::request::GET(
        "/managed-postgresql/v1/clusters/cluster_id/hosts",
        "Authorization: Bearer iam_token\r\n"
        "X-Request-Id: request_id\r\n"
    );
    const std::string responseBody {
        R"({
            "hosts": [
                {
                    "name": "first_host",
                    "zoneId": "sas"
                },
                {
                    "name": "second_host",
                    "zoneId": "vla"
                }
            ]
        })"
    };
    yhttp::response response {200, {}, responseBody, ""};
    const std::vector<Host> hosts {{Host {"first_host", "sas"}, Host {"second_host", "vla"}}};
};

TEST_F(TestServicesYcClient, get_hosts_should_return_hosts) {
    const YcClient impl(config, httpClient);

    withContext([&] (const auto& context) {
        const InSequence s;

        EXPECT_CALL(*httpClient, async_run(_, request, config.http.options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

        const auto result = impl.getHosts(iamToken, context);

        ASSERT_TRUE(result) << result.error().full_message();
        EXPECT_EQ(result.value(), hosts);
    });
}

TEST_F(TestServicesYcClient, get_hosts_for_http_client_error_should_return_error) {
    const YcClient impl(config, httpClient);

    withContext([&] (const auto& context) {
        const InSequence s;

        EXPECT_CALL(*httpClient, async_run(_, request, config.http.options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::connect_error, response));

        const auto result = impl.getHosts(iamToken, context);

        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::ycHttpError) << result.error().full_message();
    });
}

TEST_F(TestServicesYcClient, get_hosts_for_status_not_200_should_return_error) {
    const YcClient impl(config, httpClient);

    response.status = 500;

    withContext([&] (const auto& context) {
        const InSequence s;

        EXPECT_CALL(*httpClient, async_run(_, request, config.http.options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

        const auto result = impl.getHosts(iamToken, context);

        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::ycHttpError) << result.error().full_message();
    });
}

TEST_F(TestServicesYcClient, get_hosts_for_invalid_response_should_return_error) {
    const YcClient impl(config, httpClient);

    response.body = "";

    withContext([&] (const auto& context) {
        const InSequence s;

        EXPECT_CALL(*httpClient, async_run(_, request, config.http.options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

        const auto result = impl.getHosts(iamToken, context);

        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::ycParseError) << result.error().full_message();
    });
}

} // namespace
