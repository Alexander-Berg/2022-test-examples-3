#include "../test_with_context.h"
#include "../../mocks.h"

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/services/iam/iam_client.h>

namespace {

using namespace testing;
using namespace sharpei;
using namespace sharpei::services;
using namespace sharpei::services::iam;

using sharpei::tests::TestWithContext;

struct JsonWebTokenGeneratorMock : JsonWebTokenGenerator {
    MOCK_METHOD(expected<std::string>, generate, (const TaskContextPtr&), (const, override));
};

IamClientConfig makeIamClientConfig() {
    IamClientConfig result;
    result.http.options = makeClusterClientOptions();
    return result;
}

struct TestServicesIamClient : TestWithContext {
    const IamClientConfig config = makeIamClientConfig();
    const std::shared_ptr<StrictMock<ClusterClientMock>> httpClient = std::make_shared<StrictMock<ClusterClientMock>>();
    const std::shared_ptr<StrictMock<const JsonWebTokenGeneratorMock>> jwtGenerator = std::make_shared<StrictMock<const JsonWebTokenGeneratorMock>>();
    const std::string jsonWebToken {"json_web_token"};
    const yhttp::request request = yhttp::request::POST(
        "/v1/tokens",
        "Content-Type: application/json\r\n"
        "X-Request-Id: request_id\r\n",
        R"({"jwt":"json_web_token"})"
    );
    const std::string responseBody {
        R"({
            "iamToken": "iam_token"
        })"
    };
    yhttp::response response {200, {}, responseBody, ""};
    const IamToken iamToken {"iam_token"};
};

TEST_F(TestServicesIamClient, get_iam_token_should_return_iam_token) {
    const IamClient impl(config, httpClient, jwtGenerator);

    withContext([&] (const auto& context) {
        const InSequence s;

        EXPECT_CALL(*jwtGenerator, generate(context))
            .WillOnce(Return(expected<std::string>(jsonWebToken)));
        EXPECT_CALL(*httpClient, async_run(_, request, config.http.options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

        const auto result = impl.getIamToken(context);

        ASSERT_TRUE(result) << result.error().full_message();
        EXPECT_EQ(result.value(), iamToken);
    });
}

TEST_F(TestServicesIamClient, get_iam_token_for_json_web_token_generator_error_should_return_error) {
    const IamClient impl(config, httpClient, jwtGenerator);

    withContext([&] (const auto& context) {
        const InSequence s;

        EXPECT_CALL(*jwtGenerator, generate(context))
            .WillOnce(Return(make_unexpected(ExplainedError(Error::readJsonWebTokenError))));

        const auto result = impl.getIamToken(context);

        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::readJsonWebTokenError) << result.error().full_message();
    });
}

TEST_F(TestServicesIamClient, get_iam_token_for_http_client_error_should_return_error) {
    const IamClient impl(config, httpClient, jwtGenerator);

    withContext([&] (const auto& context) {
        const InSequence s;

        EXPECT_CALL(*jwtGenerator, generate(context))
            .WillOnce(Return(expected<std::string>(jsonWebToken)));
        EXPECT_CALL(*httpClient, async_run(_, request, config.http.options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::connect_error, response));

        const auto result = impl.getIamToken(context);

        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::iamHttpError) << result.error().full_message();
    });
}

TEST_F(TestServicesIamClient, get_iam_token_for_not_200_http_status_should_return_error) {
    const IamClient impl(config, httpClient, jwtGenerator);

    response.status = 500;

    withContext([&] (const auto& context) {
        const InSequence s;

        EXPECT_CALL(*jwtGenerator, generate(context))
            .WillOnce(Return(expected<std::string>(jsonWebToken)));
        EXPECT_CALL(*httpClient, async_run(_, request, config.http.options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

        const auto result = impl.getIamToken(context);

        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::iamHttpError) << result.error().full_message();
    });
}

TEST_F(TestServicesIamClient, get_iam_token_for_invalid_response_status_should_return_error) {
    const IamClient impl(config, httpClient, jwtGenerator);

    response.body = "";

    withContext([&] (const auto& context) {
        const InSequence s;

        EXPECT_CALL(*jwtGenerator, generate(context))
            .WillOnce(Return(expected<std::string>(jsonWebToken)));
        EXPECT_CALL(*httpClient, async_run(_, request, config.http.options, _))
            .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

        const auto result = impl.getIamToken(context);

        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::iamParseError) << result.error().full_message();
    });
}

} // namespace
